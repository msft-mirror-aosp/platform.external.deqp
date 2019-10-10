/* WARNING: This is auto-generated file. Do not modify, since changes will
 * be lost! Modify the generating script instead.
 */
#include "vkDeviceFeatures.hpp"

namespace vk
{
#define DECL_PROTECTED_MEMORY_EXTENSION_NAME "not_existent_feature"
#define VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME "VK_KHR_shader_draw_parameters"
#define VK_EXT_TRANSFORM_FEEDBACK_EXTENSION_NAME "VK_EXT_transform_feedback"
#define VK_NV_CORNER_SAMPLED_IMAGE_EXTENSION_NAME "VK_NV_corner_sampled_image"
#define VK_EXT_CONDITIONAL_RENDERING_EXTENSION_NAME "VK_EXT_conditional_rendering"
#define VK_KHR_SHADER_FLOAT16_INT8_EXTENSION_NAME "VK_KHR_shader_float16_int8"
#define VK_EXT_DEPTH_CLIP_ENABLE_EXTENSION_NAME "VK_EXT_depth_clip_enable"
#define VK_KHR_IMAGELESS_FRAMEBUFFER_EXTENSION_NAME "VK_KHR_imageless_framebuffer"
#define VK_EXT_INLINE_UNIFORM_BLOCK_EXTENSION_NAME "VK_EXT_inline_uniform_block"
#define VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME "VK_EXT_blend_operation_advanced"
#define VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME "VK_EXT_descriptor_indexing"
#define VK_NV_SHADING_RATE_IMAGE_EXTENSION_NAME "VK_NV_shading_rate_image"
#define VK_NV_REPRESENTATIVE_FRAGMENT_TEST_EXTENSION_NAME "VK_NV_representative_fragment_test"
#define VK_KHR_8BIT_STORAGE_EXTENSION_NAME "VK_KHR_8bit_storage"
#define VK_KHR_SHADER_ATOMIC_INT64_EXTENSION_NAME "VK_KHR_shader_atomic_int64"
#define VK_KHR_SHADER_CLOCK_EXTENSION_NAME    "VK_KHR_shader_clock"
#define VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_EXTENSION_NAME "VK_EXT_vertex_attribute_divisor"
#define VK_NV_COMPUTE_SHADER_DERIVATIVES_EXTENSION_NAME "VK_NV_compute_shader_derivatives"
#define VK_NV_MESH_SHADER_EXTENSION_NAME  "VK_NV_mesh_shader"
#define VK_NV_FRAGMENT_SHADER_BARYCENTRIC_EXTENSION_NAME "VK_NV_fragment_shader_barycentric"
#define VK_NV_SHADER_IMAGE_FOOTPRINT_EXTENSION_NAME "VK_NV_shader_image_footprint"
#define VK_NV_SCISSOR_EXCLUSIVE_EXTENSION_NAME "VK_NV_scissor_exclusive"
#define VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME "VK_KHR_timeline_semaphore"
#define VK_KHR_VULKAN_MEMORY_MODEL_EXTENSION_NAME "VK_KHR_vulkan_memory_model"
#define VK_EXT_FRAGMENT_DENSITY_MAP_EXTENSION_NAME "VK_EXT_fragment_density_map"
#define VK_EXT_SCALAR_BLOCK_LAYOUT_EXTENSION_NAME "VK_EXT_scalar_block_layout"
#define VK_EXT_MEMORY_PRIORITY_EXTENSION_NAME "VK_EXT_memory_priority"
#define VK_NV_DEDICATED_ALLOCATION_IMAGE_ALIASING_EXTENSION_NAME "VK_NV_dedicated_allocation_image_aliasing"
#define VK_EXT_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME "VK_EXT_buffer_device_address"
#define VK_NV_COOPERATIVE_MATRIX_EXTENSION_NAME "VK_NV_cooperative_matrix"
#define VK_NV_COVERAGE_REDUCTION_MODE_EXTENSION_NAME "VK_NV_coverage_reduction_mode"
#define VK_EXT_FRAGMENT_SHADER_INTERLOCK_EXTENSION_NAME "VK_EXT_fragment_shader_interlock"
#define VK_EXT_YCBCR_IMAGE_ARRAYS_EXTENSION_NAME "VK_EXT_ycbcr_image_arrays"
#define VK_KHR_UNIFORM_BUFFER_STANDARD_LAYOUT_EXTENSION_NAME "VK_KHR_uniform_buffer_standard_layout"
#define VK_EXT_HOST_QUERY_RESET_EXTENSION_NAME "VK_EXT_host_query_reset"
#define VK_EXT_INDEX_TYPE_UINT8_EXTENSION_NAME "VK_EXT_index_type_uint8"
#define VK_EXT_SHADER_DEMOTE_TO_HELPER_INVOCATION_EXTENSION_NAME "VK_EXT_shader_demote_to_helper_invocation"
#define VK_KHR_PIPELINE_EXECUTABLE_PROPERTIES_EXTENSION_NAME "VK_KHR_pipeline_executable_properties"
#define VK_KHR_MULTIVIEW_EXTENSION_NAME   "VK_KHR_multiview"
#define VK_KHR_16BIT_STORAGE_EXTENSION_NAME "VK_KHR_16bit_storage"
#define VK_KHR_VARIABLE_POINTERS_EXTENSION_NAME "VK_KHR_variable_pointers"
#define VK_KHR_SAMPLER_YCBCR_CONVERSION_EXTENSION_NAME "VK_KHR_sampler_ycbcr_conversion"


template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceProtectedMemoryFeatures>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROTECTED_MEMORY_FEATURES, DECL_PROTECTED_MEMORY_EXTENSION_NAME, 0, 42); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceShaderDrawParametersFeatures>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_DRAW_PARAMETERS_FEATURES, VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME, VK_KHR_SHADER_DRAW_PARAMETERS_SPEC_VERSION, 41); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceTransformFeedbackFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TRANSFORM_FEEDBACK_FEATURES_EXT, VK_EXT_TRANSFORM_FEEDBACK_EXTENSION_NAME, VK_EXT_TRANSFORM_FEEDBACK_SPEC_VERSION, 40); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceCornerSampledImageFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_CORNER_SAMPLED_IMAGE_FEATURES_NV, VK_NV_CORNER_SAMPLED_IMAGE_EXTENSION_NAME, VK_NV_CORNER_SAMPLED_IMAGE_SPEC_VERSION, 39); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceConditionalRenderingFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_CONDITIONAL_RENDERING_FEATURES_EXT, VK_EXT_CONDITIONAL_RENDERING_EXTENSION_NAME, VK_EXT_CONDITIONAL_RENDERING_SPEC_VERSION, 38); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceFloat16Int8FeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FLOAT16_INT8_FEATURES_KHR, VK_KHR_SHADER_FLOAT16_INT8_EXTENSION_NAME, VK_KHR_SHADER_FLOAT16_INT8_SPEC_VERSION, 37); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceDepthClipEnableFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DEPTH_CLIP_ENABLE_FEATURES_EXT, VK_EXT_DEPTH_CLIP_ENABLE_EXTENSION_NAME, VK_EXT_DEPTH_CLIP_ENABLE_SPEC_VERSION, 36); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceImagelessFramebufferFeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGELESS_FRAMEBUFFER_FEATURES_KHR, VK_KHR_IMAGELESS_FRAMEBUFFER_EXTENSION_NAME, VK_KHR_IMAGELESS_FRAMEBUFFER_SPEC_VERSION, 35); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceInlineUniformBlockFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INLINE_UNIFORM_BLOCK_FEATURES_EXT, VK_EXT_INLINE_UNIFORM_BLOCK_EXTENSION_NAME, VK_EXT_INLINE_UNIFORM_BLOCK_SPEC_VERSION, 34); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceBlendOperationAdvancedFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_BLEND_OPERATION_ADVANCED_FEATURES_EXT, VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME, VK_EXT_BLEND_OPERATION_ADVANCED_SPEC_VERSION, 33); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceDescriptorIndexingFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES_EXT, VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME, VK_EXT_DESCRIPTOR_INDEXING_SPEC_VERSION, 32); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceShadingRateImageFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADING_RATE_IMAGE_FEATURES_NV, VK_NV_SHADING_RATE_IMAGE_EXTENSION_NAME, VK_NV_SHADING_RATE_IMAGE_SPEC_VERSION, 31); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceRepresentativeFragmentTestFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_REPRESENTATIVE_FRAGMENT_TEST_FEATURES_NV, VK_NV_REPRESENTATIVE_FRAGMENT_TEST_EXTENSION_NAME, VK_NV_REPRESENTATIVE_FRAGMENT_TEST_SPEC_VERSION, 30); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDevice8BitStorageFeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_8BIT_STORAGE_FEATURES_KHR, VK_KHR_8BIT_STORAGE_EXTENSION_NAME, VK_KHR_8BIT_STORAGE_SPEC_VERSION, 29); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceShaderAtomicInt64FeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_INT64_FEATURES_KHR, VK_KHR_SHADER_ATOMIC_INT64_EXTENSION_NAME, VK_KHR_SHADER_ATOMIC_INT64_SPEC_VERSION, 28); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceShaderClockFeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_CLOCK_FEATURES_KHR, VK_KHR_SHADER_CLOCK_EXTENSION_NAME, VK_KHR_SHADER_CLOCK_SPEC_VERSION, 27); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceVertexAttributeDivisorFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VERTEX_ATTRIBUTE_DIVISOR_FEATURES_EXT, VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_EXTENSION_NAME, VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_SPEC_VERSION, 26); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceComputeShaderDerivativesFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_COMPUTE_SHADER_DERIVATIVES_FEATURES_NV, VK_NV_COMPUTE_SHADER_DERIVATIVES_EXTENSION_NAME, VK_NV_COMPUTE_SHADER_DERIVATIVES_SPEC_VERSION, 25); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceMeshShaderFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_NV, VK_NV_MESH_SHADER_EXTENSION_NAME, VK_NV_MESH_SHADER_SPEC_VERSION, 24); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceFragmentShaderBarycentricFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADER_BARYCENTRIC_FEATURES_NV, VK_NV_FRAGMENT_SHADER_BARYCENTRIC_EXTENSION_NAME, VK_NV_FRAGMENT_SHADER_BARYCENTRIC_SPEC_VERSION, 23); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceShaderImageFootprintFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_IMAGE_FOOTPRINT_FEATURES_NV, VK_NV_SHADER_IMAGE_FOOTPRINT_EXTENSION_NAME, VK_NV_SHADER_IMAGE_FOOTPRINT_SPEC_VERSION, 22); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceExclusiveScissorFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXCLUSIVE_SCISSOR_FEATURES_NV, VK_NV_SCISSOR_EXCLUSIVE_EXTENSION_NAME, VK_NV_SCISSOR_EXCLUSIVE_SPEC_VERSION, 21); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceTimelineSemaphoreFeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TIMELINE_SEMAPHORE_FEATURES_KHR, VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME, VK_KHR_TIMELINE_SEMAPHORE_SPEC_VERSION, 20); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceVulkanMemoryModelFeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_MEMORY_MODEL_FEATURES_KHR, VK_KHR_VULKAN_MEMORY_MODEL_EXTENSION_NAME, VK_KHR_VULKAN_MEMORY_MODEL_SPEC_VERSION, 19); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceFragmentDensityMapFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_DENSITY_MAP_FEATURES_EXT, VK_EXT_FRAGMENT_DENSITY_MAP_EXTENSION_NAME, VK_EXT_FRAGMENT_DENSITY_MAP_SPEC_VERSION, 18); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceScalarBlockLayoutFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SCALAR_BLOCK_LAYOUT_FEATURES_EXT, VK_EXT_SCALAR_BLOCK_LAYOUT_EXTENSION_NAME, VK_EXT_SCALAR_BLOCK_LAYOUT_SPEC_VERSION, 17); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceMemoryPriorityFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PRIORITY_FEATURES_EXT, VK_EXT_MEMORY_PRIORITY_EXTENSION_NAME, VK_EXT_MEMORY_PRIORITY_SPEC_VERSION, 16); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceDedicatedAllocationImageAliasingFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DEDICATED_ALLOCATION_IMAGE_ALIASING_FEATURES_NV, VK_NV_DEDICATED_ALLOCATION_IMAGE_ALIASING_EXTENSION_NAME, VK_NV_DEDICATED_ALLOCATION_IMAGE_ALIASING_SPEC_VERSION, 15); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceBufferDeviceAddressFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_BUFFER_DEVICE_ADDRESS_FEATURES_EXT, VK_EXT_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME, VK_EXT_BUFFER_DEVICE_ADDRESS_SPEC_VERSION, 14); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceCooperativeMatrixFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_COOPERATIVE_MATRIX_FEATURES_NV, VK_NV_COOPERATIVE_MATRIX_EXTENSION_NAME, VK_NV_COOPERATIVE_MATRIX_SPEC_VERSION, 13); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceCoverageReductionModeFeaturesNV>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_COVERAGE_REDUCTION_MODE_FEATURES_NV, VK_NV_COVERAGE_REDUCTION_MODE_EXTENSION_NAME, VK_NV_COVERAGE_REDUCTION_MODE_SPEC_VERSION, 12); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceFragmentShaderInterlockFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADER_INTERLOCK_FEATURES_EXT, VK_EXT_FRAGMENT_SHADER_INTERLOCK_EXTENSION_NAME, VK_EXT_FRAGMENT_SHADER_INTERLOCK_SPEC_VERSION, 11); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceYcbcrImageArraysFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_YCBCR_IMAGE_ARRAYS_FEATURES_EXT, VK_EXT_YCBCR_IMAGE_ARRAYS_EXTENSION_NAME, VK_EXT_YCBCR_IMAGE_ARRAYS_SPEC_VERSION, 10); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceUniformBufferStandardLayoutFeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_UNIFORM_BUFFER_STANDARD_LAYOUT_FEATURES_KHR, VK_KHR_UNIFORM_BUFFER_STANDARD_LAYOUT_EXTENSION_NAME, VK_KHR_UNIFORM_BUFFER_STANDARD_LAYOUT_SPEC_VERSION, 9); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceHostQueryResetFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_HOST_QUERY_RESET_FEATURES_EXT, VK_EXT_HOST_QUERY_RESET_EXTENSION_NAME, VK_EXT_HOST_QUERY_RESET_SPEC_VERSION, 8); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceIndexTypeUint8FeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INDEX_TYPE_UINT8_FEATURES_EXT, VK_EXT_INDEX_TYPE_UINT8_EXTENSION_NAME, VK_EXT_INDEX_TYPE_UINT8_SPEC_VERSION, 7); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceShaderDemoteToHelperInvocationFeaturesEXT>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_DEMOTE_TO_HELPER_INVOCATION_FEATURES_EXT, VK_EXT_SHADER_DEMOTE_TO_HELPER_INVOCATION_EXTENSION_NAME, VK_EXT_SHADER_DEMOTE_TO_HELPER_INVOCATION_SPEC_VERSION, 6); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDevicePipelineExecutablePropertiesFeaturesKHR>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PIPELINE_EXECUTABLE_PROPERTIES_FEATURES_KHR, VK_KHR_PIPELINE_EXECUTABLE_PROPERTIES_EXTENSION_NAME, VK_KHR_PIPELINE_EXECUTABLE_PROPERTIES_SPEC_VERSION, 5); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceMultiviewFeatures>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTIVIEW_FEATURES_KHR, VK_KHR_MULTIVIEW_EXTENSION_NAME, VK_KHR_MULTIVIEW_SPEC_VERSION, 4); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDevice16BitStorageFeatures>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_16BIT_STORAGE_FEATURES_KHR, VK_KHR_16BIT_STORAGE_EXTENSION_NAME, VK_KHR_16BIT_STORAGE_SPEC_VERSION, 3); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceVariablePointersFeatures>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VARIABLE_POINTERS_FEATURES_KHR, VK_KHR_VARIABLE_POINTERS_EXTENSION_NAME, VK_KHR_VARIABLE_POINTERS_SPEC_VERSION, 2); }
template<> FeatureDesc makeFeatureDesc<VkPhysicalDeviceSamplerYcbcrConversionFeatures>(void) { return FeatureDesc(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SAMPLER_YCBCR_CONVERSION_FEATURES_KHR, VK_KHR_SAMPLER_YCBCR_CONVERSION_EXTENSION_NAME, VK_KHR_SAMPLER_YCBCR_CONVERSION_SPEC_VERSION, 1); }


