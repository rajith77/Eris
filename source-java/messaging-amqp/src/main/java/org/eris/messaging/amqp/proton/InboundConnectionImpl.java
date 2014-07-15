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
package org.eris.messaging.amqp.proton;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.UnsignedShort;
import org.apache.qpid.proton.amqp.transport.Attach;
import org.apache.qpid.proton.amqp.transport.Begin;
import org.apache.qpid.proton.amqp.transport.Close;
import org.apache.qpid.proton.amqp.transport.Detach;
import org.apache.qpid.proton.amqp.transport.Disposition;
import org.apache.qpid.proton.amqp.transport.End;
import org.apache.qpid.proton.amqp.transport.Flow;
import org.apache.qpid.proton.amqp.transport.Open;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.amqp.transport.Transfer;
import org.apache.qpid.proton.amqp.transport.FrameBody.FrameBodyHandler;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.impl.FrameHandler;
import org.apache.qpid.proton.framing.TransportFrame;
import org.apache.qpid.proton.message.Message;
import org.eris.logging.Logger;
import org.eris.messaging.server.EventListener;
import org.eris.messaging.server.InboundConnection;
import org.eris.messaging.server.InboundSession;
import org.eris.messaging.server.ReasonCode;
import org.eris.messaging.server.ServerConnectionSettings;
import org.eris.network.NetworkConnection;
import org.eris.network.NetworkException;

public class InboundConnectionImpl implements InboundConnection, org.eris.network.Receiver<ByteBuffer>
{
    private static final Logger _logger = Logger.get(InboundConnectionImpl.class);

    private final AtomicLong _deliveryTag = new AtomicLong(0);

    private NetworkConnection<ByteBuffer> _networkConnection;

    private org.eris.network.Sender<ByteBuffer> _sender;

    private String _hostname;

    private String _remoteHostname;

    private String _localID;

    private String _remoteId;

    private ServerConnectionSettings _settings;

    private final Collector _collector = Proton.collector();

    private EventListener _listener;

    private Connection _connection;

    private Transport _transport;

    InboundConnectionImpl(ServerConnectionSettings settings, NetworkConnection<ByteBuffer> networkCon)
    {
        _networkConnection = networkCon;
        _networkConnection.setReceiver(this);
        _settings = settings;
        try
        {
            _networkConnection.start();
        }
        catch (NetworkException e)
        {
            _logger.error(e, "Exception creating InboundConnection object. Dropping network connection");
            try
            {
                _networkConnection.close();
            }
            catch (NetworkException ex)
            {
                // ignore.
            }
        }
        _sender = _networkConnection.getSender();
    }

    @Override
    public void accept()
    {
        _connection = Proton.connection();
        _connection.collect(_collector);
        _connection.setContainer(_settings.getServerID());

        _transport = Proton.transport();
        _transport.bind(_connection);

        // TODO: full SASL
        Sasl sasl = _transport.sasl();
        if (sasl != null)
        {
            sasl.server();
            sasl.setMechanisms(new String[] { "ANONYMOUS" });
            sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
        }
        _connection.open();
        write();
    }

    @Override
    public void reject(ReasonCode code, String desc)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void redirect()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void close()
    {
        // TODO needs to cleanup resources
        _connection.close();
        write();
    }

    @Override
    public void setEventListener(EventListener l)
    {
        _listener = l;
    }

    @Override
    public String getRemoteID()
    {
        return _remoteId;
    }

    @Override
    public String getRemoteHostname()
    {
        return _remoteHostname;
    }

    @Override
    public void setLocalID(String id)
    {
        _localID = id;
    }

    @Override
    public String getLocalID()
    {
        return _localID;
    }

    void setRemoteID(String id)
    {
        _remoteId = id;
    }

    void setRemoteHostname(String host)
    {
        _remoteHostname = host;
    }

