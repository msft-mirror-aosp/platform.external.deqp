/* WARNING: This is auto-generated file. Do not modify, since changes will
 * be lost! Modify the generating script instead.
 * This file was generated by /scripts/gen_framework.py
 */
#include "vkDeviceProperties.hpp"

namespace vk
{
#define VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME "VK_KHR_acceleration_structure"
#define VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME "VK_EXT_blend_operation_advanced"
#define VK_EXT_CONSERVATIVE_RASTERIZATION_EXTENSION_NAME "VK_EXT_conservative_rasterization"
#define VK_NV_COOPERATIVE_MATRIX_EXTENSION_NAME "VK_NV_cooperative_matrix"
#define VK_EXT_CUSTOM_BORDER_COLOR_EXTENSION_NAME "VK_EXT_custom_border_color"
#define VK_KHR_DEPTH_STENCIL_RESOLVE_EXTENSION_NAME "VK_KHR_depth_stencil_resolve"
#define VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME "VK_EXT_descriptor_indexing"
#define VK_NV_DEVICE_GENERATED_COMMANDS_EXTENSION_NAME "VK_NV_device_generated_commands"
#define VK_EXT_DISCARD_RECTANGLES_EXTENSION_NAME "VK_EXT_discard_rectangles"
#define VK_KHR_DRIVER_PROPERTIES_EXTENSION_NAME "VK_KHR_driver_properties"
#define VK_EXT_EXTERNAL_MEMORY_HOST_EXTENSION_NAME "VK_EXT_external_memory_host"
#define VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME "VK_KHR_shader_float_controls"
#define VK_EXT_FRAGMENT_DENSITY_MAP_EXTENSION_NAME "VK_EXT_fragment_density_map"
#define VK_EXT_FRAGMENT_DENSITY_MAP_2_EXTENSION_NAME "VK_EXT_fragment_density_map2"
#define VK_KHR_FRAGMENT_SHADING_RATE_EXTENSION_NAME "VK_KHR_fragment_shading_rate"
#define VK_NV_FRAGMENT_SHADING_RATE_ENUMS_EXTENSION_NAME "VK_NV_fragment_shading_rate_enums"
#define DECL_ID_EXTENSION_NAME "core_property"
#define VK_EXT_INLINE_UNIFORM_BLOCK_EXTENSION_NAME "VK_EXT_inline_uniform_block"
#define VK_EXT_LINE_RASTERIZATION_EXTENSION_NAME "VK_EXT_line_rasterization"
#define VK_KHR_MAINTENANCE3_EXTENSION_NAME "VK_KHR_maintenance3"
#define VK_NV_MESH_SHADER_EXTENSION_NAME  "VK_NV_mesh_shader"
#define VK_KHR_MULTIVIEW_EXTENSION_NAME   "VK_KHR_multiview"
#define VK_NVX_MULTIVIEW_PER_VIEW_ATTRIBUTES_EXTENSION_NAME "VK_NVX_multiview_per_view_attributes"
#define VK_KHR_PERFORMANCE_QUERY_EXTENSION_NAME "VK_KHR_performance_query"
#define VK_KHR_MAINTENANCE2_EXTENSION_NAME "VK_KHR_maintenance2"
#define VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME "VK_KHR_portability_subset"
#define DECL_PROTECTED_MEMORY_EXTENSION_NAME "core_property"
#define VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME "VK_KHR_push_descriptor"
#define VK_NV_RAY_TRACING_EXTENSION_NAME  "VK_NV_ray_tracing"
#define VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME "VK_KHR_ray_tracing_pipeline"
#define VK_EXT_ROBUSTNESS_2_EXTENSION_NAME "VK_EXT_robustness2"
#define VK_EXT_SAMPLER_FILTER_MINMAX_EXTENSION_NAME "VK_EXT_sampler_filter_minmax"
#define VK_EXT_SAMPLE_LOCATIONS_EXTENSION_NAME "VK_EXT_sample_locations"
#define VK_AMD_SHADER_CORE_PROPERTIES_EXTENSION_NAME "VK_AMD_shader_core_properties"
#define VK_AMD_SHADER_CORE_PROPERTIES_2_EXTENSION_NAME "VK_AMD_shader_core_properties2"
#define VK_KHR_SHADER_INTEGER_DOT_PRODUCT_EXTENSION_NAME "VK_KHR_shader_integer_dot_product"
#define VK_NV_SHADING_RATE_IMAGE_EXTENSION_NAME "VK_NV_shading_rate_image"
#define DECL_SUBGROUP_EXTENSION_NAME "core_property"
#define VK_EXT_SUBGROUP_SIZE_CONTROL_EXTENSION_NAME "VK_EXT_subgroup_size_control"
#define VK_EXT_TEXEL_BUFFER_ALIGNMENT_EXTENSION_NAME "VK_EXT_texel_buffer_alignment"
#define VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME "VK_KHR_timeline_semaphore"
#define VK_EXT_TRANSFORM_FEEDBACK_EXTENSION_NAME "VK_EXT_transform_feedback"
#define VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_EXTENSION_NAME "VK_EXT_vertex_attribute_divisor"


template<> void initPropertyFromBlob<VkPhysicalDeviceSubgroupProperties>(VkPhysicalDeviceSubgroupProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.subgroupSize = allPropertiesBlobs.vk11.subgroupSize;
	propertyType.supportedStages = allPropertiesBlobs.vk11.subgroupSupportedStages;
	propertyType.supportedOperations = allPropertiesBlobs.vk11.subgroupSupportedOperations;
	propertyType.quadOperationsInAllStages = allPropertiesBlobs.vk11.subgroupQuadOperationsInAllStages;
}
template<> void initPropertyFromBlob<VkPhysicalDevicePointClippingProperties>(VkPhysicalDevicePointClippingProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.pointClippingBehavior = allPropertiesBlobs.vk11.pointClippingBehavior;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceMultiviewProperties>(VkPhysicalDeviceMultiviewProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.maxMultiviewViewCount = allPropertiesBlobs.vk11.maxMultiviewViewCount;
	propertyType.maxMultiviewInstanceIndex = allPropertiesBlobs.vk11.maxMultiviewInstanceIndex;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceProtectedMemoryProperties>(VkPhysicalDeviceProtectedMemoryProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.protectedNoFault = allPropertiesBlobs.vk11.protectedNoFault;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceIDProperties>(VkPhysicalDeviceIDProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	memcpy(propertyType.deviceUUID, allPropertiesBlobs.vk11.deviceUUID, sizeof(deUint8) * VK_UUID_SIZE);
	memcpy(propertyType.driverUUID, allPropertiesBlobs.vk11.driverUUID, sizeof(deUint8) * VK_UUID_SIZE);
	memcpy(propertyType.deviceLUID, allPropertiesBlobs.vk11.deviceLUID, sizeof(deUint8) * VK_LUID_SIZE);
	propertyType.deviceNodeMask = allPropertiesBlobs.vk11.deviceNodeMask;
	propertyType.deviceLUIDValid = allPropertiesBlobs.vk11.deviceLUIDValid;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceMaintenance3Properties>(VkPhysicalDeviceMaintenance3Properties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.maxPerSetDescriptors = allPropertiesBlobs.vk11.maxPerSetDescriptors;
	propertyType.maxMemoryAllocationSize = allPropertiesBlobs.vk11.maxMemoryAllocationSize;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceDriverProperties>(VkPhysicalDeviceDriverProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.driverID = allPropertiesBlobs.vk12.driverID;
	memcpy(propertyType.driverName, allPropertiesBlobs.vk12.driverName, sizeof(char) * VK_MAX_DRIVER_NAME_SIZE);
	memcpy(propertyType.driverInfo, allPropertiesBlobs.vk12.driverInfo, sizeof(char) * VK_MAX_DRIVER_INFO_SIZE);
	propertyType.conformanceVersion = allPropertiesBlobs.vk12.conformanceVersion;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceFloatControlsProperties>(VkPhysicalDeviceFloatControlsProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.denormBehaviorIndependence = allPropertiesBlobs.vk12.denormBehaviorIndependence;
	propertyType.roundingModeIndependence = allPropertiesBlobs.vk12.roundingModeIndependence;
	propertyType.shaderSignedZeroInfNanPreserveFloat16 = allPropertiesBlobs.vk12.shaderSignedZeroInfNanPreserveFloat16;
	propertyType.shaderSignedZeroInfNanPreserveFloat32 = allPropertiesBlobs.vk12.shaderSignedZeroInfNanPreserveFloat32;
	propertyType.shaderSignedZeroInfNanPreserveFloat64 = allPropertiesBlobs.vk12.shaderSignedZeroInfNanPreserveFloat64;
	propertyType.shaderDenormPreserveFloat16 = allPropertiesBlobs.vk12.shaderDenormPreserveFloat16;
	propertyType.shaderDenormPreserveFloat32 = allPropertiesBlobs.vk12.shaderDenormPreserveFloat32;
	propertyType.shaderDenormPreserveFloat64 = allPropertiesBlobs.vk12.shaderDenormPreserveFloat64;
	propertyType.shaderDenormFlushToZeroFloat16 = allPropertiesBlobs.vk12.shaderDenormFlushToZeroFloat16;
	propertyType.shaderDenormFlushToZeroFloat32 = allPropertiesBlobs.vk12.shaderDenormFlushToZeroFloat32;
	propertyType.shaderDenormFlushToZeroFloat64 = allPropertiesBlobs.vk12.shaderDenormFlushToZeroFloat64;
	propertyType.shaderRoundingModeRTEFloat16 = allPropertiesBlobs.vk12.shaderRoundingModeRTEFloat16;
	propertyType.shaderRoundingModeRTEFloat32 = allPropertiesBlobs.vk12.shaderRoundingModeRTEFloat32;
	propertyType.shaderRoundingModeRTEFloat64 = allPropertiesBlobs.vk12.shaderRoundingModeRTEFloat64;
	propertyType.shaderRoundingModeRTZFloat16 = allPropertiesBlobs.vk12.shaderRoundingModeRTZFloat16;
	propertyType.shaderRoundingModeRTZFloat32 = allPropertiesBlobs.vk12.shaderRoundingModeRTZFloat32;
	propertyType.shaderRoundingModeRTZFloat64 = allPropertiesBlobs.vk12.shaderRoundingModeRTZFloat64;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceDescriptorIndexingProperties>(VkPhysicalDeviceDescriptorIndexingProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.maxUpdateAfterBindDescriptorsInAllPools = allPropertiesBlobs.vk12.maxUpdateAfterBindDescriptorsInAllPools;
	propertyType.shaderUniformBufferArrayNonUniformIndexingNative = allPropertiesBlobs.vk12.shaderUniformBufferArrayNonUniformIndexingNative;
	propertyType.shaderSampledImageArrayNonUniformIndexingNative = allPropertiesBlobs.vk12.shaderSampledImageArrayNonUniformIndexingNative;
	propertyType.shaderStorageBufferArrayNonUniformIndexingNative = allPropertiesBlobs.vk12.shaderStorageBufferArrayNonUniformIndexingNative;
	propertyType.shaderStorageImageArrayNonUniformIndexingNative = allPropertiesBlobs.vk12.shaderStorageImageArrayNonUniformIndexingNative;
	propertyType.shaderInputAttachmentArrayNonUniformIndexingNative = allPropertiesBlobs.vk12.shaderInputAttachmentArrayNonUniformIndexingNative;
	propertyType.robustBufferAccessUpdateAfterBind = allPropertiesBlobs.vk12.robustBufferAccessUpdateAfterBind;
	propertyType.quadDivergentImplicitLod = allPropertiesBlobs.vk12.quadDivergentImplicitLod;
	propertyType.maxPerStageDescriptorUpdateAfterBindSamplers = allPropertiesBlobs.vk12.maxPerStageDescriptorUpdateAfterBindSamplers;
	propertyType.maxPerStageDescriptorUpdateAfterBindUniformBuffers = allPropertiesBlobs.vk12.maxPerStageDescriptorUpdateAfterBindUniformBuffers;
	propertyType.maxPerStageDescriptorUpdateAfterBindStorageBuffers = allPropertiesBlobs.vk12.maxPerStageDescriptorUpdateAfterBindStorageBuffers;
	propertyType.maxPerStageDescriptorUpdateAfterBindSampledImages = allPropertiesBlobs.vk12.maxPerStageDescriptorUpdateAfterBindSampledImages;
	propertyType.maxPerStageDescriptorUpdateAfterBindStorageImages = allPropertiesBlobs.vk12.maxPerStageDescriptorUpdateAfterBindStorageImages;
	propertyType.maxPerStageDescriptorUpdateAfterBindInputAttachments = allPropertiesBlobs.vk12.maxPerStageDescriptorUpdateAfterBindInputAttachments;
	propertyType.maxPerStageUpdateAfterBindResources = allPropertiesBlobs.vk12.maxPerStageUpdateAfterBindResources;
	propertyType.maxDescriptorSetUpdateAfterBindSamplers = allPropertiesBlobs.vk12.maxDescriptorSetUpdateAfterBindSamplers;
	propertyType.maxDescriptorSetUpdateAfterBindUniformBuffers = allPropertiesBlobs.vk12.maxDescriptorSetUpdateAfterBindUniformBuffers;
	propertyType.maxDescriptorSetUpdateAfterBindUniformBuffersDynamic = allPropertiesBlobs.vk12.maxDescriptorSetUpdateAfterBindUniformBuffersDynamic;
	propertyType.maxDescriptorSetUpdateAfterBindStorageBuffers = allPropertiesBlobs.vk12.maxDescriptorSetUpdateAfterBindStorageBuffers;
	propertyType.maxDescriptorSetUpdateAfterBindStorageBuffersDynamic = allPropertiesBlobs.vk12.maxDescriptorSetUpdateAfterBindStorageBuffersDynamic;
	propertyType.maxDescriptorSetUpdateAfterBindSampledImages = allPropertiesBlobs.vk12.maxDescriptorSetUpdateAfterBindSampledImages;
	propertyType.maxDescriptorSetUpdateAfterBindStorageImages = allPropertiesBlobs.vk12.maxDescriptorSetUpdateAfterBindStorageImages;
	propertyType.maxDescriptorSetUpdateAfterBindInputAttachments = allPropertiesBlobs.vk12.maxDescriptorSetUpdateAfterBindInputAttachments;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceDepthStencilResolveProperties>(VkPhysicalDeviceDepthStencilResolveProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.supportedDepthResolveModes = allPropertiesBlobs.vk12.supportedDepthResolveModes;
	propertyType.supportedStencilResolveModes = allPropertiesBlobs.vk12.supportedStencilResolveModes;
	propertyType.independentResolveNone = allPropertiesBlobs.vk12.independentResolveNone;
	propertyType.independentResolve = allPropertiesBlobs.vk12.independentResolve;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceSamplerFilterMinmaxProperties>(VkPhysicalDeviceSamplerFilterMinmaxProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.filterMinmaxSingleComponentFormats = allPropertiesBlobs.vk12.filterMinmaxSingleComponentFormats;
	propertyType.filterMinmaxImageComponentMapping = allPropertiesBlobs.vk12.filterMinmaxImageComponentMapping;
}
template<> void initPropertyFromBlob<VkPhysicalDeviceTimelineSemaphoreProperties>(VkPhysicalDeviceTimelineSemaphoreProperties& propertyType, const AllPropertiesBlobs& allPropertiesBlobs)
{
	propertyType.maxTimelineSemaphoreValueDifference = allPropertiesBlobs.vk12.maxTimelineSemaphoreValueDifference;
}

// generic template is not enough for some compilers
template<> void initPropertyFromBlob<VkPhysicalDeviceMemoryProperties>(VkPhysicalDeviceMemoryProperties&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceSparseProperties>(VkPhysicalDeviceSparseProperties&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceGroupProperties>(VkPhysicalDeviceGroupProperties&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceMemoryProperties2>(VkPhysicalDeviceMemoryProperties2&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDevicePushDescriptorPropertiesKHR>(VkPhysicalDevicePushDescriptorPropertiesKHR&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDevicePerformanceQueryPropertiesKHR>(VkPhysicalDevicePerformanceQueryPropertiesKHR&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceFragmentShadingRatePropertiesKHR>(VkPhysicalDeviceFragmentShadingRatePropertiesKHR&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDevicePipelineExecutablePropertiesFeaturesKHR>(VkPhysicalDevicePipelineExecutablePropertiesFeaturesKHR&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceShaderIntegerDotProductPropertiesKHR>(VkPhysicalDeviceShaderIntegerDotProductPropertiesKHR&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceTransformFeedbackPropertiesEXT>(VkPhysicalDeviceTransformFeedbackPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceMultiviewPerViewAttributesPropertiesNVX>(VkPhysicalDeviceMultiviewPerViewAttributesPropertiesNVX&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceDiscardRectanglePropertiesEXT>(VkPhysicalDeviceDiscardRectanglePropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceConservativeRasterizationPropertiesEXT>(VkPhysicalDeviceConservativeRasterizationPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceInlineUniformBlockPropertiesEXT>(VkPhysicalDeviceInlineUniformBlockPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceSampleLocationsPropertiesEXT>(VkPhysicalDeviceSampleLocationsPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceBlendOperationAdvancedPropertiesEXT>(VkPhysicalDeviceBlendOperationAdvancedPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceShaderSMBuiltinsPropertiesNV>(VkPhysicalDeviceShaderSMBuiltinsPropertiesNV&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceShadingRateImagePropertiesNV>(VkPhysicalDeviceShadingRateImagePropertiesNV&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceRayTracingPropertiesNV>(VkPhysicalDeviceRayTracingPropertiesNV&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceExternalMemoryHostPropertiesEXT>(VkPhysicalDeviceExternalMemoryHostPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceShaderCorePropertiesAMD>(VkPhysicalDeviceShaderCorePropertiesAMD&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceVertexAttributeDivisorPropertiesEXT>(VkPhysicalDeviceVertexAttributeDivisorPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceMeshShaderPropertiesNV>(VkPhysicalDeviceMeshShaderPropertiesNV&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDevicePCIBusInfoPropertiesEXT>(VkPhysicalDevicePCIBusInfoPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceFragmentDensityMapPropertiesEXT>(VkPhysicalDeviceFragmentDensityMapPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceSubgroupSizeControlPropertiesEXT>(VkPhysicalDeviceSubgroupSizeControlPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceShaderCoreProperties2AMD>(VkPhysicalDeviceShaderCoreProperties2AMD&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceMemoryBudgetPropertiesEXT>(VkPhysicalDeviceMemoryBudgetPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceToolPropertiesEXT>(VkPhysicalDeviceToolPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceCooperativeMatrixPropertiesNV>(VkPhysicalDeviceCooperativeMatrixPropertiesNV&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceLineRasterizationPropertiesEXT>(VkPhysicalDeviceLineRasterizationPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceDeviceGeneratedCommandsPropertiesNV>(VkPhysicalDeviceDeviceGeneratedCommandsPropertiesNV&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceTexelBufferAlignmentPropertiesEXT>(VkPhysicalDeviceTexelBufferAlignmentPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceRobustness2PropertiesEXT>(VkPhysicalDeviceRobustness2PropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceCustomBorderColorPropertiesEXT>(VkPhysicalDeviceCustomBorderColorPropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceFragmentShadingRateEnumsPropertiesNV>(VkPhysicalDeviceFragmentShadingRateEnumsPropertiesNV&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceFragmentDensityMap2PropertiesEXT>(VkPhysicalDeviceFragmentDensityMap2PropertiesEXT&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceAccelerationStructurePropertiesKHR>(VkPhysicalDeviceAccelerationStructurePropertiesKHR&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDeviceRayTracingPipelinePropertiesKHR>(VkPhysicalDeviceRayTracingPipelinePropertiesKHR&, const AllPropertiesBlobs&) {}
template<> void initPropertyFromBlob<VkPhysicalDevicePortabilitySubsetPropertiesKHR>(VkPhysicalDevicePortabilitySubsetPropertiesKHR&, const AllPropertiesBlobs&) {}


template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceAccelerationStructurePropertiesKHR>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_PROPERTIES_KHR, VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME, VK_KHR_ACCELERATION_STRUCTURE_SPEC_VERSION, 43}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceBlendOperationAdvancedPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_BLEND_OPERATION_ADVANCED_PROPERTIES_EXT, VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME, VK_EXT_BLEND_OPERATION_ADVANCED_SPEC_VERSION, 42}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceConservativeRasterizationPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_CONSERVATIVE_RASTERIZATION_PROPERTIES_EXT, VK_EXT_CONSERVATIVE_RASTERIZATION_EXTENSION_NAME, VK_EXT_CONSERVATIVE_RASTERIZATION_SPEC_VERSION, 41}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceCooperativeMatrixPropertiesNV>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_COOPERATIVE_MATRIX_PROPERTIES_NV, VK_NV_COOPERATIVE_MATRIX_EXTENSION_NAME, VK_NV_COOPERATIVE_MATRIX_SPEC_VERSION, 40}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceCustomBorderColorPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_CUSTOM_BORDER_COLOR_PROPERTIES_EXT, VK_EXT_CUSTOM_BORDER_COLOR_EXTENSION_NAME, VK_EXT_CUSTOM_BORDER_COLOR_SPEC_VERSION, 39}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceDepthStencilResolveProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DEPTH_STENCIL_RESOLVE_PROPERTIES, VK_KHR_DEPTH_STENCIL_RESOLVE_EXTENSION_NAME, VK_KHR_DEPTH_STENCIL_RESOLVE_SPEC_VERSION, 38}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceDescriptorIndexingProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_PROPERTIES, VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME, VK_EXT_DESCRIPTOR_INDEXING_SPEC_VERSION, 37}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceDeviceGeneratedCommandsPropertiesNV>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DEVICE_GENERATED_COMMANDS_PROPERTIES_NV, VK_NV_DEVICE_GENERATED_COMMANDS_EXTENSION_NAME, VK_NV_DEVICE_GENERATED_COMMANDS_SPEC_VERSION, 36}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceDiscardRectanglePropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DISCARD_RECTANGLE_PROPERTIES_EXT, VK_EXT_DISCARD_RECTANGLES_EXTENSION_NAME, VK_EXT_DISCARD_RECTANGLES_SPEC_VERSION, 35}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceDriverProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES, VK_KHR_DRIVER_PROPERTIES_EXTENSION_NAME, VK_KHR_DRIVER_PROPERTIES_SPEC_VERSION, 34}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceExternalMemoryHostPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_MEMORY_HOST_PROPERTIES_EXT, VK_EXT_EXTERNAL_MEMORY_HOST_EXTENSION_NAME, VK_EXT_EXTERNAL_MEMORY_HOST_SPEC_VERSION, 33}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceFloatControlsProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FLOAT_CONTROLS_PROPERTIES, VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME, VK_KHR_SHADER_FLOAT_CONTROLS_SPEC_VERSION, 32}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceFragmentDensityMapPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_DENSITY_MAP_PROPERTIES_EXT, VK_EXT_FRAGMENT_DENSITY_MAP_EXTENSION_NAME, VK_EXT_FRAGMENT_DENSITY_MAP_SPEC_VERSION, 31}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceFragmentDensityMap2PropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_DENSITY_MAP_2_PROPERTIES_EXT, VK_EXT_FRAGMENT_DENSITY_MAP_2_EXTENSION_NAME, VK_EXT_FRAGMENT_DENSITY_MAP_2_SPEC_VERSION, 30}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceFragmentShadingRatePropertiesKHR>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADING_RATE_PROPERTIES_KHR, VK_KHR_FRAGMENT_SHADING_RATE_EXTENSION_NAME, VK_KHR_FRAGMENT_SHADING_RATE_SPEC_VERSION, 29}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceFragmentShadingRateEnumsPropertiesNV>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADING_RATE_ENUMS_PROPERTIES_NV, VK_NV_FRAGMENT_SHADING_RATE_ENUMS_EXTENSION_NAME, VK_NV_FRAGMENT_SHADING_RATE_ENUMS_SPEC_VERSION, 28}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceIDProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES, DECL_ID_EXTENSION_NAME, 0, 27}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceInlineUniformBlockPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INLINE_UNIFORM_BLOCK_PROPERTIES_EXT, VK_EXT_INLINE_UNIFORM_BLOCK_EXTENSION_NAME, VK_EXT_INLINE_UNIFORM_BLOCK_SPEC_VERSION, 26}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceLineRasterizationPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_LINE_RASTERIZATION_PROPERTIES_EXT, VK_EXT_LINE_RASTERIZATION_EXTENSION_NAME, VK_EXT_LINE_RASTERIZATION_SPEC_VERSION, 25}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceMaintenance3Properties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_3_PROPERTIES, VK_KHR_MAINTENANCE3_EXTENSION_NAME, VK_KHR_MAINTENANCE3_SPEC_VERSION, 24}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceMeshShaderPropertiesNV>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_PROPERTIES_NV, VK_NV_MESH_SHADER_EXTENSION_NAME, VK_NV_MESH_SHADER_SPEC_VERSION, 23}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceMultiviewProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTIVIEW_PROPERTIES, VK_KHR_MULTIVIEW_EXTENSION_NAME, VK_KHR_MULTIVIEW_SPEC_VERSION, 22}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceMultiviewPerViewAttributesPropertiesNVX>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTIVIEW_PER_VIEW_ATTRIBUTES_PROPERTIES_NVX, VK_NVX_MULTIVIEW_PER_VIEW_ATTRIBUTES_EXTENSION_NAME, VK_NVX_MULTIVIEW_PER_VIEW_ATTRIBUTES_SPEC_VERSION, 21}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDevicePerformanceQueryPropertiesKHR>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PERFORMANCE_QUERY_PROPERTIES_KHR, VK_KHR_PERFORMANCE_QUERY_EXTENSION_NAME, VK_KHR_PERFORMANCE_QUERY_SPEC_VERSION, 20}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDevicePointClippingProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_POINT_CLIPPING_PROPERTIES, VK_KHR_MAINTENANCE2_EXTENSION_NAME, VK_KHR_MAINTENANCE2_SPEC_VERSION, 19}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDevicePortabilitySubsetPropertiesKHR>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PORTABILITY_SUBSET_PROPERTIES_KHR, VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME, VK_KHR_PORTABILITY_SUBSET_SPEC_VERSION, 18}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceProtectedMemoryProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROTECTED_MEMORY_PROPERTIES, DECL_PROTECTED_MEMORY_EXTENSION_NAME, 0, 17}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDevicePushDescriptorPropertiesKHR>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PUSH_DESCRIPTOR_PROPERTIES_KHR, VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME, VK_KHR_PUSH_DESCRIPTOR_SPEC_VERSION, 16}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceRayTracingPropertiesNV>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PROPERTIES_NV, VK_NV_RAY_TRACING_EXTENSION_NAME, VK_NV_RAY_TRACING_SPEC_VERSION, 15}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceRayTracingPipelinePropertiesKHR>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_PROPERTIES_KHR, VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME, VK_KHR_RAY_TRACING_PIPELINE_SPEC_VERSION, 14}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceRobustness2PropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ROBUSTNESS_2_PROPERTIES_EXT, VK_EXT_ROBUSTNESS_2_EXTENSION_NAME, VK_EXT_ROBUSTNESS_2_SPEC_VERSION, 13}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceSamplerFilterMinmaxProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SAMPLER_FILTER_MINMAX_PROPERTIES, VK_EXT_SAMPLER_FILTER_MINMAX_EXTENSION_NAME, VK_EXT_SAMPLER_FILTER_MINMAX_SPEC_VERSION, 12}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceSampleLocationsPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SAMPLE_LOCATIONS_PROPERTIES_EXT, VK_EXT_SAMPLE_LOCATIONS_EXTENSION_NAME, VK_EXT_SAMPLE_LOCATIONS_SPEC_VERSION, 11}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceShaderCorePropertiesAMD>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_CORE_PROPERTIES_AMD, VK_AMD_SHADER_CORE_PROPERTIES_EXTENSION_NAME, VK_AMD_SHADER_CORE_PROPERTIES_SPEC_VERSION, 10}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceShaderCoreProperties2AMD>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_CORE_PROPERTIES_2_AMD, VK_AMD_SHADER_CORE_PROPERTIES_2_EXTENSION_NAME, VK_AMD_SHADER_CORE_PROPERTIES_2_SPEC_VERSION, 9}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceShaderIntegerDotProductPropertiesKHR>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_INTEGER_DOT_PRODUCT_PROPERTIES_KHR, VK_KHR_SHADER_INTEGER_DOT_PRODUCT_EXTENSION_NAME, VK_KHR_SHADER_INTEGER_DOT_PRODUCT_SPEC_VERSION, 8}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceShadingRateImagePropertiesNV>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADING_RATE_IMAGE_PROPERTIES_NV, VK_NV_SHADING_RATE_IMAGE_EXTENSION_NAME, VK_NV_SHADING_RATE_IMAGE_SPEC_VERSION, 7}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceSubgroupProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_PROPERTIES, DECL_SUBGROUP_EXTENSION_NAME, 0, 6}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceSubgroupSizeControlPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_SIZE_CONTROL_PROPERTIES_EXT, VK_EXT_SUBGROUP_SIZE_CONTROL_EXTENSION_NAME, VK_EXT_SUBGROUP_SIZE_CONTROL_SPEC_VERSION, 5}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceTexelBufferAlignmentPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TEXEL_BUFFER_ALIGNMENT_PROPERTIES_EXT, VK_EXT_TEXEL_BUFFER_ALIGNMENT_EXTENSION_NAME, VK_EXT_TEXEL_BUFFER_ALIGNMENT_SPEC_VERSION, 4}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceTimelineSemaphoreProperties>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TIMELINE_SEMAPHORE_PROPERTIES, VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME, VK_KHR_TIMELINE_SEMAPHORE_SPEC_VERSION, 3}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceTransformFeedbackPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TRANSFORM_FEEDBACK_PROPERTIES_EXT, VK_EXT_TRANSFORM_FEEDBACK_EXTENSION_NAME, VK_EXT_TRANSFORM_FEEDBACK_SPEC_VERSION, 2}; }
template<> PropertyDesc makePropertyDesc<VkPhysicalDeviceVertexAttributeDivisorPropertiesEXT>(void) { return PropertyDesc{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VERTEX_ATTRIBUTE_DIVISOR_PROPERTIES_EXT, VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_EXTENSION_NAME, VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_SPEC_VERSION, 1}; }


static const PropertyStructCreationData propertyStructCreationArray[] =
{
	{ createPropertyStructWrapper<VkPhysicalDeviceAccelerationStructurePropertiesKHR>, VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME, VK_KHR_ACCELERATION_STRUCTURE_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceBlendOperationAdvancedPropertiesEXT>, VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME, VK_EXT_BLEND_OPERATION_ADVANCED_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceConservativeRasterizationPropertiesEXT>, VK_EXT_CONSERVATIVE_RASTERIZATION_EXTENSION_NAME, VK_EXT_CONSERVATIVE_RASTERIZATION_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceCooperativeMatrixPropertiesNV>, VK_NV_COOPERATIVE_MATRIX_EXTENSION_NAME, VK_NV_COOPERATIVE_MATRIX_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceCustomBorderColorPropertiesEXT>, VK_EXT_CUSTOM_BORDER_COLOR_EXTENSION_NAME, VK_EXT_CUSTOM_BORDER_COLOR_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceDepthStencilResolveProperties>, VK_KHR_DEPTH_STENCIL_RESOLVE_EXTENSION_NAME, VK_KHR_DEPTH_STENCIL_RESOLVE_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceDescriptorIndexingProperties>, VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME, VK_EXT_DESCRIPTOR_INDEXING_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceDeviceGeneratedCommandsPropertiesNV>, VK_NV_DEVICE_GENERATED_COMMANDS_EXTENSION_NAME, VK_NV_DEVICE_GENERATED_COMMANDS_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceDiscardRectanglePropertiesEXT>, VK_EXT_DISCARD_RECTANGLES_EXTENSION_NAME, VK_EXT_DISCARD_RECTANGLES_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceDriverProperties>, VK_KHR_DRIVER_PROPERTIES_EXTENSION_NAME, VK_KHR_DRIVER_PROPERTIES_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceExternalMemoryHostPropertiesEXT>, VK_EXT_EXTERNAL_MEMORY_HOST_EXTENSION_NAME, VK_EXT_EXTERNAL_MEMORY_HOST_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceFloatControlsProperties>, VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME, VK_KHR_SHADER_FLOAT_CONTROLS_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceFragmentDensityMapPropertiesEXT>, VK_EXT_FRAGMENT_DENSITY_MAP_EXTENSION_NAME, VK_EXT_FRAGMENT_DENSITY_MAP_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceFragmentDensityMap2PropertiesEXT>, VK_EXT_FRAGMENT_DENSITY_MAP_2_EXTENSION_NAME, VK_EXT_FRAGMENT_DENSITY_MAP_2_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceFragmentShadingRatePropertiesKHR>, VK_KHR_FRAGMENT_SHADING_RATE_EXTENSION_NAME, VK_KHR_FRAGMENT_SHADING_RATE_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceFragmentShadingRateEnumsPropertiesNV>, VK_NV_FRAGMENT_SHADING_RATE_ENUMS_EXTENSION_NAME, VK_NV_FRAGMENT_SHADING_RATE_ENUMS_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceIDProperties>, DECL_ID_EXTENSION_NAME, 0 },
	{ createPropertyStructWrapper<VkPhysicalDeviceInlineUniformBlockPropertiesEXT>, VK_EXT_INLINE_UNIFORM_BLOCK_EXTENSION_NAME, VK_EXT_INLINE_UNIFORM_BLOCK_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceLineRasterizationPropertiesEXT>, VK_EXT_LINE_RASTERIZATION_EXTENSION_NAME, VK_EXT_LINE_RASTERIZATION_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceMaintenance3Properties>, VK_KHR_MAINTENANCE3_EXTENSION_NAME, VK_KHR_MAINTENANCE3_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceMeshShaderPropertiesNV>, VK_NV_MESH_SHADER_EXTENSION_NAME, VK_NV_MESH_SHADER_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceMultiviewProperties>, VK_KHR_MULTIVIEW_EXTENSION_NAME, VK_KHR_MULTIVIEW_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceMultiviewPerViewAttributesPropertiesNVX>, VK_NVX_MULTIVIEW_PER_VIEW_ATTRIBUTES_EXTENSION_NAME, VK_NVX_MULTIVIEW_PER_VIEW_ATTRIBUTES_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDevicePerformanceQueryPropertiesKHR>, VK_KHR_PERFORMANCE_QUERY_EXTENSION_NAME, VK_KHR_PERFORMANCE_QUERY_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDevicePointClippingProperties>, VK_KHR_MAINTENANCE2_EXTENSION_NAME, VK_KHR_MAINTENANCE2_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDevicePortabilitySubsetPropertiesKHR>, VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME, VK_KHR_PORTABILITY_SUBSET_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceProtectedMemoryProperties>, DECL_PROTECTED_MEMORY_EXTENSION_NAME, 0 },
	{ createPropertyStructWrapper<VkPhysicalDevicePushDescriptorPropertiesKHR>, VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME, VK_KHR_PUSH_DESCRIPTOR_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceRayTracingPropertiesNV>, VK_NV_RAY_TRACING_EXTENSION_NAME, VK_NV_RAY_TRACING_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceRayTracingPipelinePropertiesKHR>, VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME, VK_KHR_RAY_TRACING_PIPELINE_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceRobustness2PropertiesEXT>, VK_EXT_ROBUSTNESS_2_EXTENSION_NAME, VK_EXT_ROBUSTNESS_2_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceSamplerFilterMinmaxProperties>, VK_EXT_SAMPLER_FILTER_MINMAX_EXTENSION_NAME, VK_EXT_SAMPLER_FILTER_MINMAX_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceSampleLocationsPropertiesEXT>, VK_EXT_SAMPLE_LOCATIONS_EXTENSION_NAME, VK_EXT_SAMPLE_LOCATIONS_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceShaderCorePropertiesAMD>, VK_AMD_SHADER_CORE_PROPERTIES_EXTENSION_NAME, VK_AMD_SHADER_CORE_PROPERTIES_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceShaderCoreProperties2AMD>, VK_AMD_SHADER_CORE_PROPERTIES_2_EXTENSION_NAME, VK_AMD_SHADER_CORE_PROPERTIES_2_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceShaderIntegerDotProductPropertiesKHR>, VK_KHR_SHADER_INTEGER_DOT_PRODUCT_EXTENSION_NAME, VK_KHR_SHADER_INTEGER_DOT_PRODUCT_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceShadingRateImagePropertiesNV>, VK_NV_SHADING_RATE_IMAGE_EXTENSION_NAME, VK_NV_SHADING_RATE_IMAGE_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceSubgroupProperties>, DECL_SUBGROUP_EXTENSION_NAME, 0 },
	{ createPropertyStructWrapper<VkPhysicalDeviceSubgroupSizeControlPropertiesEXT>, VK_EXT_SUBGROUP_SIZE_CONTROL_EXTENSION_NAME, VK_EXT_SUBGROUP_SIZE_CONTROL_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceTexelBufferAlignmentPropertiesEXT>, VK_EXT_TEXEL_BUFFER_ALIGNMENT_EXTENSION_NAME, VK_EXT_TEXEL_BUFFER_ALIGNMENT_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceTimelineSemaphoreProperties>, VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME, VK_KHR_TIMELINE_SEMAPHORE_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceTransformFeedbackPropertiesEXT>, VK_EXT_TRANSFORM_FEEDBACK_EXTENSION_NAME, VK_EXT_TRANSFORM_FEEDBACK_SPEC_VERSION },
	{ createPropertyStructWrapper<VkPhysicalDeviceVertexAttributeDivisorPropertiesEXT>, VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_EXTENSION_NAME, VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_SPEC_VERSION },
};

bool isPartOfBlobProperties (VkStructureType sType)
{
	const std::vector<VkStructureType> sTypeVect =	{
		// Vulkan11
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_3_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTIVIEW_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_POINT_CLIPPING_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROTECTED_MEMORY_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_PROPERTIES,
		// Vulkan12
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DEPTH_STENCIL_RESOLVE_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FLOAT_CONTROLS_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SAMPLER_FILTER_MINMAX_PROPERTIES,
		VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TIMELINE_SEMAPHORE_PROPERTIES,
	};
	return de::contains(sTypeVect.begin(), sTypeVect.end(), sType);
}

} // vk

