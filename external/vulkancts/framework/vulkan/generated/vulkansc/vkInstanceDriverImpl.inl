/* WARNING: This is auto-generated file. Do not modify, since changes will
 * be lost! Modify the generating script instead.
 * This file was generated by /scripts/gen_framework.py
 */


void InstanceDriver::destroyInstance (VkInstance instance, const VkAllocationCallbacks* pAllocator) const
{
	m_vk.destroyInstance(instance, pAllocator);
}

VkResult InstanceDriver::enumeratePhysicalDevices (VkInstance instance, uint32_t* pPhysicalDeviceCount, VkPhysicalDevice* pPhysicalDevices) const
{
	return m_vk.enumeratePhysicalDevices(instance, pPhysicalDeviceCount, pPhysicalDevices);
}

void InstanceDriver::getPhysicalDeviceProperties (VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties* pProperties) const
{
	m_vk.getPhysicalDeviceProperties(physicalDevice, pProperties);
}

void InstanceDriver::getPhysicalDeviceQueueFamilyProperties (VkPhysicalDevice physicalDevice, uint32_t* pQueueFamilyPropertyCount, VkQueueFamilyProperties* pQueueFamilyProperties) const
{
	m_vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, pQueueFamilyProperties);
}

void InstanceDriver::getPhysicalDeviceMemoryProperties (VkPhysicalDevice physicalDevice, VkPhysicalDeviceMemoryProperties* pMemoryProperties) const
{
	m_vk.getPhysicalDeviceMemoryProperties(physicalDevice, pMemoryProperties);
}

void InstanceDriver::getPhysicalDeviceFeatures (VkPhysicalDevice physicalDevice, VkPhysicalDeviceFeatures* pFeatures) const
{
	m_vk.getPhysicalDeviceFeatures(physicalDevice, pFeatures);
}

void InstanceDriver::getPhysicalDeviceFormatProperties (VkPhysicalDevice physicalDevice, VkFormat format, VkFormatProperties* pFormatProperties) const
{
	m_vk.getPhysicalDeviceFormatProperties(physicalDevice, format, pFormatProperties);
}

VkResult InstanceDriver::getPhysicalDeviceImageFormatProperties (VkPhysicalDevice physicalDevice, VkFormat format, VkImageType type, VkImageTiling tiling, VkImageUsageFlags usage, VkImageCreateFlags flags, VkImageFormatProperties* pImageFormatProperties) const
{
	return m_vk.getPhysicalDeviceImageFormatProperties(physicalDevice, format, type, tiling, usage, flags, pImageFormatProperties);
}

VkResult InstanceDriver::createDevice (VkPhysicalDevice physicalDevice, const VkDeviceCreateInfo* pCreateInfo, const VkAllocationCallbacks* pAllocator, VkDevice* pDevice) const
{
	return m_vk.createDevice(physicalDevice, pCreateInfo, pAllocator, pDevice);
}

VkResult InstanceDriver::enumerateDeviceLayerProperties (VkPhysicalDevice physicalDevice, uint32_t* pPropertyCount, VkLayerProperties* pProperties) const
{
	return m_vk.enumerateDeviceLayerProperties(physicalDevice, pPropertyCount, pProperties);
}

VkResult InstanceDriver::enumerateDeviceExtensionProperties (VkPhysicalDevice physicalDevice, const char* pLayerName, uint32_t* pPropertyCount, VkExtensionProperties* pProperties) const
{
	return m_vk.enumerateDeviceExtensionProperties(physicalDevice, pLayerName, pPropertyCount, pProperties);
}

VkResult InstanceDriver::getPhysicalDeviceDisplayPropertiesKHR (VkPhysicalDevice physicalDevice, uint32_t* pPropertyCount, VkDisplayPropertiesKHR* pProperties) const
{
	return m_vk.getPhysicalDeviceDisplayPropertiesKHR(physicalDevice, pPropertyCount, pProperties);
}

VkResult InstanceDriver::getPhysicalDeviceDisplayPlanePropertiesKHR (VkPhysicalDevice physicalDevice, uint32_t* pPropertyCount, VkDisplayPlanePropertiesKHR* pProperties) const
{
	return m_vk.getPhysicalDeviceDisplayPlanePropertiesKHR(physicalDevice, pPropertyCount, pProperties);
}

