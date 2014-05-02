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

import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.UnsignedShort;
import org.apache.qpid.proton.amqp.transport.Open;
import org.apache.qpid.proton.engine.EndpointState;
import org.eris.logging.Logger;
import org.eris.messaging.server.InboundConnection;
import org.eris.messaging.server.ServerConnectionSettings;
import org.eris.transport.NetworkConnection;
import org.eris.transport.Receiver;
import org.eris.transport.TransportException;

public class InboundConnectionImpl implements InboundConnection, Receiver<ByteBuffer>
{
    private static final Logger _logger = Logger.get(InboundConnectionImpl.class);

    private static final int CHANNEL_ZERO = 0;
    
    private NetworkConnection<ByteBuffer> _networkConnection;

    private org.eris.transport.Sender<ByteBuffer> _sender;

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
}
