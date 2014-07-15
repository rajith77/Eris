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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.eris.messaging.CompletionListener;
import org.eris.messaging.CreditMode;
import org.eris.messaging.ReceiverMode;
import org.eris.messaging.SenderMode;

public class SessionImpl implements org.eris.messaging.Session
{
    private static final DeliveryState ACCEPTED = Accepted.getInstance();

    private static final DeliveryState REJECTED = new Rejected();

    private static final DeliveryState RELEASED = Released.getInstance();

    private ConnectionImpl _conn;

    private Session _session;

    private AtomicLong _deliveryTag = new AtomicLong(0);

    private AtomicLong _incommingSequence = new AtomicLong(0);

    private CompletionListener _completionListener = null;

    private final Map<Sender, SenderImpl> _senders = new ConcurrentHashMap<Sender, SenderImpl>(2);

    private final Map<Receiver, ReceiverImpl> _receivers = new ConcurrentHashMap<Receiver, ReceiverImpl>(2);

    private final Map<Long, Delivery> _unsettled = new ConcurrentHashMap<Long, Delivery>();

    private final String _id;

    SessionImpl(ConnectionImpl conn, Session ssn)
    {
        _id = UUID.randomUUID().toString();
        _conn = conn;
        _session = ssn;
    }

    @Override
    public org.eris.messaging.Sender createSender(String address, SenderMode mode)
            throws org.eris.messaging.TransportException, org.eris.messaging.SessionException
    {
        checkPreConditions();
        Sender sender;
        Source source = new Source();
        Target target = new Target();
        if (address == null || address.isEmpty() || address.equals("#"))
        {
            String temp = UUID.randomUUID().toString();
            sender = _session.sender(temp);
            target.setDynamic(true);
        }
        else
        {
            sender = _session.sender(address);
            target.setAddress(address);
        }
        sender.setTarget(target);
        sender.setSource(source);
        sender.setSenderSettleMode(mode == SenderMode.AT_MOST_ONCE ? SenderSettleMode.SETTLED
                : SenderSettleMode.UNSETTLED);
        sender.open();

        SenderImpl senderImpl = new SenderImpl(address, this, sender);
        senderImpl.setDynamicAddress(target.getDynamic());
        _senders.put(sender, senderImpl);
        sender.setContext(senderImpl);
        _conn.write();
        return senderImpl;
    }

    @Override
    public org.eris.messaging.Receiver createReceiver(String address, ReceiverMode mode)
            throws org.eris.messaging.TransportException, org.eris.messaging.SessionException
    {
        return createReceiver(address, mode, CreditMode.AUTO);
    }

    @Override
    public org.eris.messaging.Receiver createReceiver(String address, ReceiverMode mode, CreditMode creditMode)
            throws org.eris.messaging.TransportException, org.eris.messaging.SessionException
    {
        checkPreConditions();
        Receiver receiver;
        Source source = new Source();
        Target target = new Target();
        if (address == null || address.isEmpty() || address.equals("#"))
        {
            String temp = UUID.randomUUID().toString();
            receiver = _session.receiver(temp);
            source.setDynamic(true);
        }
        else
        {
            receiver = _session.receiver(address);
            source.setAddress(address);
        }
        receiver.setSource(source);
        receiver.setTarget(target);
        switch (mode)
        {
        case AT_MOST_ONCE:
            receiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);
            receiver.setSenderSettleMode(SenderSettleMode.SETTLED);
            break;
        case AT_LEAST_ONCE:
            receiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);
            receiver.setSenderSettleMode(SenderSettleMode.UNSETTLED);
            break;
        case EXACTLY_ONCE:
            receiver.setReceiverSettleMode(ReceiverSettleMode.SECOND);
            receiver.setSenderSettleMode(SenderSettleMode.UNSETTLED);
            break;
        }
        receiver.open();

        ReceiverImpl receiverImpl = new ReceiverImpl(address, this, receiver, creditMode);
        receiverImpl.setDynamicAddress(source.getDynamic());
        _receivers.put(receiver, receiverImpl);
        receiver.setContext(receiverImpl);
        _conn.write();
        return receiverImpl;
    }

    @Override
    public void accept(org.eris.messaging.Message msg, int... flags) throws org.eris.messaging.SessionException
    {
        setDispositionAndSettleIfRequired(convertMessage(msg), ACCEPTED);
    }

    @Override
    public void reject(org.eris.messaging.Message msg, int... flags) throws org.eris.messaging.SessionException
    {
        setDispositionAndSettleIfRequired(convertMessage(msg), REJECTED);
    }

    @Override
    public void release(org.eris.messaging.Message msg, int... flags) throws org.eris.messaging.SessionException
    {
        setDispositionAndSettleIfRequired(convertMessage(msg), RELEASED);
    }

    @Override
    public void close() throws org.eris.messaging.TransportException
    {
        _conn.closeSession(_session);
    }

    @Override
    public void setCompletionListener(CompletionListener l) throws org.eris.messaging.SessionException
    {
        _completionListener = l;
    }

    IncommingMessage convertMessage(org.eris.messaging.Message msg) throws org.eris.messaging.SessionException
    {
        if (!(msg instanceof IncommingMessage))
        {
            throw new org.eris.messaging.SessionException("The supplied message is not a valid type");
        }

        IncommingMessage m = (IncommingMessage) msg;

        if (m.getSessionID() != _id)
        {
            throw new org.eris.messaging.SessionException("The supplied message is not associated with this session");
        }

        return m;
    }

    void setDispositionAndSettleIfRequired(IncommingMessage msg, DeliveryState state)
    {
        Delivery d = _unsettled.get(msg.getSequence());
        d.disposition(state);
        if (d.getLink().getReceiverSettleMode() == ReceiverSettleMode.FIRST)
        {
            d.settle();
            ((ReceiverImpl) d.getLink().getContext()).decrementUnsettledCount();
        }
    }

    long getNextDeliveryTag()
    {
        return _deliveryTag.incrementAndGet();
    }

    long getNextIncommingSequence()
    {
        return _incommingSequence.incrementAndGet();
    }

    ConnectionImpl getConnection()
    {
        return _conn;
    }

    void closeLink(Link link) throws org.eris.messaging.TransportException
    {
        link.close();
        _conn.write();
    }

    void linkClosed(Link link)
    {
        if (link instanceof Sender)
        {
            _senders.remove(link);
        }
    }

    void write() throws org.eris.messaging.TransportException
    {
        _conn.write();
    }

    void checkPreConditions() throws org.eris.messaging.SessionException
    {
        if (_session.getLocalState() != EndpointState.ACTIVE)
        {
            throw new org.eris.messaging.SessionException("Session is closed");
        }
    }

    String getID()
    {
        return _id;
    }

    void addUnsettled(long id, Delivery d)
    {
        _unsettled.put(id, d);
    }

    CompletionListener getCompletionListener()
    {
        return _completionListener;
    }
}