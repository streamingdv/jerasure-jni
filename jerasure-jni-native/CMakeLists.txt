###################
# Policy settings #
###################
cmake_policy(SET CMP0091 NEW)
cmake_policy(SET CMP0048 NEW)

#####################
# Pre project setup #
#####################
set(CMAKE_OSX_DEPLOYMENT_TARGET "10.10" CACHE STRING "Minimum OS X deployment version" FORCE)

#################
# Project setup #
#################
project(jerasure-jni-native)
cmake_minimum_required(VERSION 3.1)

##############################
# Set global compile options #
##############################
if(UNIX OR MINGW)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fstack-protector")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -fstack-protector")
endif()

############################
# Add 3rd party libraries #
###########################
add_subdirectory(3rdparty)

################
# Requires JNI #
################
find_package(JNI REQUIRED)

#####################
# Configure project #
#####################
if(NOT DEFINED GENERATED_JNI_HEADERS_DIR)
    message(FATAL_ERROR "Please run cmake with -DGENERATED_JNI_HEADERS_DIR=/path/to/generated/headers or simply use the"
            " gradle build task")
endif()

###################
# Project sources #
###################
set(JERASURE_JNI_NATIVE_SOURCES
        src/native_jerasure_jni.cpp)

##############
# Add target #
##############
add_library(jerasure-jni-native SHARED ${JERASURE_JNI_NATIVE_SOURCES})
target_link_libraries(jerasure-jni-native PUBLIC jerasure ${JNI_LIBRARIES})
target_include_directories(jerasure-jni-native PUBLIC ${JNI_INCLUDE_DIRS} ${GENERATED_JNI_HEADERS_DIR})

if(MSVC)
    set_property(
            TARGET jerasure
            PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreadedDebug"
    )

    set_property(
            TARGET jerasure-jni-native
            PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreadedDebug"
    )
endif()