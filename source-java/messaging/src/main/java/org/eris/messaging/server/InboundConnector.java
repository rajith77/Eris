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

import org.eris.messaging.TransportException;

/**
 * Listens for incoming connections.
 */
public interface InboundConnector
{
    /**
     * Listens for incoming connections in the background until
     * {@link InboundConnector#close()} is called.
     */
    void start() throws TransportException;

    /**
     * Sets the callback for receiving inbound connections.
     */
    void setInboundConnectionListener(InboundConnectionListener l);

    /**
     * Close the listener and free any resources associated with it. This will
     * also close all InboundConnections accepted via this listener.
     */
    void close() throws TransportException;
}