VkResult InstanceDriver::getDisplayPlaneSupportedDisplaysKHR (VkPhysicalDevice physicalDevice, uint32_t planeIndex, uint32_t* pDisplayCount, VkDisplayKHR* pDisplays) const
{
	return m_vk.getDisplayPlaneSupportedDisplaysKHR(physicalDevice, planeIndex, pDisplayCount, pDisplays);
}

VkResult InstanceDriver::getDisplayModePropertiesKHR (VkPhysicalDevice physicalDevice, VkDisplayKHR display, uint32_t* pPropertyCount, VkDisplayModePropertiesKHR* pProperties) const
{
	return m_vk.getDisplayModePropertiesKHR(physicalDevice, display, pPropertyCount, pProperties);
}

VkResult InstanceDriver::createDisplayModeKHR (VkPhysicalDevice physicalDevice, VkDisplayKHR display, const VkDisplayModeCreateInfoKHR* pCreateInfo, const VkAllocationCallbacks* pAllocator, VkDisplayModeKHR* pMode) const
{
	return m_vk.createDisplayModeKHR(physicalDevice, display, pCreateInfo, pAllocator, pMode);
}

VkResult InstanceDriver::getDisplayPlaneCapabilitiesKHR (VkPhysicalDevice physicalDevice, VkDisplayModeKHR mode, uint32_t planeIndex, VkDisplayPlaneCapabilitiesKHR* pCapabilities) const
{
	return m_vk.getDisplayPlaneCapabilitiesKHR(physicalDevice, mode, planeIndex, pCapabilities);
}

VkResult InstanceDriver::createDisplayPlaneSurfaceKHR (VkInstance instance, const VkDisplaySurfaceCreateInfoKHR* pCreateInfo, const VkAllocationCallbacks* pAllocator, VkSurfaceKHR* pSurface) const
{
	return m_vk.createDisplayPlaneSurfaceKHR(instance, pCreateInfo, pAllocator, pSurface);
}

void InstanceDriver::destroySurfaceKHR (VkInstance instance, VkSurfaceKHR surface, const VkAllocationCallbacks* pAllocator) const
{
	m_vk.destroySurfaceKHR(instance, surface, pAllocator);
}

VkResult InstanceDriver::getPhysicalDeviceSurfaceSupportKHR (VkPhysicalDevice physicalDevice, uint32_t queueFamilyIndex, VkSurfaceKHR surface, VkBool32* pSupported) const
{
	return m_vk.getPhysicalDeviceSurfaceSupportKHR(physicalDevice, queueFamilyIndex, surface, pSupported);
}

VkResult InstanceDriver::getPhysicalDeviceSurfaceCapabilitiesKHR (VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, VkSurfaceCapabilitiesKHR* pSurfaceCapabilities) const
{
	return m_vk.getPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, pSurfaceCapabilities);
}

VkResult InstanceDriver::getPhysicalDeviceSurfaceFormatsKHR (VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, uint32_t* pSurfaceFormatCount, VkSurfaceFormatKHR* pSurfaceFormats) const
{
	return m_vk.getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pSurfaceFormatCount, pSurfaceFormats);
}

VkResult InstanceDriver::getPhysicalDeviceSurfacePresentModesKHR (VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, uint32_t* pPresentModeCount, VkPresentModeKHR* pPresentModes) const
{
	return m_vk.getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pPresentModeCount, pPresentModes);
}

void InstanceDriver::getPhysicalDeviceFeatures2 (VkPhysicalDevice physicalDevice, VkPhysicalDeviceFeatures2* pFeatures) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		m_vk.getPhysicalDeviceFeatures2(physicalDevice, pFeatures);
	else
		m_vk.getPhysicalDeviceFeatures2KHR(physicalDevice, pFeatures);
}

void InstanceDriver::getPhysicalDeviceProperties2 (VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties2* pProperties) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		m_vk.getPhysicalDeviceProperties2(physicalDevice, pProperties);
	else
		m_vk.getPhysicalDeviceProperties2KHR(physicalDevice, pProperties);
}

void InstanceDriver::getPhysicalDeviceFormatProperties2 (VkPhysicalDevice physicalDevice, VkFormat format, VkFormatProperties2* pFormatProperties) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		m_vk.getPhysicalDeviceFormatProperties2(physicalDevice, format, pFormatProperties);
	else
		m_vk.getPhysicalDeviceFormatProperties2KHR(physicalDevice, format, pFormatProperties);
}

