/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

#include <stdio.h>
#include <signal.h>
#include <qpid/dispatch.h>
#include <eris/eris_node.h>

/**
* Inbound Delivery Handler
*/
static void eris_rx_handler(void* context, qd_link_t *link, qd_delivery_t *delivery)
{
  
}

/**
* Delivery Disposition Handler
*/
static void eris_disp_handler(void* context, qd_link_t *link, qd_delivery_t *delivery)
{

}

/**
* New Incoming Link Handler
*/
static int eris_incoming_link_handler(void* context, qd_link_t *link)
{
   return 0;
}

/**
* New Outgoing Link Handler
*/
static int eris_outgoing_link_handler(void* context, qd_link_t *link)
{
   return 0;
}

/**
* Outgoing Link Writable Handler
*/
static int eris_writable_link_handler(void* context, qd_link_t *link)
{
   return 0;
}

/**
* Link Detached Handler
*/
static int eris_link_detach_handler(void* context, qd_link_t *link, int closed)
{
   return 0;
}

static void eris_inbound_open_handler(void *type_context, qd_connection_t *conn)
{
}


static void eris_outbound_open_handler(void *type_context, qd_connection_t *conn)
{
}

eris_node_t *eris_node(eris_node_spec_t *spec)
{
   qd_node_type_t eris_node = {spec->addr_namespace, 0, 0,
                               eris_rx_handler,
                               eris_disp_handler,
                               eris_incoming_link_handler,
                               eris_outgoing_link_handler,
                               eris_writable_link_handler,
                               eris_link_detach_handler,
                               0, // node_created_handler
                               0, // node_destroyed_handler
                               eris_inbound_open_handler,
                               eris_outbound_open_handler };

  return 0;   
}
