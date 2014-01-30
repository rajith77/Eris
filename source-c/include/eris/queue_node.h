#ifndef __queue_node_h__
#define __queue_node_h__ 1
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
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
 */

#include <qpid/dispatch.h>

typedef struct queue_node_spec_t queue_node_spec_t;
typedef struct queue_node_t queue_node_t;

struct queue_node_spec_t {
   char *addr_namespace;
   char *address;
   char *node_desc;
};

// Need to figure out a way to keep a list of backup nodes.
struct queue_node_t {
   queue_node_spec_t *node_spec;
   qd_node_t *qd_node;
};

queue_node_t *queue_node(queue_node_spec_t *spec, qd_dispatch_t *_dx);

#endif
