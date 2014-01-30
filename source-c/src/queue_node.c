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
#include <eris/queue_node.h>

ALLOC_DECLARE(queue_node_spec_t);
ALLOC_DEFINE(queue_node_spec_t);

ALLOC_DECLARE(queue_node_t);
ALLOC_DEFINE(queue_node_t);

static qd_log_config_t *log_handler=0;
static qd_dispatch_t *dx=0;
static sys_mutex_t *msg_lock;
static sys_mutex_t *subscriber_lock;
static qd_message_list_t available_msgs;
static qd_message_list_t unsettled_msgs;
static qd_link_list_t subscribers;
static qd_link_item_t *next_subscriber=0;
static queue_node_t *node=0;

/**
* Inbound Delivery Handler
*/
static void queue_rx_handler(void* context, qd_link_t *link, qd_delivery_t *delivery)
{
    //queue_node_t   *queue_node  = (queue_node_t*) context;
    pn_link_t *pn_link = qd_link_pn(link);
    qd_message_t *msg;
    int valid_message = 0;

    // Extract the message from the incoming delivery.
    msg = qd_message_receive(delivery);
    if (!msg)
    {
        //
        // The delivery didn't contain the entire message, we'll come through here
        // again when there's more data to receive.
        //
        return;
    }

    valid_message = qd_message_check(msg, QD_DEPTH_BODY);
    sys_mutex_lock(msg_lock);
    if (valid_message)
    {
        DEQ_INSERT_TAIL(available_msgs, msg);
        if (next_subscriber)
        {
            activate_next_subscriber();
        }
        // For now blindly accept
        qd_delivery_free_LH(delivery, PN_ACCEPTED);
    }    
    else
    {
        qd_delivery_free_LH(delivery, PN_REJECTED);
        qd_message_free(msg);
    }
    sys_mutex_unlock(msg_lock);

    // Advance the link and issue flow-control credit.
    pn_link_advance(pn_link);
    pn_link_flow(pn_link, 1);
}

/**
* Delivery Disposition Handler
*/
static void queue_disp_handler(void* context, qd_link_t *link, qd_delivery_t *delivery)
{

}

/**
* New Incoming Link Handler
*/
static int queue_incoming_link_handler(void* context, qd_link_t *link)
{
    return 0;
}

/**
* New Outgoing Link Handler
*/
static int queue_outgoing_link_handler(void* context, qd_link_t *link)
{
    sys_mutex_lock(subscriber_lock);

    qd_link_item_t *sub = NEW(qd_link_item_t); 
    DEQ_ITEM_INIT(sub);
    sub->link = link;
    DEQ_INSERT_TAIL(subscribers, sub);
    if (!next_subscriber) { next_subscriber = sub; }

    sys_mutex_unlock(subscriber_lock);
    return 0;
}

/**
* Outgoing Link Writable Handler
*/
static int queue_writable_link_handler(void* context, qd_link_t *link)
{    
    qd_router_link_t *rlink = (qd_router_link_t*) qd_link_get_context(link);
    pn_link_t *pn_link      = qd_link_pn(link);    
    int event_count         = 0;
    int link_credit         = pn_link_credit(pn_link);
    qd_message_list_t  to_send;
    size_t    offer;
    bool drain_mode;
    bool drain_changed     = qd_link_drain_changed(link, &drain_mode);

    DEQ_INIT(to_send);

    sys_mutex_lock(msg_lock);
    if (link_credit > 0)
    {
        dtag = tag;
        msg = DEQ_HEAD(available_msgs);
        while (msg) {
            DEQ_REMOVE_HEAD(available_msgs);
            DEQ_INSERT_TAIL(to_send, msg);
            DEQ_INSERT_TAIL(unsettled_msgs, msg);
            if (DEQ_SIZE(to_send) == link_credit)
                break;
            msg = DEQ_HEAD(available_msgs);
        }
        tag += DEQ_SIZE(to_send);
    }
    offer = DEQ_SIZE(available_msgs);
    sys_mutex_unlock(msg_lock);

    msg = DEQ_HEAD(to_send);
    while (msg) {
        DEQ_REMOVE_HEAD(to_send);
        dtag++;
        qd_delivery_t *delivery = qd_delivery(link, pn_dtag((char*) &dtag, 8));
        qd_message_send(msg, link);
        pn_link_advance(pn_link);
        event_count++;        
        msg = DEQ_HEAD(to_send);
    }

    if (offer > 0)
        pn_link_offered(pn_link, offer);
    else {
        pn_link_drained(pn_link);
        if (drain_changed && drain_mode)
            event_count++;
    }

    return event_count;
}

/**
* Link Detached Handler
*/
static int queue_link_detach_handler(void* context, qd_link_t *link, int closed)
{
    return 0;
}

static void queue_inbound_open_handler(void *type_context, qd_connection_t *conn)
{
}


static void queue_outbound_open_handler(void *type_context, qd_connection_t *conn)
{
}

static void activate_next_subscriber()
{
    qd_link_activate(next_subscriber->link);
    qd_link_item_t *next = DEQ_NEXT(next_subscriber);
    if (next)
    {
        next_subscriber = next;
    }
    else
    {
        next_subscriber = DEQ_HEAD(subscribers);
    }
}

queue_node_t *queue_node(queue_node_spec_t *spec, qd_dispatch_t *_dx)
{
    dx = _dx;
    msg_lock = sys_mutex();
    subscriber_lock = sys_mutex();
    DEQ_INIT(available_msgs);
    DEQ_INIT(subscribers);

    qd_node_type_t queue_node_descriptor = {spec->addr_namespace, 0, 0,
                                           queue_rx_handler,
                                           queue_disp_handler,
                                           queue_incoming_link_handler,
                                           queue_outgoing_link_handler,
                                           queue_writable_link_handler,
                                           queue_link_detach_handler,
                                           0, // node_created_handler
                                           0, // node_destroyed_handler
                                           queue_inbound_open_handler,
                                           queue_outbound_open_handler };


    node = NEW(queue_node_t);

    qd_container_register_node_type(dx, &queue_node_descriptor);
    node->qd_node = qd_container_create_node(dx, &queue_node_descriptor, spec->addr_namespace, node, QD_DIST_MOVE, QD_LIFE_PERMANENT);


    return node;   
}
