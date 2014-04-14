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
package org.eris.messaging;

/**
 * 
 *  Represents a logical Connection to a remote peer within a messaging network.
 *  
 *  <h3>Exceptions</h3>
 *  TransportException  : Thrown when the underlying transport fails.
 *  ConnectionException : Thrown when the Connection gets to an erroneous state.
 *  TimeoutException    : Thrown when an operation exceeds the connection timeout.
 *  
 *  Connection timeout defaults to 60 secs.
 *  This value can be changed via the "eris.connection.timeout" system property, or
 *  by providing an application specific ConnectionSettings implementation when creating
 *  the Connection object.
 *  @see ConnectionSettings
 */
public interface Connection
{
    /** 
     * Creates the underlying physical connection to the peer.
     */
    public void connect() throws TransportException, ConnectionException, TimeoutException;

    /** 
     * Establishes a logical Session for exchanging of messages.
     */
    public Session createSession() throws TransportException, ConnectionException, TimeoutException;

    /** 
     * Terminates the Connection and free any resources associated with this Connection.
     * If there are any active sessions, it will close them first before closing the Connection.
     */
    public void close() throws TransportException, ConnectionException, TimeoutException;
}