VkResult InstanceDriver::getPhysicalDeviceImageFormatProperties2 (VkPhysicalDevice physicalDevice, const VkPhysicalDeviceImageFormatInfo2* pImageFormatInfo, VkImageFormatProperties2* pImageFormatProperties) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		return m_vk.getPhysicalDeviceImageFormatProperties2(physicalDevice, pImageFormatInfo, pImageFormatProperties);
	else
		return m_vk.getPhysicalDeviceImageFormatProperties2KHR(physicalDevice, pImageFormatInfo, pImageFormatProperties);
}

void InstanceDriver::getPhysicalDeviceQueueFamilyProperties2 (VkPhysicalDevice physicalDevice, uint32_t* pQueueFamilyPropertyCount, VkQueueFamilyProperties2* pQueueFamilyProperties) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		m_vk.getPhysicalDeviceQueueFamilyProperties2(physicalDevice, pQueueFamilyPropertyCount, pQueueFamilyProperties);
	else
		m_vk.getPhysicalDeviceQueueFamilyProperties2KHR(physicalDevice, pQueueFamilyPropertyCount, pQueueFamilyProperties);
}

void InstanceDriver::getPhysicalDeviceMemoryProperties2 (VkPhysicalDevice physicalDevice, VkPhysicalDeviceMemoryProperties2* pMemoryProperties) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		m_vk.getPhysicalDeviceMemoryProperties2(physicalDevice, pMemoryProperties);
	else
		m_vk.getPhysicalDeviceMemoryProperties2KHR(physicalDevice, pMemoryProperties);
}

void InstanceDriver::getPhysicalDeviceExternalBufferProperties (VkPhysicalDevice physicalDevice, const VkPhysicalDeviceExternalBufferInfo* pExternalBufferInfo, VkExternalBufferProperties* pExternalBufferProperties) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		m_vk.getPhysicalDeviceExternalBufferProperties(physicalDevice, pExternalBufferInfo, pExternalBufferProperties);
	else
		m_vk.getPhysicalDeviceExternalBufferPropertiesKHR(physicalDevice, pExternalBufferInfo, pExternalBufferProperties);
}

VkResult InstanceDriver::getPhysicalDeviceExternalMemorySciBufPropertiesNV (VkPhysicalDevice physicalDevice, VkExternalMemoryHandleTypeFlagBits handleType, pt::NvSciBufObj handle, VkMemorySciBufPropertiesNV* pMemorySciBufProperties) const
{
	return m_vk.getPhysicalDeviceExternalMemorySciBufPropertiesNV(physicalDevice, handleType, handle, pMemorySciBufProperties);
}

VkResult InstanceDriver::getPhysicalDeviceSciBufAttributesNV (VkPhysicalDevice physicalDevice, pt::NvSciBufAttrList pAttributes) const
{
	return m_vk.getPhysicalDeviceSciBufAttributesNV(physicalDevice, pAttributes);
}

void InstanceDriver::getPhysicalDeviceExternalSemaphoreProperties (VkPhysicalDevice physicalDevice, const VkPhysicalDeviceExternalSemaphoreInfo* pExternalSemaphoreInfo, VkExternalSemaphoreProperties* pExternalSemaphoreProperties) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		m_vk.getPhysicalDeviceExternalSemaphoreProperties(physicalDevice, pExternalSemaphoreInfo, pExternalSemaphoreProperties);
	else
		m_vk.getPhysicalDeviceExternalSemaphorePropertiesKHR(physicalDevice, pExternalSemaphoreInfo, pExternalSemaphoreProperties);
}

void InstanceDriver::getPhysicalDeviceExternalFenceProperties (VkPhysicalDevice physicalDevice, const VkPhysicalDeviceExternalFenceInfo* pExternalFenceInfo, VkExternalFenceProperties* pExternalFenceProperties) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		m_vk.getPhysicalDeviceExternalFenceProperties(physicalDevice, pExternalFenceInfo, pExternalFenceProperties);
	else
		m_vk.getPhysicalDeviceExternalFencePropertiesKHR(physicalDevice, pExternalFenceInfo, pExternalFenceProperties);
}

