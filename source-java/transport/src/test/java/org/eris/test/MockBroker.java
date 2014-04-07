package org.eris.test;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.driver.Connector;
import org.apache.qpid.proton.driver.Driver;
import org.apache.qpid.proton.driver.Listener;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;

public class MockBroker
{
    enum State
    {
        NEW, AUTHENTICATING, CONNECTION_UP, FAILED
    };

    private static final EnumSet<EndpointState> UNINIT = EnumSet.of(EndpointState.UNINITIALIZED);

    private static final EnumSet<EndpointState> ACTIVE = EnumSet.of(EndpointState.ACTIVE);

    private static final EnumSet<EndpointState> CLOSED = EnumSet.of(EndpointState.CLOSED);

    private static final EnumSet<EndpointState> ANY = EnumSet.allOf(EndpointState.class);

    private static Accepted ACCEPTED = new Accepted();
  
    private Driver _driver;

    private Listener<State> _listener;

    private int _counter;

    private Map<String, List<byte[]>> _queues = new HashMap<String, List<byte[]>>();

    public MockBroker() throws Exception
    {
        _driver = Proton.driver();
        _listener = _driver.createListener("localhost", 5672, State.NEW);
    }

    public void doWait()
    {
        _driver.doWait(0);
    }

    public void acceptConnections()
    {
        // We have only one listener
        if (_driver.listener() != null)
        {
            System.out.println("Accepting Connection.");
            Connector<State> ctor = _listener.accept();
            Sasl sasl = ctor.sasl();
            sasl.server();
            sasl.setMechanisms(new String[] { "ANONYMOUS" });
            sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
        }
    }

    public void processConnections() throws Exception
    {
        Connector<State> ctor = _driver.connector();
        while (ctor != null)
        {
            // process any data coming from the network, this will update the
            // engine's view of the state of the remote clients
            ctor.process();
            serviceConnector(ctor);
            // now generate any outbound network data generated in response to
            // any work done by the engine.
            ctor.process();

            if (ctor.isClosed())
            {
                ctor.destroy();
            }

            ctor = _driver.connector();
        }
    }

    private void serviceConnector(Connector<State> ctor) throws Exception
    {
        Connection con = ctor.getConnection();

        // Step 1: setup the engine's connection, and any sessions and links
        // that may be pending.

        // initialize the connection if it's new
        if (con.getLocalState() == EndpointState.UNINITIALIZED)
        {
            con.open();
            System.out.println("Connection Opened.");
        }

        // open all pending sessions
        Session ssn = con.sessionHead(UNINIT, ACTIVE);
        while (ssn != null)
        {
            ssn.open();
            System.out.println("Session Opened.");
            ssn = con.sessionHead(UNINIT, ACTIVE);
        }

        // configure and open any pending links
        Link link = con.linkHead(UNINIT, ACTIVE);
        while (link != null)
        {

            setupLink(link);
            System.out.println("Link Opened.");
            link = con.linkHead(UNINIT, ACTIVE);
        }

        // Step 2: Now drain all the pending deliveries from the connection's
        // work queue and process them

        Delivery delivery = con.getWorkHead();
        while (delivery != null)
        {
            System.out.println("Process delivery " + String.valueOf(delivery.getTag()));

            if (delivery.isReadable()) // inbound data available
            {
                processReceive(delivery);
            }
            else if (delivery.isWritable()) // can send a message
            {
                sendMessage(delivery);
            }

            // very basic message handling
            if (delivery.getRemoteState() != null)
            {
                System.out.println("Remote has seen it, Settling delivery " + String.valueOf(delivery.getTag()));
                // once we know the remote has seen the message, we can
                // release the delivery.
                delivery.settle();
            }

            delivery = delivery.getWorkNext();
        }

        // Step 3: Clean up any links or sessions that have been closed by the
        // remote. If the connection has been closed remotely, clean that up
        // also.

        // teardown any terminating links
        link = con.linkHead(ACTIVE, CLOSED);
        while (link != null)
        {
            link.close();
            System.out.println("Link Closed");
            link = con.linkHead(ACTIVE, CLOSED);
        }

        // teardown any terminating sessions
        ssn = con.sessionHead(ACTIVE, CLOSED);
        while (ssn != null)
        {
            ssn.close();
            System.out.println("Session Closed");
            ssn = con.sessionHead(ACTIVE, CLOSED);
        }

        // teardown the connection if it's terminating
        if (con.getRemoteState() == EndpointState.CLOSED)
        {
            System.out.println("Connection Closed");
            con.close();
        }
    }

    private void setupLink(Link link)
    {
        String srcAddress = link.getRemoteSource().getAddress();
        String targetAddress = link.getRemoteTarget().getAddress();

        if (link instanceof Sender)
        {
            System.out.println("Opening Link from Consumer for queue: " + srcAddress);
            if (!_queues.containsKey(srcAddress))
            {
                System.out.println("Queue " + srcAddress + " does not exist! Creating one");
                _queues.put(targetAddress, new ArrayList<byte[]>());
            }
        }
        else
        {
            System.out.println("Opening Link from Producer for queue: " + targetAddress);
            if (!_queues.containsKey(targetAddress))
            {
                _queues.put(targetAddress, new ArrayList<byte[]>());
            }
        }

        Source src = new Source();
        src.setAddress(srcAddress);
        link.setSource(src);

        Target target = new Target();
        target.setAddress(targetAddress);
        link.setTarget(target);

        if (link instanceof Sender)
        {
            // grant a delivery to the link - it will become "writable" when the
            // driver can accept messages for the sender.
            String id = "server-delivery-" + _counter;
            link.delivery(id.getBytes(), 0, id.getBytes().length);
            _counter++;
        }
        else
        {
            // Grant enough credit to the receiver to allow one inbound message
            ((Receiver) link).flow(1);
        }
        link.open();
    }

    private void processReceive(Delivery d)
    {
        Receiver rec = (Receiver) d.getLink();
        String name = rec.getRemoteTarget().getAddress();
        List<byte[]> queue;
        if (!_queues.containsKey(name))
        {
            System.out.println("Error: cannot sent to mailbox " + name + " - dropping message.");
        }
        else
        {
            queue = _queues.get(name);
            byte[] readBuf = new byte[1024];
            int bytesRead = rec.recv(readBuf, 0, readBuf.length);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while (bytesRead > 0)
            {
                bout.write(readBuf, 0, bytesRead);
                bytesRead = rec.recv(readBuf, 0, readBuf.length);
            }
            queue.add(bout.toByteArray());
        }

        d.disposition(ACCEPTED);
        d.settle();
        rec.advance();
        if (rec.getCredit() == 0)
        {
            rec.flow(1);
        }
    }

    private void sendMessage(Delivery d)
    {
        Sender sender = (Sender) d.getLink();
        String name = sender.getRemoteSource().getAddress();
        System.out.println("Sending msg from Queue : " + name);
        byte[] msg;
        if (_queues.containsKey(name))
        {
            msg = _queues.get(name).remove(0);
            System.out.println("Fetching message " + new String(msg));
        }
        else
        {
            System.out.println("Warning: queue " + name + " is empty. No messages to send.");
            return;
        }
        sender.send(msg, 0, msg.length);
        if (sender.advance())
        {
            String id = "server-delivery-" + _counter;
            sender.delivery(id.getBytes(), 0, id.getBytes().length);
            _counter++;
        }
    }

    public static void main(String[] args) throws Exception
    {
        MockBroker server = new MockBroker();
        while (true)
        {
            server.doWait();
            server.acceptConnections();
            server.processConnections();
        }
    }
}