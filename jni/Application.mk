# The ARMv7 is significanly faster due to the use of the hardware FPU
APP_STL := gnustl_static
APP_CPPFLAGS := -frtti -fexceptions
APP_ABI := armeabi armeabi-v7a

