##
## Licensed to the Apache Software Foundation (ASF) under one
## or more contributor license agreements.  See the NOTICE file
## distributed with this work for additional information
## regarding copyright ownership.  The ASF licenses this file
## to you under the Apache License, Version 2.0 (the
## "License"); you may not use this file except in compliance
## with the License.  You may obtain a copy of the License at
##
##   http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing,
## software distributed under the License is distributed on an
## "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
## KIND, either express or implied.  See the License for the
## specific language governing permissions and limitations
## under the License.
##

project(eris C)

cmake_minimum_required(VERSION 2.6)
include(CheckLibraryExists)
include(CheckSymbolExists)
include(CheckFunctionExists)
include(CheckIncludeFiles)
include(FindPythonInterp)
include(FindPythonLibs)

enable_testing()
include (CTest)

if (NOT PYTHONLIBS_FOUND)
     message(FATAL_ERROR "Python Development Libraries are needed.")
endif (NOT PYTHONLIBS_FOUND)

set (SO_VERSION_MAJOR 0)
set (SO_VERSION_MINOR 1)
set (SO_VERSION "${SO_VERSION_MAJOR}.${SO_VERSION_MINOR}")

if (NOT DEFINED LIB_SUFFIX)
    get_property(LIB64 GLOBAL PROPERTY FIND_LIBRARY_USE_LIB64_PATHS)
    if ("${LIB64}" STREQUAL "TRUE" AND ${CMAKE_SIZEOF_VOID_P} STREQUAL "8")
        set(LIB_SUFFIX 64)
    else()
        set(LIB_SUFFIX "")
    endif()
endif()

set(INCLUDE_INSTALL_DIR include CACHE PATH "Include file directory")
set(ERIS_HOME "lib/eris" CACHE PATH "Private Dispatch library directory")
set(LIB_INSTALL_DIR "lib${LIB_SUFFIX}" CACHE PATH "Library object file directory")
set(SHARE_INSTALL_DIR share CACHE PATH "Shared read only data directory")
set(DOC_INSTALL_DIR ${SHARE_INSTALL_DIR}/doc CACHE PATH "Shared read-only data directory")
set(MAN_INSTALL_DIR share/man CACHE PATH "Manpage directory")
set(ERIS_HOME_INSTALLED ${CMAKE_INSTALL_PREFIX}/${ERIS_HOME})

# define the configuration directory based on whether or not the install prefix is defined
if(NOT DEFINED SYSCONF_INSTALL_DIR)
    if(CMAKE_INSTALL_PREFIX STREQUAL "/usr")
        set(SYSCONF_INSTALL_DIR "/etc")
    else()
        set(SYSCONF_INSTALL_DIR "${CMAKE_INSTALL_PREFIX}/etc")
    endif()
endif()

set(ERIS_CONFDIR ${SYSCONF_INSTALL_DIR}/eris)

##
## Find dependencies
##
find_library(pthread_lib pthread)
find_library(rt_lib rt)

find_library(proton_lib qpid-proton)
find_path(proton_include proton/driver.h)

find_library(dispatch_lib qpid-dispatch)
find_path(dispatch_include qpid/dispatch/server.h)

# First check whether we get clock_gettime without any special library linked
CHECK_SYMBOL_EXISTS(clock_gettime "time.h" CLOCK_GETTIME_IN_LIBC)
if (CLOCK_GETTIME_IN_LIBC)
  list(APPEND PLATFORM_DEFINITIONS "USE_CLOCK_GETTIME")
else (CLOCK_GETTIME_IN_LIBC)
  CHECK_LIBRARY_EXISTS (rt clock_gettime "" CLOCK_GETTIME_IN_RT)
  if (CLOCK_GETTIME_IN_RT)
    set (TIME_LIB rt)
    list(APPEND PLATFORM_DEFINITIONS "USE_CLOCK_GETTIME")
  else (CLOCK_GETTIME_IN_RT)
    list(APPEND PLATFORM_DEFINITIONS "USE_GETTIMEOFDAY")
  endif (CLOCK_GETTIME_IN_RT)
endif (CLOCK_GETTIME_IN_LIBC)

CHECK_SYMBOL_EXISTS(uuid_generate "uuid/uuid.h" UUID_GENERATE_IN_LIBC)
if (UUID_GENERATE_IN_LIBC)
  list(APPEND PLATFORM_DEFINITIONS "USE_UUID_GENERATE")
else (UUID_GENERATE_IN_LIBC)
  CHECK_LIBRARY_EXISTS (uuid uuid_generate "" UUID_GENERATE_IN_UUID)
  if (UUID_GENERATE_IN_UUID)
    set (UUID_LIB uuid)
    list(APPEND PLATFORM_DEFINITIONS "USE_UUID_GENERATE")
  else (UUID_GENERATE_IN_UUID)
    CHECK_SYMBOL_EXISTS(uuid_create "uuid.h" UUID_CREATE_IN_LIBC)
    if (UUID_CREATE_IN_LIBC)
      list(APPEND PLATFORM_DEFINITIONS "USE_UUID_CREATE")
    else (UUID_CREATE_IN_LIBC)
      message(FATAL_ERROR "No Uuid API found")
    endif (UUID_CREATE_IN_LIBC)
  endif (UUID_GENERATE_IN_UUID)
endif (UUID_GENERATE_IN_LIBC)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${CMAKE_CURRENT_BINARY_DIR}
    ${proton_include}
    ${dispatch_include}
    ${PYTHON_INCLUDE_PATH}
    )

set(CMAKE_C_FLAGS "-pthread -Wall -Werror -std=gnu99")
set(CATCH_UNDEFINED "-Wl,--no-undefined")

##
## Build the Eris toolkit library
##
set(eris_SOURCES
    src/queue_node.c
    src/util.c
    )

#set_property(SOURCE src/python_embedded.c src/router_pynode.c
#    PROPERTY COMPILE_FLAGS -Wno-strict-aliasing
#    )

add_library(eris SHARED ${eris_SOURCES})
target_link_libraries(eris ${dispatch_lib} ${proton_lib} ${pthread_lib} ${rt_lib} ${UUID_LIB} ${TIME_LIB} ${PYTHON_LIBRARIES})
set_target_properties(eris PROPERTIES
                      VERSION "${SO_VERSION}"
                      SOVERSION "${SO_VERSION_MAJOR}"
                      LINK_FLAGS "${CATCH_UNDEFINED}"
                      )
install(TARGETS eris
        LIBRARY DESTINATION ${LIB_INSTALL_DIR})
file(GLOB headers "include/eris/*.h")
install(FILES ${headers} DESTINATION ${INCLUDE_INSTALL_DIR}/eris)
# The header files for the toolkit that can be used within python.
install(FILES etc/eris.conf DESTINATION ${SYSCONF_INSTALL_DIR}/eris)

##
## Python modules installation
##
#set(TOOLS_EXECUTABLES
#    ${CMAKE_CURRENT_BINARY_DIR}/tools/qdstat
#    ${CMAKE_CURRENT_BINARY_DIR}/tools/qdtest
#)

set(DOC_FILES
    LICENSE
    README
    TODO
)

#install(DIRECTORY python/qpid_dispatch_internal
#        DESTINATION ${ERIS_HOME}/python)

#install(PROGRAMS ${TOOLS_EXECUTABLES}
#        DESTINATION bin)

install(FILES ${DOC_FILES}
        DESTINATION ${DOC_INSTALL_DIR}/eris)

##
## Build Tests
##
#add_subdirectory(doc)
#add_subdirectory(router)
#add_subdirectory(tests)
