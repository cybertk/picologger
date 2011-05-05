LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := agentd
LOCAL_MODULE_TAGS := agentd
LOCAL_SRC_FILES := \
	agentd.c \
	fdevent.c \
	services.c \
	util.c

LOCAL_C_INCLUDES += $(common_C_INCLUDES)

LOCAL_PRELINK_MODULE := false

include $(BUILD_EXECUTABLE)
