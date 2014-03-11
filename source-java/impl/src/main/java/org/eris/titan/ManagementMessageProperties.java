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
package org.eris.titan;

public interface ManagementMessageProperties
{
    final static String MGT_PREFIX = "MGT_";

    final static String HOST = "host";

    final static String PORT = "port";

    final static String DEST = "dest";

    final static String COMPETING = "competing";

    final static String MSG_ID = "msg_id";

    final static String REMOVE_MSG = "MGT_REMOVE_MSG";

    final static String ADD_SUBSCRIBER = "MGT_ADD_SUBSCRIBER";

    final static String REMOVE_SUBSCRIBER = "MGT_REMOVE_SUBSCRIBER";

    final static String ADD_OBSERVER = "MGT_ADD_OBSERVER";

    final static String REMOVE_OBSERVER = "MGT_REMOVE_OBSERVER";

    final static String SEND_STATS = "MGT_SEND_STATS";

    final static String STOP_STATS = "MGT_STOP_STATS";

    final static String START_STARTS = "MGT_START_STARTS";

    final static String SHUTDOWN = "MGT_SHUTDOWN";

    final static String UNKNOWN = "MGT_UNKNOWN";
}
