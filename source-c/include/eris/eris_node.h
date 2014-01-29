#ifndef __eris_node_h__
#define __eris_node_h__ 1
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

typedef struct eris_node_spec_t eris_node_spec_t;
typedef struct eris_node_t eris_node_t;

struct eris_node_spec_t {
   char *addr_namespace;
   char *address;
};

// Need to figure out a way to keep a list of backup nodes.
struct eris_node_t {
   eris_node_spec_t *node_spec;   
};

eris_node_t *eris_node(eris_node_spec_t *spec);

#endif
