/* WARNING: This is auto-generated file. Do not modify, since changes will
 * be lost! Modify the generating script instead.
 * This file was generated by /scripts/gen_framework.py
 */

#include "tcuCommandLine.hpp"
#include "vktTestCase.hpp"
#include "vkPlatform.hpp"
#include "vkDeviceUtil.hpp"
#include "vkQueryUtil.hpp"
#include "vktCustomInstancesDevices.hpp"
#include "vktTestCase.hpp"
#include "vktTestCaseUtil.hpp"

namespace vkt
{

using namespace vk;

tcu::TestStatus		testGetDeviceProcAddr		(Context& context)
{
	tcu::TestLog&								log						(context.getTestContext().getLog());
	const PlatformInterface&					platformInterface		= context.getPlatformInterface();
	const auto									validationEnabled		= context.getTestContext().getCommandLine().isValidationEnabled();
	const CustomInstance						instance				(createCustomInstanceFromContext(context));
	const InstanceDriver&						instanceDriver			= instance.getDriver();
	const VkPhysicalDevice						physicalDevice			= chooseDevice(instanceDriver, instance, context.getTestContext().getCommandLine());
	const deUint32								queueFamilyIndex		= 0;
	const deUint32								queueCount				= 1;
	const float									queuePriority			= 1.0f;
	const std::vector<VkQueueFamilyProperties>	queueFamilyProperties	= getPhysicalDeviceQueueFamilyProperties(instanceDriver, physicalDevice);

	const VkDeviceQueueCreateInfo			deviceQueueCreateInfo =
	{
		VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO,	//  VkStructureType				sType;
		DE_NULL,									//  const void*					pNext;
		(VkDeviceQueueCreateFlags)0u,				//  VkDeviceQueueCreateFlags	flags;
		queueFamilyIndex,							//  deUint32					queueFamilyIndex;
		queueCount,									//  deUint32					queueCount;
		&queuePriority,								//  const float*				pQueuePriorities;
	};

	const VkDeviceCreateInfo				deviceCreateInfo =
	{
		VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO,		//  VkStructureType					sType;
		DE_NULL,									//  const void*						pNext;
		(VkDeviceCreateFlags)0u,					//  VkDeviceCreateFlags				flags;
		1u,											//  deUint32						queueCreateInfoCount;
		&deviceQueueCreateInfo,						//  const VkDeviceQueueCreateInfo*	pQueueCreateInfos;
		0u,											//  deUint32						enabledLayerCount;
		DE_NULL,									//  const char* const*				ppEnabledLayerNames;
		0u,											//  deUint32						enabledExtensionCount;
		DE_NULL,									//  const char* const*				ppEnabledExtensionNames;
		DE_NULL,									//  const VkPhysicalDeviceFeatures*	pEnabledFeatures;
	};
	const Unique<VkDevice>					device			(createCustomDevice(validationEnabled, platformInterface, instance, instanceDriver, physicalDevice, &deviceCreateInfo));
	const DeviceDriver						deviceDriver	(platformInterface, instance, device.get(), context.getUsedApiVersion());

	const std::vector<std::string> loaderExceptions{
		"vkSetDebugUtilsObjectNameEXT",
		"vkSetDebugUtilsObjectTagEXT",
		"vkQueueBeginDebugUtilsLabelEXT",
		"vkQueueEndDebugUtilsLabelEXT",
		"vkQueueInsertDebugUtilsLabelEXT",
		"vkCmdBeginDebugUtilsLabelEXT",
		"vkCmdEndDebugUtilsLabelEXT",
		"vkCmdInsertDebugUtilsLabelEXT",
	};

	const std::vector<std::string> functions{
		"vkDestroySurfaceKHR",
		"vkGetPhysicalDeviceSurfaceSupportKHR",
		"vkGetPhysicalDeviceSurfaceCapabilitiesKHR",
		"vkGetPhysicalDeviceSurfaceFormatsKHR",
		"vkGetPhysicalDeviceSurfacePresentModesKHR",
		"vkCreateSwapchainKHR",
		"vkDestroySwapchainKHR",
		"vkGetSwapchainImagesKHR",
		"vkAcquireNextImageKHR",
		"vkQueuePresentKHR",
		"vkGetDeviceGroupPresentCapabilitiesKHR",
		"vkGetDeviceGroupSurfacePresentModesKHR",
		"vkGetPhysicalDevicePresentRectanglesKHR",
		"vkAcquireNextImage2KHR",
		"vkGetPhysicalDeviceDisplayPropertiesKHR",
		"vkGetPhysicalDeviceDisplayPlanePropertiesKHR",
		"vkGetDisplayPlaneSupportedDisplaysKHR",
		"vkGetDisplayModePropertiesKHR",
		"vkCreateDisplayModeKHR",
		"vkGetDisplayPlaneCapabilitiesKHR",
		"vkCreateDisplayPlaneSurfaceKHR",
		"vkCreateSharedSwapchainsKHR",
		"vkGetMemoryFdKHR",
		"vkGetMemoryFdPropertiesKHR",
		"vkImportSemaphoreFdKHR",
		"vkGetSemaphoreFdKHR",
		"vkReleaseDisplayEXT",
		"vkGetPhysicalDeviceSurfaceCapabilities2EXT",
		"vkDisplayPowerControlEXT",
		"vkRegisterDeviceEventEXT",
		"vkRegisterDisplayEventEXT",
		"vkGetSwapchainCounterEXT",
		"vkCmdSetDiscardRectangleEXT",
		"vkCmdSetDiscardRectangleEnableEXT",
		"vkCmdSetDiscardRectangleModeEXT",
		"vkSetHdrMetadataEXT",
		"vkGetSwapchainStatusKHR",
		"vkImportFenceFdKHR",
		"vkGetFenceFdKHR",
		"vkEnumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR",
		"vkGetPhysicalDeviceQueueFamilyPerformanceQueryPassesKHR",
		"vkAcquireProfilingLockKHR",
		"vkReleaseProfilingLockKHR",
		"vkGetPhysicalDeviceSurfaceCapabilities2KHR",
		"vkGetPhysicalDeviceSurfaceFormats2KHR",
		"vkGetPhysicalDeviceDisplayProperties2KHR",
		"vkGetPhysicalDeviceDisplayPlaneProperties2KHR",
		"vkGetDisplayModeProperties2KHR",
		"vkGetDisplayPlaneCapabilities2KHR",
		"vkSetDebugUtilsObjectNameEXT",
		"vkSetDebugUtilsObjectTagEXT",
		"vkQueueBeginDebugUtilsLabelEXT",
		"vkQueueEndDebugUtilsLabelEXT",
		"vkQueueInsertDebugUtilsLabelEXT",
		"vkCmdBeginDebugUtilsLabelEXT",
		"vkCmdEndDebugUtilsLabelEXT",
		"vkCmdInsertDebugUtilsLabelEXT",
		"vkCreateDebugUtilsMessengerEXT",
		"vkDestroyDebugUtilsMessengerEXT",
		"vkSubmitDebugUtilsMessageEXT",
		"vkCmdSetSampleLocationsEXT",
		"vkGetPhysicalDeviceMultisamplePropertiesEXT",
		"vkGetImageDrmFormatModifierPropertiesEXT",
		"vkGetMemoryHostPointerPropertiesEXT",
		"vkGetPhysicalDeviceCalibrateableTimeDomainsEXT",
		"vkGetCalibratedTimestampsEXT",
		"vkGetPhysicalDeviceFragmentShadingRatesKHR",
		"vkCmdSetFragmentShadingRateKHR",
		"vkCreateHeadlessSurfaceEXT",
		"vkCmdSetLineStippleEXT",
		"vkCmdSetCullModeEXT",
		"vkCmdSetFrontFaceEXT",
		"vkCmdSetPrimitiveTopologyEXT",
		"vkCmdSetViewportWithCountEXT",
		"vkCmdSetScissorWithCountEXT",
		"vkCmdBindVertexBuffers2EXT",
		"vkCmdSetDepthTestEnableEXT",
		"vkCmdSetDepthWriteEnableEXT",
		"vkCmdSetDepthCompareOpEXT",
		"vkCmdSetDepthBoundsTestEnableEXT",
		"vkCmdSetStencilTestEnableEXT",
		"vkCmdSetStencilOpEXT",
		"vkCmdRefreshObjectsKHR",
		"vkGetPhysicalDeviceRefreshableObjectTypesKHR",
		"vkCmdSetEvent2KHR",
		"vkCmdResetEvent2KHR",
		"vkCmdWaitEvents2KHR",
		"vkCmdPipelineBarrier2KHR",
		"vkCmdWriteTimestamp2KHR",
		"vkQueueSubmit2KHR",
		"vkCmdWriteBufferMarker2AMD",
		"vkGetQueueCheckpointData2NV",
		"vkCmdCopyBuffer2KHR",
		"vkCmdCopyImage2KHR",
		"vkCmdCopyBufferToImage2KHR",
		"vkCmdCopyImageToBuffer2KHR",
		"vkCmdBlitImage2KHR",
		"vkCmdResolveImage2KHR",
		"vkCmdSetVertexInputEXT",
		"vkGetFenceSciSyncFenceNV",
		"vkGetFenceSciSyncObjNV",
		"vkImportFenceSciSyncFenceNV",
		"vkImportFenceSciSyncObjNV",
		"vkGetPhysicalDeviceSciSyncAttributesNV",
		"vkGetSemaphoreSciSyncObjNV",
		"vkImportSemaphoreSciSyncObjNV",
		"vkGetMemorySciBufNV",
		"vkGetPhysicalDeviceExternalMemorySciBufPropertiesNV",
		"vkGetPhysicalDeviceSciBufAttributesNV",
		"vkCmdSetPatchControlPointsEXT",
		"vkCmdSetRasterizerDiscardEnableEXT",
		"vkCmdSetDepthBiasEnableEXT",
		"vkCmdSetLogicOpEXT",
		"vkCmdSetPrimitiveRestartEnableEXT",
		"vkCmdSetColorWriteEnableEXT",
		"vkCreateSemaphoreSciSyncPoolNV",
		"vkDestroySemaphoreSciSyncPoolNV",
		"vkGetFenceSciSyncFenceNV",
		"vkGetFenceSciSyncObjNV",
		"vkImportFenceSciSyncFenceNV",
		"vkImportFenceSciSyncObjNV",
		"vkGetPhysicalDeviceSciSyncAttributesNV",
		"vkGetScreenBufferPropertiesQNX",
		"vkCmdSetCheckpointNV",
		"vkGetQueueCheckpointDataNV",
	};

	bool fail = false;
	for (const auto& function : functions)
	{
		if (std::find(loaderExceptions.begin(), loaderExceptions.end(), function) != loaderExceptions.end())
		{
			continue;
		}
		if (deviceDriver.getDeviceProcAddr(device.get(), function.c_str()) != DE_NULL)
		{
			fail = true;
			log << tcu::TestLog::Message << "Function " << function << " is not NULL" << tcu::TestLog::EndMessage;
		}
	}
	if (fail)
		return tcu::TestStatus::fail("Fail");
	return tcu::TestStatus::pass("All functions are NULL");
}

void addGetDeviceProcAddrTests (tcu::TestCaseGroup* testGroup)
{
	addFunctionCase(testGroup, "non_enabled", "GetDeviceProcAddr", testGetDeviceProcAddr);
}

}

