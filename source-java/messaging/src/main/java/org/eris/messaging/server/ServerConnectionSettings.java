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
package org.eris.messaging.server;

import org.eris.messaging.ConnectionSettings;

public class ServerConnectionSettings extends ConnectionSettings
{
    protected int _outBufferSize = 1024;
    protected int _maxFrameSize = -1;
    protected int _channelMax = 65535;
    
    public ServerConnectionSettings(String host, int port)
    {
        _host = host;
        _port = port;
    }

    public int getOutBufferSize()
    {
        return _outBufferSize;
    }

    public int getMaxFrameSize()
    {
        return _maxFrameSize;
    }

    public void setMaxFrameSize(int maxFrameSize)
    {
        this._maxFrameSize = maxFrameSize;
    }

    public int getChannelMax()
    {
        return _channelMax;
    }

    public void setChannelMax(int channelMax)
    {
        this._channelMax = channelMax;
    }

    public void setOutBufferSize(int outBufferSize)
    {
        this._outBufferSize = outBufferSize;
    }

    public void setTcpNodelay(boolean tcpNodelay)
    {
        _tcpNodelay = tcpNodelay;
    }

    public void setReadBufferSize(int readBufferSize)
    {
        _readBufferSize = readBufferSize;
    }

    public void setWriteBufferSize(int writeBufferSize)
    {
        _writeBufferSize = writeBufferSize;
    }

    public void setConnectTimeout(long connectTimeout)
    {
        _connectTimeout = connectTimeout;
    }

    public void setIdleTimeout(long idleTimeout)
    {
        _idleTimeout = idleTimeout;
    }
}