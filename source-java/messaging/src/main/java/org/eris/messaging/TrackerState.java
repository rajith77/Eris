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
 * Outgoing message-delivery state
 */
public enum TrackerState
{
    /**
     * Message has been accepted by the remote peer.
     */
    ACCEPTED,

    /**
     * Message has been rejected by the remote peer.
     */
    REJECTED,

    /**
     * Message has been released by the remote peer.
     */
    RELEASED,

    /**
     * Delivery is still pending and the state is not known.
     */
    UNKNOWN,

    /**
     * The link has failed and the message is in-doubt.
     */
    LINK_FAILED,

    /**
     * The application or the remote peer has closed the link. The state of
     * the delivery is unknown.
     */
    LINK_CLOSED
}