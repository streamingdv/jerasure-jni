###################
# Policy settings #
###################
cmake_policy(SET CMP0091 NEW)
cmake_policy(SET CMP0048 NEW)

###################
# Set architecture #
###################
if(APPLE)
    if(${CMAKE_OSX_ARCHITECTURES} STREQUAL "x86_64")
        message(STATUS "Building for x86_64")
        set(CMAKE_OSX_ARCHITECTURES "x86_64" CACHE STRING "Build architectures for Mac" FORCE)
    elseif(${CMAKE_OSX_ARCHITECTURES} STREQUAL "arm64")
        message(STATUS "Building for arm64")
        set(CMAKE_OSX_ARCHITECTURES "arm64" CACHE STRING "Build architectures for Mac" FORCE)
    else()
        message(STATUS "Fallback to arm64")
        set(CMAKE_OSX_ARCHITECTURES "arm64" CACHE STRING "Build architectures for Mac" FORCE)
    endif()
endif()

#####################
# Pre project setup #
#####################
set(CMAKE_OSX_DEPLOYMENT_TARGET "10.10" CACHE STRING "Minimum OS X deployment version" FORCE)

# Ensure the build type is always Release
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING "Set the build type to Release by default" FORCE)
endif()

# Ensure the build type is always Release
set(CMAKE_BUILD_TYPE Release CACHE STRING "Force the build type to Release" FORCE)

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
            PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded"
    )

    set_property(
            TARGET jerasure-jni-native
            PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded"
    )
endif()