    void write()
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
        catch (org.eris.network.NetworkException e)
        {
            _logger.error(e, "Error while writing to ouput stream for connection ");
        }
    }

    /*
     * ------ Receiver<ByteBuffer> interface methods ------------
     */

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

        // Quick and dirty impl .. need to decouple the event notification from
        // the IO thread.
        for (Event event = _collector.peek(); event != null; event = _collector.peek())
        {
            switch (event.getType())
            {
            case CONNECTION_REMOTE_STATE:
            case CONNECTION_LOCAL_STATE:
                if (_connection.getRemoteState() == EndpointState.CLOSED)
                {
                    if (_connection.getLocalState() == EndpointState.ACTIVE)
                    {
                        close();
                    }
                    _listener.connectionClosed(this);
                }
                break;
            case SESSION_REMOTE_STATE:
            case SESSION_LOCAL_STATE:
                Session session = event.getSession();
                if (session.getRemoteState() == EndpointState.ACTIVE)
                {
                    InboundSessionImpl ssn = new InboundSessionImpl(session, this);
                    session.setContext(ssn);
                    _listener.sessionRequested(ssn);
                }
                if (session.getRemoteState() == EndpointState.CLOSED)
                {
                    if (session.getLocalState() == EndpointState.ACTIVE)
                    {
                        InboundSession ssn = (InboundSession) session.getContext();
                        ssn.close();
                        _listener.sessionClosed(ssn);
                    }
                }
                break;
            case LINK_REMOTE_STATE:
            case LINK_LOCAL_STATE:
                Link link = event.getLink();
                if (link.getRemoteState() == EndpointState.ACTIVE)
                {
                    if (link.getLocalState() == EndpointState.UNINITIALIZED)
                    {
                        link.setSource(link.getRemoteSource());
                        link.setTarget(link.getRemoteTarget());
                        if (link instanceof Sender)
                        {
                            PublisherImpl pub = new PublisherImpl(link.getSource().getAddress(), (Sender) link, this);
                            link.setContext(pub);
                            _listener.publishingRequested(pub.getAddress(), pub);
                        }
                        else
                        {
                            SubscriberImpl sub = new SubscriberImpl(link.getSource().getAddress(), (Receiver) link,
                                    this);
                            link.setContext(sub);
                            _listener.subscriptionRequested(sub.getAddress(), sub);
                        }
                    }
                }
                if (link.getRemoteState() == EndpointState.CLOSED)
                {
                    if (link.getLocalState() == EndpointState.ACTIVE)
                    {
                        if (link instanceof Sender)
                        {
                            PublisherImpl pub = (PublisherImpl) link.getContext();
                            pub.close();
                            _listener.publisherClosed(pub);
                        }
                        else
                        {
                            SubscriberImpl sub = (SubscriberImpl) link.getContext();
                            sub.close();
                            _listener.subscriptionClosed(sub);
                        }
                    }
                }
                break;
            case LINK_FLOW:
                link = event.getLink();
                if (link instanceof Sender)
                {
                    SubscriberImpl sub = (SubscriberImpl) link.getContext();
                    _listener.subscriptionCredit(sub, link.getCredit());
                }
                break;
            case DELIVERY:
                Delivery delivery = event.getDelivery();
                link = delivery.getLink();
                if (delivery.isUpdated())
                {
                    if (link instanceof Sender)
                    {
                        if (delivery.getRemoteState() != null)
                        {
                            delivery.disposition(delivery.getRemoteState());
                            TrackerImpl tracker = (TrackerImpl) delivery.getContext();
                            tracker.update(delivery.getRemoteState());
                        }
                        if (delivery.remotelySettled() && link.getSenderSettleMode() == SenderSettleMode.UNSETTLED)
                        {
                            TrackerImpl tracker = (TrackerImpl) delivery.getContext();
                            delivery.settle();
                            tracker.markSettled();
                        }

                    }
                }

                if (delivery.isReadable() && !delivery.isPartial())
                {
                    incomming(delivery);
                }

                delivery.clear();
                break;
            case TRANSPORT:
                // TODO
                break;
            }
            _collector.pop();
        }
    }

    @Override
    public void exception(Throwable t)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void closed()
    {
        // TODO Auto-generated method stub
    }

    void incomming(Delivery delivery)
    {
        Receiver receiver = (Receiver) delivery.getLink();
        int size = delivery.pending();
        byte[] buffer = new byte[size];
        int read = receiver.recv(buffer, 0, buffer.length);
        if (read != size)
        {
            // TODO need to handle this error
        }
        Message msg = Proton.message();
        msg.decode(buffer, 0, read);

        PublisherImpl pub = (PublisherImpl) receiver.getContext();
        String tag = String.valueOf(delivery.getTag());

        _listener.incomingMessageDelivery(pub, new InboundMessageImpl(delivery, msg));
    }

    long getNextDeliveryTag()
    {
        return _deliveryTag.incrementAndGet();
    }
}