VkResult InstanceDriver::getPhysicalDeviceSciSyncAttributesNV (VkPhysicalDevice physicalDevice, const VkSciSyncAttributesInfoNV* pSciSyncAttributesInfo, pt::NvSciSyncAttrList pAttributes) const
{
	return m_vk.getPhysicalDeviceSciSyncAttributesNV(physicalDevice, pSciSyncAttributesInfo, pAttributes);
}

VkResult InstanceDriver::releaseDisplayEXT (VkPhysicalDevice physicalDevice, VkDisplayKHR display) const
{
	return m_vk.releaseDisplayEXT(physicalDevice, display);
}

VkResult InstanceDriver::getPhysicalDeviceSurfaceCapabilities2EXT (VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, VkSurfaceCapabilities2EXT* pSurfaceCapabilities) const
{
	return m_vk.getPhysicalDeviceSurfaceCapabilities2EXT(physicalDevice, surface, pSurfaceCapabilities);
}

VkResult InstanceDriver::enumeratePhysicalDeviceGroups (VkInstance instance, uint32_t* pPhysicalDeviceGroupCount, VkPhysicalDeviceGroupProperties* pPhysicalDeviceGroupProperties) const
{
	return m_vk.enumeratePhysicalDeviceGroups(instance, pPhysicalDeviceGroupCount, pPhysicalDeviceGroupProperties);
}

VkResult InstanceDriver::getPhysicalDevicePresentRectanglesKHR (VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, uint32_t* pRectCount, VkRect2D* pRects) const
{
	return m_vk.getPhysicalDevicePresentRectanglesKHR(physicalDevice, surface, pRectCount, pRects);
}

void InstanceDriver::getPhysicalDeviceMultisamplePropertiesEXT (VkPhysicalDevice physicalDevice, VkSampleCountFlagBits samples, VkMultisamplePropertiesEXT* pMultisampleProperties) const
{
	m_vk.getPhysicalDeviceMultisamplePropertiesEXT(physicalDevice, samples, pMultisampleProperties);
}

VkResult InstanceDriver::getPhysicalDeviceSurfaceCapabilities2KHR (VkPhysicalDevice physicalDevice, const VkPhysicalDeviceSurfaceInfo2KHR* pSurfaceInfo, VkSurfaceCapabilities2KHR* pSurfaceCapabilities) const
{
	return m_vk.getPhysicalDeviceSurfaceCapabilities2KHR(physicalDevice, pSurfaceInfo, pSurfaceCapabilities);
}

VkResult InstanceDriver::getPhysicalDeviceSurfaceFormats2KHR (VkPhysicalDevice physicalDevice, const VkPhysicalDeviceSurfaceInfo2KHR* pSurfaceInfo, uint32_t* pSurfaceFormatCount, VkSurfaceFormat2KHR* pSurfaceFormats) const
{
	return m_vk.getPhysicalDeviceSurfaceFormats2KHR(physicalDevice, pSurfaceInfo, pSurfaceFormatCount, pSurfaceFormats);
}

VkResult InstanceDriver::getPhysicalDeviceDisplayProperties2KHR (VkPhysicalDevice physicalDevice, uint32_t* pPropertyCount, VkDisplayProperties2KHR* pProperties) const
{
	return m_vk.getPhysicalDeviceDisplayProperties2KHR(physicalDevice, pPropertyCount, pProperties);
}

VkResult InstanceDriver::getPhysicalDeviceDisplayPlaneProperties2KHR (VkPhysicalDevice physicalDevice, uint32_t* pPropertyCount, VkDisplayPlaneProperties2KHR* pProperties) const
{
	return m_vk.getPhysicalDeviceDisplayPlaneProperties2KHR(physicalDevice, pPropertyCount, pProperties);
}

VkResult InstanceDriver::getDisplayModeProperties2KHR (VkPhysicalDevice physicalDevice, VkDisplayKHR display, uint32_t* pPropertyCount, VkDisplayModeProperties2KHR* pProperties) const
{
	return m_vk.getDisplayModeProperties2KHR(physicalDevice, display, pPropertyCount, pProperties);
}

