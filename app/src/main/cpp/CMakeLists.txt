cmake_minimum_required(VERSION 3.22.1)
project(voiceengine)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# --- Oboe (Google low-latency Android audio library) ---
include(FetchContent)
FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG        1.9.0
)
FetchContent_MakeAvailable(oboe)

add_library(voiceengine SHARED
    native-lib.cpp
    audio_engine.cpp
    pitch_shifter.cpp
    formant_shifter.cpp
    effects.cpp
)

target_include_directories(voiceengine PRIVATE
    ${oboe_SOURCE_DIR}/include
)

find_library(log-lib log)

target_link_libraries(voiceengine
    oboe
    ${log-lib}
    android
)
