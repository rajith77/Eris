package org.eris.transport.amqp.proton;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;
import org.eris.logging.Logger;
import org.eris.messaging.ConnectionException;
import org.eris.messaging.ConnectionSettings;
import org.eris.messaging.Tracker.TrackerState;
import org.eris.transport.NetworkConnection;
import org.eris.transport.Receiver;
import org.eris.transport.Sender;
import org.eris.transport.TransportException;
import org.eris.transport.io.IoNetworkConnection;
import org.eris.util.ConditionManager;
import org.eris.util.ConditionManagerTimeoutException;

public class ConnectionImpl implements Receiver<ByteBuffer>, org.eris.messaging.Connection
{
    private static final Logger _logger = Logger.get(ConnectionImpl.class);

    enum State {UNINITIALIZED, ACTIVE, DETACHED, CLOSED};

    private ConnectionSettings _settings;
    private NetworkConnection<ByteBuffer> _networkConnection;
    private Sender<ByteBuffer> _sender;
    private Transport _transport = Proton.transport();
    private Connection _connection;
    private State _state = State.UNINITIALIZED;
    
    private final Map<Session, SessionImpl> _sessionMap = new ConcurrentHashMap<Session, SessionImpl>();
        
    private ConditionManager _connectionReady = new ConditionManager(false);

    public ConnectionImpl(String url)
    {

    }

    public ConnectionImpl(String host, int port)
    {

    }

    public ConnectionImpl(ConnectionSettings settings)
    {
        _settings = settings;
    }

    public void connect() throws ConnectionException
    {
        try
        {
            // hard code for now
            _networkConnection = new IoNetworkConnection(_settings);
            _networkConnection.setReceiver(this);
            _networkConnection.connect();
        }
        catch (TransportException e)
        {
            throw new ConnectionException("Exception occurred while making tcp connection to peer", e);
        }
        _sender = _networkConnection.getSender();

        _connection = Proton.connection();
        _connection.setContainer(UUID.randomUUID().toString());
        _connection.setHostname(_settings.getHost());
        _transport.bind(_connection);

        doSasl(_transport.sasl());
        _connection.open();
        write();
        try
        {
            _connectionReady.waitUntilTrue(_settings.getConnectTimeout());
        }
        catch (ConditionManagerTimeoutException e)
        {
            throw new ConnectionException("Timeout while waiting for connection to be ready",e);
        }
    }

    public org.eris.messaging.Session createSession()
    {
        Session ssn = _connection.session();
        ssn.open();
        SessionImpl session = new SessionImpl(this,ssn);
        _sessionMap.put(ssn, session);
        return session;
    }
    
    // Needs to expand to handle other mechs
    void doSasl(Sasl sasl)
    {
        if (sasl != null)
        {
            sasl.client();
            sasl.setMechanisms(new String[]{"ANONYMOUS"});
        }
    }

    void write() throws ConnectionException
    {
        try
        {
            while (_transport.pending() > 0)
            {
                ByteBuffer data = _transport.getOutputBuffer();
                _sender.send(data);
                _transport.outputConsumed();
            }
        }
        catch (TransportException e)
        {
            _logger.error(e, "Error while writing to ouput stream");
            throw new ConnectionException("Error while writing to ouput stream",e);
        }
    }

    @Override
    public void received(ByteBuffer data)
    {
        while (data.hasRemaining())
        {
            ByteBuffer buf = _transport.getInputBuffer();
            int maxAllowed = Math.min(data.remaining(), buf.remaining());
            ByteBuffer temp = data.duplicate();
            temp.limit(data.position() + maxAllowed);
            buf.put(temp);
            _transport.processInput();
            data.position(data.position() + maxAllowed);
        }
        if (_state == State.UNINITIALIZED)
        {
            if (_connection.getRemoteState() == EndpointState.ACTIVE)
            {
                _state = State.ACTIVE;
                _connectionReady.setValueAndNotify(true);
            }
        }
        else
        {
            processDeliveries();
        }
    }

    @Override
    public void exception(Throwable t)
    {

    }

    @Override
    public void closed()
    {
        // TODO Auto-generated method stub
    }
    
    public void processDeliveries()
    {
        Delivery delivery = _connection.getWorkHead();
        while (delivery != null)
        {
            if (delivery.isUpdated() && delivery.getLink() instanceof Sender)
            {
                delivery.disposition(delivery.getRemoteState());
                TrackerImpl tracker = (TrackerImpl)delivery.getContext();
                if (delivery.remotelySettled() && delivery.getRemoteState() != null)
                {
                    if (delivery.getRemoteState() instanceof Accepted)
                    {
                        tracker.setState(TrackerState.ACCEPTED);
                    }
                    else if (delivery.getRemoteState() instanceof Rejected)
                    {
                        tracker.setState(TrackerState.REJECTED);
                    }
                    else if (delivery.getRemoteState() instanceof Released)
                    {
                        tracker.setState(TrackerState.RELEASED);
                    }
                }                
            }
            delivery = delivery.getWorkNext();
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        ConnectionImpl con = new ConnectionImpl(new ConnectionSettings());
        con.connect();
    }
}