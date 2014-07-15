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

/**
 * Represents an in-bound connection.
 */
public interface InboundConnection
{
    /**
     * Accepts the network connection and initiate the underlying protocol
     * handshake.
     */
    void accept();

    /**
     * Rejects by closing the network connection. This is useful if the
     * application is unable to accept any new connections at this point. It
     * will provide a reason-code if the underlying protocol supports it.
     */
    void reject(ReasonCode code, String desc);

    /**
     * Rejects by closing the network connection, but provides an alternative
     * endpoint to connect to if the underlying protocol supports it.
     */
    void redirect();

    /**
     * Once you accept the Connection, all state changes are notified via the
     * event Listener.
     */
    void setEventListener(EventListener l);

    /**
     * Orderly shutdown at the underlying protocol. Also closes the underlying
     * network connection.
     */
    void close();

    /**
     * An ID set by the application to identify the connection.
     */
    void setLocalID(String id);

    /**
     * Retrieve the local identifier set by the application
     */
    String getLocalID();

    /**
     * Return the ID assigned by the remote peer if exposed by the underlying
     * protocol.
     */
    String getRemoteID();

    /**
     * Return the ID assigned by the remote peer if exposed by the underlying
     * protocol or the network layer.
     */
    String getRemoteHostname();
}