static const FeatureStructMapItem featureStructCreatorMap[] =
{
	{ createFeatureStructWrapper<VkPhysicalDeviceProtectedMemoryFeatures>, DECL_PROTECTED_MEMORY_EXTENSION_NAME, 0 },
	{ createFeatureStructWrapper<VkPhysicalDeviceShaderDrawParametersFeatures>, VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME, VK_KHR_SHADER_DRAW_PARAMETERS_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceTransformFeedbackFeaturesEXT>, VK_EXT_TRANSFORM_FEEDBACK_EXTENSION_NAME, VK_EXT_TRANSFORM_FEEDBACK_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceCornerSampledImageFeaturesNV>, VK_NV_CORNER_SAMPLED_IMAGE_EXTENSION_NAME, VK_NV_CORNER_SAMPLED_IMAGE_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceConditionalRenderingFeaturesEXT>, VK_EXT_CONDITIONAL_RENDERING_EXTENSION_NAME, VK_EXT_CONDITIONAL_RENDERING_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceFloat16Int8FeaturesKHR>, VK_KHR_SHADER_FLOAT16_INT8_EXTENSION_NAME, VK_KHR_SHADER_FLOAT16_INT8_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceDepthClipEnableFeaturesEXT>, VK_EXT_DEPTH_CLIP_ENABLE_EXTENSION_NAME, VK_EXT_DEPTH_CLIP_ENABLE_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceImagelessFramebufferFeaturesKHR>, VK_KHR_IMAGELESS_FRAMEBUFFER_EXTENSION_NAME, VK_KHR_IMAGELESS_FRAMEBUFFER_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceInlineUniformBlockFeaturesEXT>, VK_EXT_INLINE_UNIFORM_BLOCK_EXTENSION_NAME, VK_EXT_INLINE_UNIFORM_BLOCK_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceBlendOperationAdvancedFeaturesEXT>, VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME, VK_EXT_BLEND_OPERATION_ADVANCED_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceDescriptorIndexingFeaturesEXT>, VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME, VK_EXT_DESCRIPTOR_INDEXING_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceShadingRateImageFeaturesNV>, VK_NV_SHADING_RATE_IMAGE_EXTENSION_NAME, VK_NV_SHADING_RATE_IMAGE_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceRepresentativeFragmentTestFeaturesNV>, VK_NV_REPRESENTATIVE_FRAGMENT_TEST_EXTENSION_NAME, VK_NV_REPRESENTATIVE_FRAGMENT_TEST_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDevice8BitStorageFeaturesKHR>, VK_KHR_8BIT_STORAGE_EXTENSION_NAME, VK_KHR_8BIT_STORAGE_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceShaderAtomicInt64FeaturesKHR>, VK_KHR_SHADER_ATOMIC_INT64_EXTENSION_NAME, VK_KHR_SHADER_ATOMIC_INT64_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceShaderClockFeaturesKHR>, VK_KHR_SHADER_CLOCK_EXTENSION_NAME, VK_KHR_SHADER_CLOCK_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceVertexAttributeDivisorFeaturesEXT>, VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_EXTENSION_NAME, VK_EXT_VERTEX_ATTRIBUTE_DIVISOR_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceComputeShaderDerivativesFeaturesNV>, VK_NV_COMPUTE_SHADER_DERIVATIVES_EXTENSION_NAME, VK_NV_COMPUTE_SHADER_DERIVATIVES_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceMeshShaderFeaturesNV>, VK_NV_MESH_SHADER_EXTENSION_NAME, VK_NV_MESH_SHADER_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceFragmentShaderBarycentricFeaturesNV>, VK_NV_FRAGMENT_SHADER_BARYCENTRIC_EXTENSION_NAME, VK_NV_FRAGMENT_SHADER_BARYCENTRIC_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceShaderImageFootprintFeaturesNV>, VK_NV_SHADER_IMAGE_FOOTPRINT_EXTENSION_NAME, VK_NV_SHADER_IMAGE_FOOTPRINT_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceExclusiveScissorFeaturesNV>, VK_NV_SCISSOR_EXCLUSIVE_EXTENSION_NAME, VK_NV_SCISSOR_EXCLUSIVE_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceTimelineSemaphoreFeaturesKHR>, VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME, VK_KHR_TIMELINE_SEMAPHORE_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceVulkanMemoryModelFeaturesKHR>, VK_KHR_VULKAN_MEMORY_MODEL_EXTENSION_NAME, VK_KHR_VULKAN_MEMORY_MODEL_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceFragmentDensityMapFeaturesEXT>, VK_EXT_FRAGMENT_DENSITY_MAP_EXTENSION_NAME, VK_EXT_FRAGMENT_DENSITY_MAP_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceScalarBlockLayoutFeaturesEXT>, VK_EXT_SCALAR_BLOCK_LAYOUT_EXTENSION_NAME, VK_EXT_SCALAR_BLOCK_LAYOUT_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceMemoryPriorityFeaturesEXT>, VK_EXT_MEMORY_PRIORITY_EXTENSION_NAME, VK_EXT_MEMORY_PRIORITY_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceDedicatedAllocationImageAliasingFeaturesNV>, VK_NV_DEDICATED_ALLOCATION_IMAGE_ALIASING_EXTENSION_NAME, VK_NV_DEDICATED_ALLOCATION_IMAGE_ALIASING_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceBufferDeviceAddressFeaturesEXT>, VK_EXT_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME, VK_EXT_BUFFER_DEVICE_ADDRESS_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceCooperativeMatrixFeaturesNV>, VK_NV_COOPERATIVE_MATRIX_EXTENSION_NAME, VK_NV_COOPERATIVE_MATRIX_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceCoverageReductionModeFeaturesNV>, VK_NV_COVERAGE_REDUCTION_MODE_EXTENSION_NAME, VK_NV_COVERAGE_REDUCTION_MODE_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceFragmentShaderInterlockFeaturesEXT>, VK_EXT_FRAGMENT_SHADER_INTERLOCK_EXTENSION_NAME, VK_EXT_FRAGMENT_SHADER_INTERLOCK_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceYcbcrImageArraysFeaturesEXT>, VK_EXT_YCBCR_IMAGE_ARRAYS_EXTENSION_NAME, VK_EXT_YCBCR_IMAGE_ARRAYS_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceUniformBufferStandardLayoutFeaturesKHR>, VK_KHR_UNIFORM_BUFFER_STANDARD_LAYOUT_EXTENSION_NAME, VK_KHR_UNIFORM_BUFFER_STANDARD_LAYOUT_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceHostQueryResetFeaturesEXT>, VK_EXT_HOST_QUERY_RESET_EXTENSION_NAME, VK_EXT_HOST_QUERY_RESET_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceIndexTypeUint8FeaturesEXT>, VK_EXT_INDEX_TYPE_UINT8_EXTENSION_NAME, VK_EXT_INDEX_TYPE_UINT8_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceShaderDemoteToHelperInvocationFeaturesEXT>, VK_EXT_SHADER_DEMOTE_TO_HELPER_INVOCATION_EXTENSION_NAME, VK_EXT_SHADER_DEMOTE_TO_HELPER_INVOCATION_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDevicePipelineExecutablePropertiesFeaturesKHR>, VK_KHR_PIPELINE_EXECUTABLE_PROPERTIES_EXTENSION_NAME, VK_KHR_PIPELINE_EXECUTABLE_PROPERTIES_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceMultiviewFeatures>, VK_KHR_MULTIVIEW_EXTENSION_NAME, VK_KHR_MULTIVIEW_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDevice16BitStorageFeatures>, VK_KHR_16BIT_STORAGE_EXTENSION_NAME, VK_KHR_16BIT_STORAGE_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceVariablePointersFeatures>, VK_KHR_VARIABLE_POINTERS_EXTENSION_NAME, VK_KHR_VARIABLE_POINTERS_SPEC_VERSION },
	{ createFeatureStructWrapper<VkPhysicalDeviceSamplerYcbcrConversionFeatures>, VK_KHR_SAMPLER_YCBCR_CONVERSION_EXTENSION_NAME, VK_KHR_SAMPLER_YCBCR_CONVERSION_SPEC_VERSION },
};
} // vk

