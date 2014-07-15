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

import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.message.Message;
import org.eris.messaging.Tracker;
import org.eris.messaging.server.InboundSession;
import org.eris.messaging.server.Publisher;
import org.eris.messaging.server.ReasonCode;

public class PublisherImpl implements Publisher
{
    private final Sender _sender;

    private final InboundConnectionImpl _conn;

    private final String _address;

    PublisherImpl(String address, Sender sender, InboundConnectionImpl conn)
    {
        _address = address;
        _sender = sender;
        _conn = conn;
    }

    @Override
    public String getAddress()
    {
        return _address;
    }

    @Override
    public void accept()
    {
        _sender.open();
        _conn.write();
    }

    @Override
    public void reject(ReasonCode code, String desc)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void close()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public InboundSession getSession()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public org.eris.messaging.Tracker send(org.eris.messaging.Message msg)
    {
        byte[] tag = longToBytes(_conn.getNextDeliveryTag());
        Delivery delivery = _sender.delivery(tag);

        TrackerImpl tracker = new TrackerImpl(null);
        delivery.setContext(tracker);
        if (_sender.getSenderSettleMode() == SenderSettleMode.SETTLED)
        {
            delivery.settle();
            tracker.markSettled();
        }

        Message m = ((MessageImpl) msg).getProtocolMessage();
        if (m.getAddress() == null)
        {
            m.setAddress(_address);
        }
        byte[] buffer = new byte[1024];
        int encoded = m.encode(buffer, 0, buffer.length);
        _sender.send(buffer, 0, encoded);
        _sender.advance();
        _conn.write();
        
        return tracker;
    }

    private byte[] longToBytes(final long value)
    {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        return buffer.array();
    }
}
