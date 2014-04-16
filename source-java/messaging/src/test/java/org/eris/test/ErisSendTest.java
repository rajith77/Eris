/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.eris.test;

import org.eris.messaging.OutboundConnection;
import org.eris.messaging.Message;
import org.eris.messaging.Messaging;
import org.eris.messaging.Sender;
import org.eris.messaging.SenderMode;
import org.eris.messaging.Session;
import org.eris.messaging.Tracker;
import org.junit.Test;

/**
 * @author Clebert Suconic
 */

public class ErisSendTest
{

   @Test
   public void testSend() throws Exception
   {
      /*Connection con = Messaging.connection("localhost", 5672);
      con.connect();

      Session ssn = con.createSession();
      Sender sender = ssn.createSender("mybox", SenderMode.AT_LEAST_ONCE);
      Message msg = Messaging.message();
      msg.setContent("Hello World");
      Tracker t = sender.send(msg);
      t.awaitSettlement();
      con.close();*/
   }
}
