project(Jerasure)
set(INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}")
include_directories (${INCLUDE_DIR})

set(SOURCES gf.c gf_cpu.c gf_general.c gf_method.c gf_rand.c gf_w4.c gf_w8.c gf_w16.c gf_w32.c gf_w64.c gf_w128.c gf_wgen.c galois.c jerasure.c reed_sol.c cauchy.c liberation.c)
add_library(jerasure ${SOURCES})

target_include_directories(jerasure  PUBLIC ${INCLUDE_DIR})