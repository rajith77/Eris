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
package org.eris.titan.msgstore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;

import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.MessengerException;
import org.apache.qpid.proton.messenger.impl.MessengerImpl;
import org.eris.ErisMessage;
import org.eris.MessageStore;
import org.eris.titan.ErisMessageImpl;
import org.eris.titan.ManagementMessageProperties;
import org.eris.titan.util.Functions;
import org.eris.titan.util.ManagementMessageFactory;

public class ManagedMessageStore implements MessageStore<Message>,
        ManagementMessageProperties
{
    enum MGT_OP_CODE
    {
        OP_ADD_SUBSCRIBER, OP_REMOVE_SUBSCRIBER, OP_ADD_OBSERVER, REMOVE_OBSERVER, OP_SEND_STATS, OP_STOP_STATS, OP_START_STARTS, OP_SHUTDOWN, OP_UNKNOWN;

        public static MGT_OP_CODE getOpCode(String s)
        {
            if (s == null || s.isEmpty())
            {
                // TODO handle null or empty, currently ignoring
                return OP_UNKNOWN;
            }
            else if (s.contains(ADD_SUBSCRIBER))
            {
                return OP_ADD_SUBSCRIBER;
            }
            else if (s.contains(REMOVE_SUBSCRIBER))
            {
                return OP_REMOVE_SUBSCRIBER;
            }
            else if (s.contains(SEND_STATS))
            {
                return OP_SEND_STATS;
            }
            else if (s.contains(STOP_STATS))
            {
                return OP_STOP_STATS;
            }
            else if (s.contains(START_STARTS))
            {
                return OP_START_STARTS;
            }
            else if (s.contains(SHUTDOWN))
            {
                return OP_SHUTDOWN;
            }
            else
            {
                // TODO handle unknown code, currently ignoring
                return OP_UNKNOWN;
            }
        }
    };

    private List<Peer> _subscribers = new ArrayList<Peer>();

    private List<Peer> _observers = new ArrayList<Peer>();

    private final MessageStore<Message> _delegate;

    private Messenger _messenger = Functions.createMessenger();

    public ManagedMessageStore(MessageStore<Message> delegate)
    {
        _delegate = delegate;
    }

    @Override
    public void put(ErisMessage<Message> m)
    {
        for (Peer p : _observers)
        {
            try
            {
                _messenger.put(m.getDelegate());
            }
            catch (MessengerException e)
            {
                // TODO The only exception thrown here is if the address is
                // invalid.
                e.printStackTrace();
            }
        }

        try
        {
            _messenger.send();
        }
        catch (TimeoutException e)
        {
            // TODO This means that one or more observes didn't get the message.
            // Need to work out error handling policies.
            // Some use cases you care about the errors in other cases you
            // don't.
            e.printStackTrace();
        }

        _delegate.put(m);
    }

    @Override
    public ErisMessage<Message> nextMessage(String... args)
    {
        ErisMessage<Message> msg = _delegate.nextMessage(args);
        for (Peer p : _subscribers)
        {
            try
            {
                _messenger.put(msg.getDelegate());
            }
            catch (MessengerException e)
            {
                // TODO The only exception thrown here is if the address is
                // invalid.
                e.printStackTrace();
            }
        }
        try
        {
            _messenger.send();
        }
        catch (TimeoutException e)
        {
            // TODO This means that one or more subscribers didn't get the
            // message.
            // Need to work out error handling policies.
            // Some use cases you care about the errors in other cases you
            // don't.
            e.printStackTrace();
        }
        return msg;
    }

    @Override
    public void remove(ErisMessage<Message> m)
    {
        _delegate.remove(m);
        Message msg = ManagementMessageFactory.createRemoveFromStoreMessage(m
                .getDelegate().getProperties().getMessageId());
        for (Peer p : _observers)
        {
            try
            {
                _messenger.put(msg);
            }
            catch (MessengerException e)
            {
                // TODO The only exception thrown here is if the address is
                // invalid.
                e.printStackTrace();
            }
        }
        try
        {
            _messenger.send();
        }
        catch (TimeoutException e)
        {
            // TODO Need to retry until it's all removed.
            e.printStackTrace();
        }
    }

    @Override
    public int available()
    {
        return _delegate.available();
    }

    public boolean isManagementMsg(ErisMessage<Message> m)
    {
        return (m.getDelegate().getProperties().getSubject()
                .startsWith(MGT_PREFIX));
    }

    public void evaluateManagementMessage(ErisMessage<Message> m)
    {
        Message msg = m.getDelegate();
        MGT_OP_CODE code = MGT_OP_CODE.getOpCode(msg.getSubject());
        switch (code)
        {
        case OP_ADD_SUBSCRIBER:
            addSubscriber(msg);
            break;
        case OP_ADD_OBSERVER:
            addObserver(msg);
            break;
        case OP_REMOVE_SUBSCRIBER:
            removeSubscriber(msg);
            break;
        case REMOVE_OBSERVER:
            removeObserver(msg);
            break;
        default:
            break;
        }
    }

    private void addSubscriber(Message m)
    {
        String host = (String) m.getApplicationProperties().getValue()
                .get(HOST);
        int port = (int) m.getApplicationProperties().getValue().get(PORT);
        String dest = (String) m.getApplicationProperties().getValue()
                .get(DEST);
        _subscribers.add(new Peer(Functions.createMessengerURLToConnect(host,
                port, dest)));
        ;
    }

    private void addObserver(Message m)
    {
        String host = (String) m.getApplicationProperties().getValue()
                .get(HOST);
        int port = (int) m.getApplicationProperties().getValue().get(PORT);
        String dest = (String) m.getApplicationProperties().getValue()
                .get(DEST);
        _observers.add(new Peer(Functions.createMessengerURLToConnect(host,
                port, dest)));
        ;
    }

    private void removeSubscriber(Message m)
    {
        String host = (String) m.getApplicationProperties().getValue()
                .get(HOST);
        int port = (int) m.getApplicationProperties().getValue().get(PORT);
        String dest = (String) m.getApplicationProperties().getValue()
                .get(DEST);
        _subscribers.remove(new Peer(Functions.createMessengerURLToConnect(
                host, port, dest)));
        ;
    }

    private void removeObserver(Message m)
    {
        String host = (String) m.getApplicationProperties().getValue()
                .get(HOST);
        int port = (int) m.getApplicationProperties().getValue().get(PORT);
        String dest = (String) m.getApplicationProperties().getValue()
                .get(DEST);
        _observers.remove(new Peer(Functions.createMessengerURLToConnect(host,
                port, dest)));
        ;
    }

    class Peer
    {
        final String _address;

        Peer(String addr)
        {
            _address = addr;
        }

        String getAddress()
        {
            return _address;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (obj.getClass() != getClass())
                return false;

            Peer p = (Peer) obj;
            return (this._address.equals(p._address));
        }

        @Override
        public int hashCode()
        {
            return 22 * _address.hashCode();
        }

    }
}
