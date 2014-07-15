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

import org.eris.messaging.TransportException;
import org.eris.messaging.server.InboundConnectionListener;
import org.eris.messaging.server.InboundConnector;
import org.eris.messaging.server.ServerConnectionSettings;
import org.eris.network.NetworkConnection;
import org.eris.network.NetworkConnectionListener;
import org.eris.network.Server;
import org.eris.network.io.ServerImpl;

public class InboundConnectorImpl implements InboundConnector, NetworkConnectionListener<ByteBuffer>
{
    private ServerConnectionSettings _settings;

    private Server<ByteBuffer> _server;

    private InboundConnectionListener _listener;

    InboundConnectorImpl(String host, int port)
    {
        this(new ServerConnectionSettings(host, port));
    }

    InboundConnectorImpl(ServerConnectionSettings settings)
    {
        _settings = settings;
    }

    @Override
    public void start() throws TransportException
    {
        if (_listener == null)
        {
            throw new TransportException("InboundConnectionListener needs to be set before starting the connector");
        }

        // HardCode for now
        _server = new ServerImpl(_settings);
        try
        {
            _server.start();
        }
        catch (org.eris.network.NetworkException e)
        {
            throw new TransportException("Exception during Inbound Connector start", e);
        }
    }

    @Override
    public void setInboundConnectionListener(InboundConnectionListener l)
    {
        _listener = l;
    }

    @Override
    public void close() throws TransportException
    {
        _server.shutdown();
    }

    @Override
    public void connection(NetworkConnection<ByteBuffer> con)
    {
        InboundConnectionImpl inConn = new InboundConnectionImpl(_settings, con);
        _listener.connectionRequested(inConn);
    }
}