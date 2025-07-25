# dEQP cmake file

# Module FindGit requires cmake >= 2.8.2
# Using AFTER in target_include_directories requires >= 3.20.0
cmake_minimum_required(VERSION 3.20.0)

option(GLES_ALLOW_DIRECT_LINK "Allow direct linking to GLES libraries" OFF)

# Target selection:
# SELECTED_BUILD_TARGETS is a CMake option that can be set to a list of targets
# that will be built. If the variable is empty (as is the default), all targets
# will be built normally.
set(SELECTED_BUILD_TARGETS "" CACHE STRING "Select some specific targets to build, separated by spaces")

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_CURRENT_SOURCE_DIR}/framework/delibs/cmake")
find_package(Git)
find_package(PythonInterp 3)

# dEQP Target.
set(DEQP_TARGET "default" CACHE STRING "dEQP Target (default, android...)")

if (DEFINED DEQP_TARGET_TOOLCHAIN)
	# \note Toolchain must be included before project() command
	include(targets/${DEQP_TARGET}/${DEQP_TARGET_TOOLCHAIN}.cmake NO_POLICY_SCOPE)
endif ()

project(dEQP-Core-${DEQP_TARGET})

include(framework/delibs/cmake/Defs.cmake NO_POLICY_SCOPE)
include(framework/delibs/cmake/CFlags.cmake)

add_definitions(-DDE_ASSERT_FAILURE_CALLBACK)

# dEQP-specific configuration. Target file should override these.
set(DEQP_TARGET_NAME		"UNKNOWN")		# Target name

set(DEQP_GLES2_LIBRARIES	)				# GLESv2 libraries. If empty, run-time linking is used
set(DEQP_GLES3_LIBRARIES	)				# GLESv3 libraries. If empty, run-time linking is used
set(DEQP_EGL_LIBRARIES		)				# EGL libraries

# Legacy APIs that don't use run-time loading
set(DEQP_SUPPORT_GLES1		OFF)			# Is GLESv1 supported
set(DEQP_GLES1_LIBRARIES	)				# GLESv1 libraries
set(DEQP_SUPPORT_VG			OFF)			# Is OpenVG supported
set(DEQP_VG_LIBRARIES		)				# VG libraries

set(DEQP_SUPPORT_WGL		OFF)
set(DEQP_SUPPORT_GLX		OFF)			# Is GLX supported \todo [2016-10-12 pyry] X11 specific - move to framework/platform/

set(DEQP_PLATFORM_LIBRARIES	)				# Other platform libraries

set(DEQP_PLATFORM_COPY_LIBRARIES	)		# Libraries / binaries that need to be copied to binary directory

# Delibs include directories
include_directories(
	framework/delibs/debase
	framework/delibs/decpp
	framework/delibs/depool
	framework/delibs/dethread
	framework/delibs/deutil
	framework/delibs/destream
	)

# Include target-specific definitions
include(targets/${DEQP_TARGET}/${DEQP_TARGET}.cmake)

# Handle target selection list.
string(STRIP "${SELECTED_BUILD_TARGETS}" TARGET_SELECTION_STRIPPED)
if ("${TARGET_SELECTION_STRIPPED}" STREQUAL "" OR (NOT (DE_OS_IS_WIN32 OR DE_OS_IS_UNIX)))
	set(MAYBE_EXCLUDE_FROM_ALL)
else ()
	# If a non-empty target selection list is provided, exclude all subdirectories by default.
	set(MAYBE_EXCLUDE_FROM_ALL EXCLUDE_FROM_ALL)

	# Each target in the target selection list will be added as a dependency of a new fake target.
	# The new fake target will be added as a dependency of the default build target.
	string(REGEX REPLACE "[ \t\r\n]+" ";" TARGET_SELECTION_LIST "${TARGET_SELECTION_STRIPPED}")
	add_custom_target(target_selection_list_target ALL)
	add_dependencies(target_selection_list_target ${TARGET_SELECTION_LIST})
endif ()

