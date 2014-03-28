/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.eris.transport.amqp.proton;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.message.Message;
import org.eris.logging.Logger;
import org.eris.messaging.SenderMode;
import org.eris.messaging.Tracker;
import org.eris.messaging.Tracker.TrackerState;
import org.eris.transport.TransportException;
import org.eris.util.ConditionManager;
import org.eris.util.ConditionManagerTimeoutException;

public class ConnectionImpl implements org.eris.transport.Receiver<ByteBuffer>, org.eris.messaging.Connection
{
    private static final Logger _logger = Logger.get(ConnectionImpl.class);

    enum State
    {
        UNINITIALIZED, ACTIVE, DETACHED, CLOSED
    };

    private org.eris.messaging.ConnectionSettings _settings;

    private org.eris.transport.NetworkConnection<ByteBuffer> _networkConnection;

    private org.eris.transport.Sender<ByteBuffer> _sender;

    private Transport _transport = Proton.transport();

    private Connection _connection;

    private State _state = State.UNINITIALIZED;

    private final Map<Session, SessionImpl> _sessionMap = new ConcurrentHashMap<Session, SessionImpl>();

    private final Object _lock = new Object();
 
    public ConnectionImpl(String url)
    {

    }

    public ConnectionImpl(String host, int port)
    {

    }

    public ConnectionImpl(org.eris.messaging.ConnectionSettings settings)
    {
        _settings = settings;
    }

    @Override
    public void connect() throws org.eris.messaging.TransportException, org.eris.messaging.ConnectionException,
    org.eris.messaging.TimeoutException
    {
        _connection = Proton.connection();
        _connection.setContainer(UUID.randomUUID().toString());
        _connection.setHostname(_settings.getHost());
        _transport.bind(_connection);
        doSasl(_transport.sasl());
        _connection.open();

        try
        {
            // hard code for now
            _networkConnection = new org.eris.transport.io.IoNetworkConnection(_settings);
            _networkConnection.setReceiver(this);
            _networkConnection.connect();
        }
        catch (org.eris.transport.TransportException e)
        {
            throw new org.eris.messaging.TransportException("Exception occurred while making tcp connection to peer", e);
        }
        _sender = _networkConnection.getSender();

        write();
        try
        {
            synchronized (_lock)
            {
                if (_state == State.UNINITIALIZED)
                {
                    _lock.wait(getDefaultTimeout());
                }
            }
        }
        catch (InterruptedException e)
        {
        }
        if (_state == State.UNINITIALIZED)
        {
            throw new org.eris.messaging.TimeoutException("Timeout while waiting for connection to be ready");
        }
    }

    @Override
    public org.eris.messaging.Session createSession() throws org.eris.messaging.TransportException,
    org.eris.messaging.ConnectionException, org.eris.messaging.TimeoutException
    {
        Session ssn = _connection.session();
        SessionImpl session = new SessionImpl(this, ssn);
        _sessionMap.put(ssn, session);
        ssn.open();
        write();
        return session;
    }

    @Override
    public void close() throws org.eris.messaging.TransportException,
    org.eris.messaging.ConnectionException, org.eris.messaging.TimeoutException
    {
        _connection.close();
        write();
        //Should we wait until the remote end close the connection?
        try
        {
            _networkConnection.close();
        }
        catch (TransportException e)
        {
            throw new org.eris.messaging.TransportException("Error closing network connection",e);
        }
    }

    // Needs to expand to handle other mechs
    void doSasl(Sasl sasl)
    {
        if (sasl != null)
        {
            sasl.client();
            sasl.setMechanisms(new String[] { "ANONYMOUS" });
        }
    }

    void write() throws org.eris.messaging.TransportException
    {
        try
        {
            while (_transport.pending() > 0)
            {
                ByteBuffer data = _transport.getOutputBuffer();
                _sender.send(data);
                _sender.flush();
                _transport.outputConsumed();
            }
        }
        catch (org.eris.transport.TransportException e)
        {
            _logger.error(e, "Error while writing to ouput stream");
            throw new org.eris.messaging.TransportException("Error while writing to ouput stream", e);
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
                synchronized (_lock)
                {
                    _state = State.ACTIVE;
                    _lock.notifyAll();
                }
            }
        }
        else
        {
            processSessions();
            processLinks();
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
            if (delivery.isUpdated())
            {
                processUpdate(delivery);
            }
            if (delivery.isReadable() && !delivery.isPartial())
            {
            	incomming(delivery);
            }
            Delivery next = delivery.getWorkNext();
            delivery.clear();
            delivery = next;
        }
    }

    void incomming(Delivery delivery)
    {
    	Receiver receiver = (Receiver)delivery.getLink();
    	int size = delivery.pending();
        byte[] buffer = new byte[size];
        int read = receiver.recv( buffer, 0, buffer.length );
        if (read != size) {
            // TODO need to handle this error
        }
        Message msg = Proton.message();
        msg.decode(buffer, 0, read);
        ((ReceiverImpl)receiver.getContext()).enqueue(new MessageImpl(msg));
    }

    void processUpdate(Delivery delivery)
    {
        if (delivery.isUpdated() && delivery.getLink() instanceof Sender)
        {
            if (delivery.getRemoteState() != null)
            {
                delivery.disposition(delivery.getRemoteState());
                TrackerImpl tracker = (TrackerImpl) delivery.getContext();
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
                if (delivery.getLink().getRemoteReceiverSettleMode() == ReceiverSettleMode.SECOND)
                {
                    if (tracker.isTerminalState())
                    {
                        delivery.settle();
                        tracker.markSettled();
                    }
                }
            }
            if (delivery.remotelySettled())
            {
                TrackerImpl tracker = (TrackerImpl) delivery.getContext();
                delivery.settle();
                tracker.markSettled();
            }
        }

    }

    void processSessions()
    {
        Session ssn = _connection.sessionHead(EndpointStateHelper.ANY, EndpointStateHelper.CLOSED);
        while (ssn != null)
        {
            ssn.close();
            _sessionMap.remove(ssn);
            ssn = ssn.next(EndpointStateHelper.ANY, EndpointStateHelper.CLOSED);
        }
    }

    void processLinks()
    {
        Link link = _connection.linkHead(EndpointStateHelper.ANY, EndpointStateHelper.CLOSED);
        while (link != null)
        {
            link.close();
            _sessionMap.get(link.getSession()).linkClosed(link);
            link = link.next(EndpointStateHelper.ANY, EndpointStateHelper.CLOSED);
        }
    }

    void closeSession(Session ssn) throws org.eris.messaging.TransportException
    {
        ssn.close();
        write();
    }

    long getDefaultTimeout()
    {
        return _settings.getConnectTimeout();
    }

    public static void main(String[] args) throws Exception
    {
        ConnectionImpl con = new ConnectionImpl(new org.eris.messaging.ConnectionSettings());
        con.connect();

        SessionImpl ssn = (SessionImpl) con.createSession();
        SenderImpl sender = (SenderImpl) ssn.createSender("mybox", SenderMode.AT_LEAST_ONCE);
        MessageImpl msg = new MessageImpl();
        msg.setContent("Hello World");
        Tracker t = sender.send(msg);
        t.awaitSettlement();
        con.close();
    }
}