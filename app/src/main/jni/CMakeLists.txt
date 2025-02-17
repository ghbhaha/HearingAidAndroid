cmake_minimum_required(VERSION 3.4.1)

# convert SDK path to forward slashes on Windows
file(TO_CMAKE_PATH ${PATH_TO_SUPERPOWERED} PATH_TO_SUPERPOWERED)

message(${ANDROID_ABI})

file(GLOB CPP_FILES "*.cpp")

add_library(
	HearingAidAudioProcessor
	SHARED
	${CPP_FILES}
	${PATH_TO_SUPERPOWERED}/OpenSource/SuperpoweredAndroidAudioIO.cpp
)

include_directories(src/main/jni)
include_directories(${PATH_TO_SUPERPOWERED})

target_link_libraries(
    HearingAidAudioProcessor
    log
    android
    OpenSLES
    ${PATH_TO_SUPERPOWERED}/libSuperpoweredAndroid${ANDROID_ABI}.a
)