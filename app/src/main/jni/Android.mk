# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#opencv
#
#OPENCVROOT:= /root/OpenCV-2.4.10-android-sdk
#OPENCV_INSTALL_MODULES:=on
#OPENCV_LIB_TYPE:=SHARED
#
#include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := ndkTest
LOCAL_SRC_FILES := decoder.c
LOCAL_LDLIBS := -llog -lz -landroid
LOCAL_STATIC_LIBRARIES := libavformat libavcodec libavutil
include $(BUILD_SHARED_LIBRARY)
$(call import-add-path, /root/Downloads/ffmpeg-3.3)
$(call import-module, build_decoder)
