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
package org.eris.messaging.server.amqp.proton;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.transport.Attach;
import org.apache.qpid.proton.amqp.transport.Begin;
import org.apache.qpid.proton.amqp.transport.Close;
import org.apache.qpid.proton.amqp.transport.Detach;
import org.apache.qpid.proton.amqp.transport.Disposition;
import org.apache.qpid.proton.amqp.transport.End;
import org.apache.qpid.proton.amqp.transport.Flow;
import org.apache.qpid.proton.amqp.transport.FrameBody.FrameBodyHandler;
import org.apache.qpid.proton.amqp.transport.Open;
import org.apache.qpid.proton.amqp.transport.Transfer;
import org.apache.qpid.proton.engine.impl.FrameHandler;
import org.apache.qpid.proton.framing.TransportFrame;

public class FrameHandlerImpl implements FrameHandler, FrameBodyHandler<Integer>
{    
    private final AtomicBoolean _closed = new AtomicBoolean(false);
    private InboundConnectionImpl _conn;

    FrameHandlerImpl(InboundConnectionImpl conn)
    {
        _conn = conn;
    }

    @Override
    public boolean handleFrame(TransportFrame frame)
    {
        frame.getBody().invoke(this,frame.getPayload(), frame.getChannel());
        return _closed.get();
    }

    @Override
    public void closed()
    {
        _closed.set(true);        

    }

    @Override
    public boolean isHandlingFrames()
    {
        return true;
    }

    @Override
    public void handleOpen(Open open, Binary payload, Integer channel)
    {
        if(open.getMaxFrameSize().longValue() > 0)
        {
            _conn.setRemoteMaxFrameSize((int) open.getMaxFrameSize().longValue());
        }

        if (open.getChannelMax().longValue() > 0)
        {
            _conn.setRemoteChannelMax((int) open.getChannelMax().longValue());
        }

        ProtocolEvent ev = new ProtocolEvent(ProtocolEvent.EventType.CONNECTION_OPENED, _conn);
        _conn.addProtocolEvent(ev);
    }

    @Override
    public void handleBegin(Begin begin, Binary payload, Integer channel)
    {
        InboundSessionImpl ssn = new InboundSessionImpl(_conn);
        ssn.setRemoteChannel(channel);
        ssn.setNextIncomingId(begin.getNextOutgoingId());
        _conn.addSession(channel, ssn);

        ProtocolEvent ev = new ProtocolEvent(ProtocolEvent.EventType.SESSION_OPENED, ssn);
        _conn.addProtocolEvent(ev);
    }

    @Override
    public void handleAttach(Attach attach, Binary payload, Integer channel)
    {
        
    }

    @Override
    public void handleFlow(Flow flow, Binary payload, Integer channel)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleTransfer(Transfer transfer, Binary payload, Integer channel)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleDisposition(Disposition disposition, Binary payload, Integer channel)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleDetach(Detach detach, Binary payload, Integer channel)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleEnd(End end, Binary payload, Integer channel)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleClose(Close close, Binary payload, Integer channel)
    {
        // TODO Auto-generated method stub
    }
}