# zlib
find_package(ZLIB)
# dEQP CMake compatibility (as for libpng)
set(ZLIB_INCLUDE_PATH ${ZLIB_INCLUDE_DIRS})
set(ZLIB_LIBRARY ${ZLIB_LIBRARIES})

if (NOT ZLIB_INCLUDE_PATH OR NOT ZLIB_LIBRARY)
	message(STATUS "System version of zlib not found, using external/zlib")
	add_subdirectory(external/zlib EXCLUDE_FROM_ALL)
	# \note ZLIB_LIBRARY and ZLIB_INCLUDE_PATH are promoted from external/zlib/CMakeLists.txt
endif ()

if (EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/external/renderdoc/src/renderdoc_app.h")
	message(STATUS "Renderdoc header found, integration enabled")
	set(DEQP_HAVE_RENDERDOC_HEADER 1)
else ()
	message(STATUS "Renderdoc header not found, integration will not work")
endif ()

include_directories(${ZLIB_INCLUDE_PATH})

# libpng
#
# The FindPNG module defines PNG_INCLUDE_DIRS and PNG_LIBRARIES. But dEQP's
# CMake files expect the non-standard PNG_INCLUDE_PATH and PNG_LIBRARY. Set the
# non-standard variables here to retain compatibility with dEQP's existing
# CMake files.
find_package(PNG)
set(PNG_INCLUDE_PATH ${PNG_INCLUDE_DIRS})
set(PNG_LIBRARY ${PNG_LIBRARIES})

if (NOT PNG_INCLUDE_PATH OR NOT PNG_LIBRARY)
	message(STATUS "System version of libpng not found, using external/libpng")
	add_subdirectory(external/libpng EXCLUDE_FROM_ALL)
	# \note PNG_LIBRARY and PNG_INCLUDE_PATH are promoted from external/libpng/CMakeLists.txt
endif ()

# spirv-tools
add_subdirectory(external/spirv-tools EXCLUDE_FROM_ALL)

# glslang
add_subdirectory(external/glslang EXCLUDE_FROM_ALL)

# spirv-headers
set(SPIRV_INCLUDE_PATH "${PROJECT_SOURCE_DIR}/external/spirv-headers/src/include")
if (NOT EXISTS ${SPIRV_INCLUDE_PATH})
	message(FATAL_ERROR "SPIR-V headers not found, please run external/fetch_sources.py")
endif()
include_directories(${SPIRV_INCLUDE_PATH})

# Amber
add_subdirectory(external/amber EXCLUDE_FROM_ALL)
include_directories(external/amber)

# jsoncpp
add_subdirectory(external/jsoncpp EXCLUDE_FROM_ALL)
include_directories(external/jsoncpp/src/include)

# RenderDoc API
include_directories(external/renderdoc/src)

# Vulkan video support libraries
if (DE_OS_IS_WIN32 OR DE_OS_IS_UNIX)
   # Some of the parsing / codec code is used from here.
   add_subdirectory(external/nvidia-video-samples EXCLUDE_FROM_ALL)
endif()

include_directories(${PNG_INCLUDE_PATH})

# DRM (Direct Rendering Manager) Headers
find_path(XF86DRM_H_PATH NAMES xf86drm.h)
find_path(DRM_H_PATH NAMES drm.h PATH_SUFFIXES libdrm drm)

if (XF86DRM_H_PATH AND DRM_H_PATH)
	include_directories(${XF86DRM_H_PATH})
	include_directories(${DRM_H_PATH})
	set(DEQP_SUPPORT_DRM ON CACHE BOOL "Build code requiring the Linux/Unix Direct Rendering Manager")
	mark_as_advanced(XF86DRM_H_PATH)
	mark_as_advanced(DRM_H_PATH)
else ()
	set(DEQP_SUPPORT_DRM OFF CACHE BOOL "Build code requiring the Linux/Unix Direct Rendering Manager")
endif ()

message(STATUS "DEQP_TARGET_NAME        = ${DEQP_TARGET_NAME}")
message(STATUS "DEQP_SUPPORT_GLES1      = ${DEQP_SUPPORT_GLES1}")
message(STATUS "DEQP_GLES1_LIBRARIES    = ${DEQP_GLES1_LIBRARIES}")
message(STATUS "DEQP_GLES2_LIBRARIES    = ${DEQP_GLES2_LIBRARIES}")
message(STATUS "DEQP_GLES3_LIBRARIES    = ${DEQP_GLES3_LIBRARIES}")
message(STATUS "DEQP_GLES31_LIBRARIES   = ${DEQP_GLES31_LIBRARIES}")
message(STATUS "DEQP_GLES32_LIBRARIES   = ${DEQP_GLES32_LIBRARIES}")
message(STATUS "DEQP_SUPPORT_VG         = ${DEQP_SUPPORT_VG}")
message(STATUS "DEQP_VG_LIBRARIES       = ${DEQP_VG_LIBRARIES}")
message(STATUS "DEQP_EGL_LIBRARIES      = ${DEQP_EGL_LIBRARIES}")
message(STATUS "DEQP_PLATFORM_LIBRARIES = ${DEQP_PLATFORM_LIBRARIES}")
message(STATUS "DEQP_SUPPORT_DRM        = ${DEQP_SUPPORT_DRM}")

# Defines
add_definitions(-DDEQP_TARGET_NAME="${DEQP_TARGET_NAME}")

# Entry points loaded directly by linking to libGL*?
if (DEFINED DEQP_GLES2_LIBRARIES AND GLES_ALLOW_DIRECT_LINK)
	add_definitions(-DDEQP_GLES2_DIRECT_LINK=1)
endif ()

if (DEFINED DEQP_GLES3_LIBRARIES AND GLES_ALLOW_DIRECT_LINK)
	add_definitions(-DDEQP_GLES3_DIRECT_LINK=1)
endif ()

if (DEFINED DEQP_GLES31_LIBRARIES AND GLES_ALLOW_DIRECT_LINK)
	add_definitions(-DDEQP_GLES31_DIRECT_LINK=1)
endif ()

if (DEFINED DEQP_GLES32_LIBRARIES AND GLES_ALLOW_DIRECT_LINK)
	add_definitions(-DDEQP_GLES32_DIRECT_LINK=1)
endif ()

if (DEFINED DEQP_EGL_LIBRARIES AND GLES_ALLOW_DIRECT_LINK)
	add_definitions(-DDEQP_EGL_DIRECT_LINK=1)
endif ()

# Legacy APIs that don't support run-time loading
if (DEQP_SUPPORT_GLES1)
	add_definitions(-DDEQP_SUPPORT_GLES1=1)

	if (NOT DEFINED DEQP_GLES1_LIBRARIES)
		message(FATAL_ERROR "Run-time loading of GLES1 is not supported (DEQP_GLES1_LIBRARIES is not set)")
	endif ()
endif ()

if (DEQP_SUPPORT_VG)
	add_definitions(-DDEQP_SUPPORT_VG=1)

	if (NOT DEFINED DEQP_VG_LIBRARIES)
		message(FATAL_ERROR "Run-time loading of VG is not supported (DEQP_VG_LIBRARIES is not set)")
	endif ()
endif ()

if (DEQP_SUPPORT_DRM)
	add_definitions(-DDEQP_SUPPORT_DRM=1)
else ()
	add_definitions(-DDEQP_SUPPORT_DRM=0)
endif ()

if (DE_COMPILER_IS_MSC)
	# Don't nag about std::copy for example
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_SCL_SECURE_NO_WARNINGS")
endif ()

# Precompiled header macro. Parameters are source file list and filename for pch cpp file.
macro(PCH SRCS PCHCPP)
  if(MSVC AND CMAKE_GENERATOR MATCHES "^Visual Studio")
    set(PCH_NAME "$(IntDir)\\pch.pch")
    # make source files use/depend on PCH_NAME
    set_source_files_properties(${${SRCS}} PROPERTIES COMPILE_FLAGS "/Yupch.h /FIpch.h /Fp${PCH_NAME}" OBJECT_DEPENDS "${PCH_NAME}")
    # make PCHCPP file compile and generate PCH_NAME
    set_source_files_properties(${PCHCPP} PROPERTIES COMPILE_FLAGS "/Ycpch.h /Fp${PCH_NAME}" OBJECT_OUTPUTS "${PCH_NAME}")
    list(APPEND ${SRCS} "${PCHCPP}")
  endif()
endmacro(PCH)

# delibs projects
add_subdirectory(framework/delibs/debase ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(framework/delibs/depool ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(framework/delibs/dethread ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(framework/delibs/destream ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(framework/delibs/deutil ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(framework/delibs/decpp ${MAYBE_EXCLUDE_FROM_ALL})

# ExecServer
add_subdirectory(execserver ${MAYBE_EXCLUDE_FROM_ALL})

# Executor framework and tools
if (IS_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/executor)
	add_subdirectory(executor ${MAYBE_EXCLUDE_FROM_ALL})
endif ()

# Test framework include directories
include_directories(
	framework/common
	framework/qphelper
	framework/opengl
	framework/opengl/wrapper
	framework/referencerenderer
	framework/opengl/simplereference
	framework/randomshaders
	framework/egl
	framework/egl/wrapper
	framework/xexml
	external/vulkancts/framework/vulkan
	)

if (DE_OS_IS_ANDROID OR DE_OS_IS_IOS)
	# On Android deqp modules are compiled as libraries and linked into final .so
	set(DEQP_MODULE_LIBRARIES )
	set(DEQP_MODULE_ENTRY_POINTS )
endif ()

# Macro for adding targets for copying binaries (usually target libraries) to the target destination dir
macro (target_copy_files target dep_name files)
	if (NOT "${files}" STREQUAL "")
		set(COPY_TARGETS )
		foreach (SRCNAME ${files})
			get_filename_component(BASENAME ${SRCNAME} NAME)
			set(DSTNAME "${CMAKE_CURRENT_BINARY_DIR}/${BASENAME}")
			add_custom_command(OUTPUT ${DSTNAME}
							   COMMAND ${CMAKE_COMMAND} -E copy_if_different ${SRCNAME} ${DSTNAME})
			set(COPY_TARGETS ${COPY_TARGETS} ${DSTNAME})
		endforeach ()

		add_custom_target(${dep_name} ALL DEPENDS ${COPY_TARGETS})
		add_dependencies(${target} ${dep_name})
	endif ()
endmacro (target_copy_files)

set(MODULE_LIB_TARGET_POSTFIX	"-package")
set(MODULE_DATA_TARGET_POSTFIX	"-data")

# Macro for adding dEQP module
# This adds 3 targets:
#	${MODULE_NAME}-package:		Static library that contains all SRCS and links to LIBS
#	${MODULE_NAME}-data:		Custom target that is used for data file copies
#	${MODULE_NAME}:				Executable binary (if supported by the platform)
macro (add_deqp_module MODULE_NAME SRCS LIBS EXECLIBS ENTRY)

	# Library target
	add_library("${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}" STATIC ${SRCS})
	target_link_libraries("${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}" ${LIBS})

	set(DEQP_MODULE_LIBRARIES		${DEQP_MODULE_LIBRARIES} "${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}")
	set(DEQP_MODULE_ENTRY_POINTS	${DEQP_MODULE_ENTRY_POINTS} "${CMAKE_CURRENT_SOURCE_DIR}/${ENTRY}")

	# Forward to parent scope
	set(DEQP_MODULE_LIBRARIES		${DEQP_MODULE_LIBRARIES} PARENT_SCOPE)
	set(DEQP_MODULE_ENTRY_POINTS	${DEQP_MODULE_ENTRY_POINTS} PARENT_SCOPE)

	if (NOT DE_OS_IS_ANDROID AND NOT DE_OS_IS_IOS)
		# Executable target
		add_executable(${MODULE_NAME} ${PROJECT_SOURCE_DIR}/framework/platform/tcuMain.cpp ${ENTRY})
		target_link_libraries(${MODULE_NAME} PUBLIC "${EXECLIBS}" "${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}")
		target_copy_files(${MODULE_NAME} platform-libs-${MODULE_NAME} "${DEQP_PLATFORM_COPY_LIBRARIES}")
	endif ()

	# Data file target
	add_custom_target("${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}")
	add_dependencies("${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}" "${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}")
endmacro (add_deqp_module)

# Macro add_deqp_module_skip_android does not add module to DEQP_MODULE_LIBRARIES, so that it is not created on Android.
# It's a temporary solution to disable Vulkan SC tests on Android, because later defined deqp library
# uses both deqp-vk and deqp-vksc and this means that definitions CTS_USES_VULKAN and CTS_USES_VULKANSC
# exist in deqp library at the same time which causes code to not compile.
macro (add_deqp_module_skip_android MODULE_NAME SRCS LIBS EXECLIBS ENTRY)

	# Library target
	add_library("${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}" STATIC ${SRCS})
	target_link_libraries("${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}" ${LIBS})

	if (NOT DE_OS_IS_ANDROID AND NOT DE_OS_IS_IOS)
		# Executable target
		add_executable(${MODULE_NAME} ${PROJECT_SOURCE_DIR}/framework/platform/tcuMain.cpp ${ENTRY})
		target_link_libraries(${MODULE_NAME} PUBLIC "${EXECLIBS}" "${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}")
		target_copy_files(${MODULE_NAME} platform-libs-${MODULE_NAME} "${DEQP_PLATFORM_COPY_LIBRARIES}")
	endif ()

	# Data file target
	add_custom_target("${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}")
	add_dependencies("${MODULE_NAME}${MODULE_LIB_TARGET_POSTFIX}" "${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}")
endmacro (add_deqp_module_skip_android)

# Macro for adding data dirs to module
macro (add_data_dir MODULE_NAME SRC_DIR DST_DIR)
	if (DE_OS_IS_WIN32 OR DE_OS_IS_UNIX OR DE_OS_IS_OSX OR DE_OS_IS_QNX)
		add_custom_command(TARGET "${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}" POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR}/${SRC_DIR} ${CMAKE_CURRENT_BINARY_DIR}/${DST_DIR})

	elseif (DE_OS_IS_ANDROID)
		add_custom_command(TARGET "${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}" POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR}/${SRC_DIR} ${CMAKE_BINARY_DIR}/assets/${DST_DIR})

	elseif (DE_OS_IS_IOS)
		add_custom_command(TARGET "${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}" POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR}/${SRC_DIR} ${CMAKE_BINARY_DIR}/\${CONFIGURATION}\${EFFECTIVE_PLATFORM_NAME}/deqp.app/${DST_DIR})
	endif ()
endmacro (add_data_dir)

# Macro for adding individual data files to module
macro (add_data_file MODULE_NAME SRC_FILE DST_FILE)
	if (DE_OS_IS_WIN32 OR DE_OS_IS_UNIX OR DE_OS_IS_OSX OR DE_OS_IS_QNX)
		add_custom_command(TARGET "${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}" POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy_if_different ${CMAKE_CURRENT_SOURCE_DIR}/${SRC_FILE} ${CMAKE_CURRENT_BINARY_DIR}/${DST_FILE})

	elseif (DE_OS_IS_ANDROID)
		add_custom_command(TARGET "${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}" POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy_if_different ${CMAKE_CURRENT_SOURCE_DIR}/${SRC_FILE} ${CMAKE_BINARY_DIR}/assets/${DST_FILE})

	elseif (DE_OS_IS_IOS)
		add_custom_command(TARGET "${MODULE_NAME}${MODULE_DATA_TARGET_POSTFIX}" POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy_if_different ${CMAKE_CURRENT_SOURCE_DIR}/${SRC_FILE} ${CMAKE_BINARY_DIR}/\${CONFIGURATION}\${EFFECTIVE_PLATFORM_NAME}/deqp.app/${DST_FILE})
	endif ()
endmacro (add_data_file)

add_subdirectory(framework ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(external/vulkancts/framework/vulkan ${MAYBE_EXCLUDE_FROM_ALL})

if (DE_COMPILER_IS_MSC)
	add_compile_options(/bigobj) # Required by glsBuiltinPrecisionTests.cpp
endif ()

add_subdirectory(modules ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(external/vulkancts/modules/vulkan ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(external/vulkancts/vkscserver ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(external/vulkancts/vkscpc ${MAYBE_EXCLUDE_FROM_ALL})
add_subdirectory(external/openglcts ${MAYBE_EXCLUDE_FROM_ALL})

# Single-binary targets
if (DE_OS_IS_ANDROID)
	include_directories(executor)
	include_directories(${PROJECT_BINARY_DIR}/external/vulkancts/framework/vulkan)

	set(DEQP_SRCS
		framework/platform/android/tcuAndroidMain.cpp
		framework/platform/android/tcuAndroidJNI.cpp
		framework/platform/android/tcuAndroidPlatformCapabilityQueryJNI.cpp
		framework/platform/android/tcuTestLogParserJNI.cpp
		${DEQP_MODULE_ENTRY_POINTS}
		)

	set(DEQP_LIBS
		tcutil-platform
		xecore
		${DEQP_MODULE_LIBRARIES}
		)

	add_library(deqp SHARED ${DEQP_SRCS})
	target_link_libraries(deqp ${DEQP_LIBS})

	# Separate out the debug information because it's enormous
	add_custom_command(TARGET deqp POST_BUILD
		COMMAND ${CMAKE_STRIP} --only-keep-debug -o $<TARGET_FILE:deqp>.debug $<TARGET_FILE:deqp>
		COMMAND ${CMAKE_STRIP} -g $<TARGET_FILE:deqp>)

	# Needed by OpenGL CTS that defines its own activity but depends on
	# common Android support code.
	target_include_directories(deqp PRIVATE framework/platform/android)

elseif (DE_OS_IS_IOS)
	# Code sign identity
	set(DEQP_IOS_CODE_SIGN_IDENTITY "drawElements" CACHE STRING "Code sign identity for iOS build")

	set(MACOSX_BUNDLE_PRODUCT_NAME "\${PRODUCT_NAME}")
	set(MACOSX_BUNDLE_GUI_IDENTIFIER "com.drawelements.\${PRODUCT_NAME:identifier}")

	include_directories(framework/platform/ios)
	set(TESTERCORE_SRC_FILES
		framework/platform/ios/tcuEAGLView.h
		framework/platform/ios/tcuEAGLView.m
		framework/platform/ios/tcuIOSAppDelegate.h
		framework/platform/ios/tcuIOSAppDelegate.m
		framework/platform/ios/tcuIOSViewController.h
		framework/platform/ios/tcuIOSViewController.m
		framework/platform/ios/tcuIOSMain.m
		)
	set_source_files_properties(${TESTERCORE_SRC_FILES} COMPILE_FLAGS "-std=c99")

	add_executable(deqp MACOSX_BUNDLE ${TESTERCORE_SRC_FILES} ${DEQP_MODULE_ENTRY_POINTS})
	target_link_libraries(deqp tcutil-platform xscore ${DEQP_MODULE_LIBRARIES})
	set_target_properties(deqp PROPERTIES XCODE_ATTRIBUTE_TARGETED_DEVICE_FAMILY "1,2")
	set_target_properties(deqp PROPERTIES XCODE_ATTRIBUTE_CODE_SIGN_IDENTITY "iPhone Developer: ${DEQP_IOS_CODE_SIGN_IDENTITY}")
endif ()

if (DE_OS_IS_FUCHSIA)
	# The default Fuchsia stack is small, so the size needs to be increased to at
	# least 2**20 bytes, but 2**24 is chosen for safety.
	set_target_properties(deqp-vk PROPERTIES LINK_FLAGS "-Wl,-z,stack-size=0x1000000")
endif ()