VkResult InstanceDriver::getDisplayPlaneCapabilities2KHR (VkPhysicalDevice physicalDevice, const VkDisplayPlaneInfo2KHR* pDisplayPlaneInfo, VkDisplayPlaneCapabilities2KHR* pCapabilities) const
{
	return m_vk.getDisplayPlaneCapabilities2KHR(physicalDevice, pDisplayPlaneInfo, pCapabilities);
}

VkResult InstanceDriver::getPhysicalDeviceCalibrateableTimeDomainsKHR (VkPhysicalDevice physicalDevice, uint32_t* pTimeDomainCount, VkTimeDomainKHR* pTimeDomains) const
{
	vk::VkPhysicalDeviceProperties props;
	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);
	if (props.apiVersion >= VK_API_VERSION_1_1)
		return m_vk.getPhysicalDeviceCalibrateableTimeDomainsKHR(physicalDevice, pTimeDomainCount, pTimeDomains);
	else
		return m_vk.getPhysicalDeviceCalibrateableTimeDomainsEXT(physicalDevice, pTimeDomainCount, pTimeDomains);
}

VkResult InstanceDriver::createDebugUtilsMessengerEXT (VkInstance instance, const VkDebugUtilsMessengerCreateInfoEXT* pCreateInfo, const VkAllocationCallbacks* pAllocator, VkDebugUtilsMessengerEXT* pMessenger) const
{
	return m_vk.createDebugUtilsMessengerEXT(instance, pCreateInfo, pAllocator, pMessenger);
}

void InstanceDriver::destroyDebugUtilsMessengerEXT (VkInstance instance, VkDebugUtilsMessengerEXT messenger, const VkAllocationCallbacks* pAllocator) const
{
	m_vk.destroyDebugUtilsMessengerEXT(instance, messenger, pAllocator);
}

void InstanceDriver::submitDebugUtilsMessageEXT (VkInstance instance, VkDebugUtilsMessageSeverityFlagBitsEXT messageSeverity, VkDebugUtilsMessageTypeFlagsEXT messageTypes, const VkDebugUtilsMessengerCallbackDataEXT* pCallbackData) const
{
	m_vk.submitDebugUtilsMessageEXT(instance, messageSeverity, messageTypes, pCallbackData);
}

VkResult InstanceDriver::enumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR (VkPhysicalDevice physicalDevice, uint32_t queueFamilyIndex, uint32_t* pCounterCount, VkPerformanceCounterKHR* pCounters, VkPerformanceCounterDescriptionKHR* pCounterDescriptions) const
{
	return m_vk.enumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR(physicalDevice, queueFamilyIndex, pCounterCount, pCounters, pCounterDescriptions);
}

void InstanceDriver::getPhysicalDeviceQueueFamilyPerformanceQueryPassesKHR (VkPhysicalDevice physicalDevice, const VkQueryPoolPerformanceCreateInfoKHR* pPerformanceQueryCreateInfo, uint32_t* pNumPasses) const
{
	m_vk.getPhysicalDeviceQueueFamilyPerformanceQueryPassesKHR(physicalDevice, pPerformanceQueryCreateInfo, pNumPasses);
}

VkResult InstanceDriver::createHeadlessSurfaceEXT (VkInstance instance, const VkHeadlessSurfaceCreateInfoEXT* pCreateInfo, const VkAllocationCallbacks* pAllocator, VkSurfaceKHR* pSurface) const
{
	return m_vk.createHeadlessSurfaceEXT(instance, pCreateInfo, pAllocator, pSurface);
}

VkResult InstanceDriver::getPhysicalDeviceRefreshableObjectTypesKHR (VkPhysicalDevice physicalDevice, uint32_t* pRefreshableObjectTypeCount, VkObjectType* pRefreshableObjectTypes) const
{
	return m_vk.getPhysicalDeviceRefreshableObjectTypesKHR(physicalDevice, pRefreshableObjectTypeCount, pRefreshableObjectTypes);
}

VkResult InstanceDriver::getPhysicalDeviceFragmentShadingRatesKHR (VkPhysicalDevice physicalDevice, uint32_t* pFragmentShadingRateCount, VkPhysicalDeviceFragmentShadingRateKHR* pFragmentShadingRates) const
{
	return m_vk.getPhysicalDeviceFragmentShadingRatesKHR(physicalDevice, pFragmentShadingRateCount, pFragmentShadingRates);
}
