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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.apache.qpid.proton.amqp.transport.Transfer;
import org.apache.qpid.proton.amqp.transport.FrameBody.FrameBodyHandler;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.impl.FrameHandler;
import org.apache.qpid.proton.framing.TransportFrame;
import org.eris.logging.Logger;
import org.eris.messaging.server.InboundConnection;
import org.eris.messaging.server.ServerConnectionSettings;
import org.eris.network.NetworkConnection;
import org.eris.network.Receiver;
import org.eris.network.TransportException;

public class InboundConnectionImpl implements InboundConnection, Receiver<ByteBuffer>, FrameHandler, FrameBodyHandler<Integer>
{
    private static final Logger _logger = Logger.get(InboundConnectionImpl.class);

    private static final int CHANNEL_ZERO = 0;

    private final AtomicBoolean _closed = new AtomicBoolean(false);

    private NetworkConnection<ByteBuffer> _networkConnection;

    private org.eris.network.Sender<ByteBuffer> _sender;

    private String _hostname;

    private String _remoteHostname;

    private String _id;

    private String _remoteId;

    private ByteBuffer _outBuffer;

    private ServerConnectionSettings _settings;

    private final Queue<WriteEvent> _writeEventQueue = new LinkedList<WriteEvent>();

    private final Queue<ProtocolEvent> _protocolEventQueue = new LinkedList<ProtocolEvent>();

    private final Map<Integer, InboundSessionImpl> _sessions = new HashMap<Integer, InboundSessionImpl>();

    private int _remoteMaxFrameSize = -1;

    private int _remoteChannelMax = -1;

    InboundConnectionImpl(ServerConnectionSettings settings, NetworkConnection<ByteBuffer> networkCon)
    {
        _networkConnection = networkCon;
        _networkConnection.setReceiver(this);
        _settings = settings;
        try
        {
            _networkConnection.start();
        }
        catch (TransportException e)
        {
            _logger.error(e, "Exception creating InboundConnection object. Dropping network connection");
            try
            {
                _networkConnection.close();
            }
            catch (TransportException ex)
            {
                // ignore.
            }
        }
        _sender = _networkConnection.getSender();
        _outBuffer = ByteBuffer.allocate(_settings.getOutBufferSize());
    }

    @Override
    public void accept()
    {
        // Avoid SASL for now.
        Open open = new Open();
        open.setContainerId(_id == null ? "" : _id);
        open.setHostname(_hostname);
        // open.setDesiredCapabilities();
        // open.setOfferedCapabilities();
        // open.setProperties(_);
        if (_settings.getMaxFrameSize() > 0)
        {
            open.setMaxFrameSize(UnsignedInteger.valueOf(_settings.getMaxFrameSize()));
        }
        if (_settings.getChannelMax() > 0)
        {
            open.setChannelMax(UnsignedShort.valueOf((short) _settings.getChannelMax()));
        }
        _writeEventQueue.add(new WriteEvent(CHANNEL_ZERO,open,null));
    }

    @Override
    public void reject()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void redirect()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void setEventListener()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void setID(String id)
    {
        _id = id;
    }

    @Override
    public void setHostname(String hostname)
    {
        _hostname = hostname;
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

    void setRemoteID(String id)
    {
        _remoteId = id;
    }

    void setRemoteHostname(String host)
    {
        _remoteHostname = host;
    }

    String getID()
    {
        return _id;
    }

    String getHostname()
    {
        return _hostname;
    }

    ByteBuffer getOutBuffer()
    {
        return _outBuffer;
    }

    void addWriteEvent(WriteEvent e)
    {
        _writeEventQueue.add(e);
    }

    void addProtocolEvent(ProtocolEvent e)
    {
        _protocolEventQueue.add(e);
    }

    void addSession(int channel, InboundSessionImpl ssn)
    {
        _sessions.put(channel, ssn);
    }

    void setRemoteChannelMax(int n)
    {
        _remoteChannelMax = n;
    }

    void setRemoteMaxFrameSize(int size)
    {
        _remoteMaxFrameSize = size;
    }

    /* ------------------------------------------------
     * Receiver<ByteBuffer> interface methods
     * ------------------------------------------------
     */
    @Override
    public void close()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void received(ByteBuffer msg)
    {
        // TODO Auto-generated method stub

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

    /* ------------------------------------------------
     * End of Receiver<ByteBuffer> interface methods
     * ------------------------------------------------
     */

    /* -----------------------------------------------
     * FrameBodyHandler methods
     * -----------------------------------------------
     */
    @Override
    public void handleOpen(Open open, Binary payload, Integer channel)
    {
        if(open.getMaxFrameSize().longValue() > 0)
        {
            _remoteMaxFrameSize = open.getMaxFrameSize().intValue();
        }

        if (open.getChannelMax().longValue() > 0)
        {
            _remoteChannelMax = open.getChannelMax().intValue();
        }

        ProtocolEvent ev = new ProtocolEvent(ProtocolEvent.EventType.CONNECTION_OPENED, this);
        addProtocolEvent(ev);
    }

    @Override
    public void handleBegin(Begin begin, Binary payload, Integer channel)
    {
        InboundSessionImpl ssn = new InboundSessionImpl(this);
        ssn.setRemoteChannel(channel);
        ssn.setNextIncomingId(begin.getNextOutgoingId());
        addSession(channel, ssn);

        ProtocolEvent ev = new ProtocolEvent(ProtocolEvent.EventType.SESSION_OPENED, ssn);
        addProtocolEvent(ev);
        
        
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
    /* -----------------------------------------------
     * End of FrameBodyHandler methods
     * -----------------------------------------------
     */

    /* -----------------------------------------------
     * FrameHandler methods
     * -----------------------------------------------
     */

    @Override
    public boolean handleFrame(TransportFrame frame)
    {
        frame.getBody().invoke(this,frame.getPayload(), frame.getChannel());
        return _closed.get();
    }

  /*  @Override
    public void closed()
    {
        _closed.set(true);        

    }
*/
    @Override
    public boolean isHandlingFrames()
    {
        return true;
    }
    /* -----------------------------------------------
     * End of FrameHandler methods
     * -----------------------------------------------
     */
}
