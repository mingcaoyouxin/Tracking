LOCAL_PATH       :=  $(call my-dir)
include              $(CLEAR_VARS)

OpenCV_INSTALL_MODULES := on
OpenCV_CAMERA_MODULES := off

OPENCV_LIB_TYPE :=STATIC

ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
include E:\coding\github3\Tracking\Tracking\native\jni\OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif

LOCAL_MODULE := OpenCV

LOCAL_C_INCLUDES += $(LOCAL_PATH)

LOCAL_SRC_FILES := hello.cpp
#LOCAL_SRC_FILES += jni_part.cpp
LOCAL_SRC_FILES += jni_part2.cpp
LOCAL_SRC_FILES += CMTProxy.cpp
LOCAL_SRC_FILES += CMT.cpp
LOCAL_SRC_FILES += common.cpp
LOCAL_SRC_FILES += Consensus.cpp
LOCAL_SRC_FILES += Fusion.cpp
LOCAL_SRC_FILES += Matcher.cpp
LOCAL_SRC_FILES += Tracker.cpp
LOCAL_SRC_FILES += fastcluster.cpp


LOCAL_LDLIBS +=  -llog -ldl
LOCAL_CFLAGS += -std=c++11

LOCAL_CPPFLAGS += -ffunction-sections -fdata-sections -fvisibility=hidden
LOCAL_CFLAGS += -ffunction-sections -fdata-sections -fvisibility=hidden -g -mfloat-abi=softfp -mfpu=neon -march=armv7-a -mtune=cortex-a8
LOCAL_LDFLAGS += -Wl,--gc-sections,--icf=safe
LOCAL_ARM_MODE := arm
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
# NEON
LOCAL_ARM_NEON := true
endif

include              $(BUILD_SHARED_LIBRARY)