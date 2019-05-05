/*-------------------------------------------------------------------------
 * Vulkan Conformance Tests
 * ------------------------
 *
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *//*!
 * \file
 * \brief Vulkan Extension Dependencies Tests
 *//*--------------------------------------------------------------------*/

#include "vktApiExtensionDependenciesTests.hpp"

#include <vector>
#include <string>

#include "deStringUtil.hpp"
#include "deUniquePtr.hpp"

#include "tcuTestLog.hpp"
#include "tcuResultCollector.hpp"

#include "vkApiVersion.hpp"
#include "vkQueryUtil.hpp"

#include "vktTestCaseUtil.hpp"
#include "vktTestGroupUtil.hpp"

namespace vkt
{
namespace api
{

namespace
{

#include "vkApiExtensionDependencyInfo.inl"

using namespace vk;
using std::string;
using std::vector;
using tcu::TestLog;

static const std::string VULKAN_1_0_PREFIX	= "1_0_";
static const std::string VULKAN_1_1_PREFIX	= "1_1_";

struct ExtensionDependencyCase
{
	typedef tcu::TestStatus (*Function) (Context& context, const ApiVersion version, const char* extension, const char* dependency);

	Function			testFunction;
	const ApiVersion	version;
	const char*			extension;
	const char*			dependency;

	ExtensionDependencyCase (Function testFunction_, const ApiVersion version_, const char* extension_, const char* dependency_)
		: testFunction	(testFunction_)
		, version		(version_)
		, extension		(extension_)
		, dependency	(dependency_)
	{}

	ExtensionDependencyCase (void)
		: testFunction	((Function)DE_NULL)
		, version		(ApiVersion(0, 0, 0))
		, extension		(DE_NULL)
		, dependency	(DE_NULL)
	{}
};

tcu::TestStatus checkExtensionDependency (Context& context, const ApiVersion version, const char* extension, const char* dependency)
{
	if (!context.contextSupports(version))
	{
		TCU_THROW(NotSupportedError, "Api version is not supported");
	}
	if (context.contextSupports(ApiVersion(1, 1, 0)) && pack(version) < pack(ApiVersion(1, 1, 0)))
	{
		TCU_THROW(NotSupportedError, "Extension has been promoted to core or tested in 1.1 dependencies");
	}

	TestLog&							log							= context.getTestContext().getLog();
	tcu::ResultCollector				results						(log);

	const vector<VkExtensionProperties>	instanceExtensionProperties	= enumerateInstanceExtensionProperties(context.getPlatformInterface(), DE_NULL);
	const vector<VkExtensionProperties>	deviceExtensionProperties	= enumerateDeviceExtensionProperties(context.getInstanceInterface(), context.getPhysicalDevice(), DE_NULL);

	if (!isExtensionSupported(deviceExtensionProperties, RequiredExtension(extension))) {
		TCU_THROW(NotSupportedError, "Extension is not supported");
	}

	if (!isExtensionSupported(deviceExtensionProperties, RequiredExtension(dependency)) &&
		!isExtensionSupported(instanceExtensionProperties, RequiredExtension(dependency))) {
		results.fail("Extension " + string(extension) + " is missing dependency: " + string(dependency));
	}

	return tcu::TestStatus(results.getResult(), results.getMessage());
}

tcu::TestStatus execExtensionDependenciesTest (Context& context, ExtensionDependencyCase testCase)
{
	return testCase.testFunction(context, testCase.version, testCase.extension, testCase.dependency);
}

inline std::string createExtensionDependencyCaseName(const std::string& prefix, const std::pair<const char*, const char*>& pair) {
	return prefix + de::toLower(string(pair.first)) + "_requires_" + de::toLower(string(pair.second));
}

void createInstanceExtensionDependenciesTests (tcu::TestCaseGroup* testGroup, ExtensionDependencyCase::Function testFunction)
{
	for (int idx = 0; idx < DE_LENGTH_OF_ARRAY(instanceExtensionDependencies_1_0); idx++)
	{
		addFunctionCase(testGroup,
						createExtensionDependencyCaseName(VULKAN_1_0_PREFIX, instanceExtensionDependencies_1_0[idx]),
						"",
						execExtensionDependenciesTest,
						ExtensionDependencyCase(testFunction,
												ApiVersion(1, 0, 0),
												instanceExtensionDependencies_1_0[idx].first,
												instanceExtensionDependencies_1_0[idx].second));
	}

	for (int idx = 0; idx < DE_LENGTH_OF_ARRAY(instanceExtensionDependencies_1_1); idx++)
	{
		addFunctionCase(testGroup,
						createExtensionDependencyCaseName(VULKAN_1_1_PREFIX, instanceExtensionDependencies_1_1[idx]),
						"",
						execExtensionDependenciesTest,
						ExtensionDependencyCase(testFunction,
												ApiVersion(1, 1, 0),
												instanceExtensionDependencies_1_1[idx].first,
												instanceExtensionDependencies_1_1[idx].second));
	}
}

void createDeviceExtensionDependenciesTests (tcu::TestCaseGroup* testGroup, ExtensionDependencyCase::Function testFunction)
{
	for (int idx = 0; idx < DE_LENGTH_OF_ARRAY(deviceExtensionDependencies_1_0); idx++)
	{
		addFunctionCase(testGroup,
						createExtensionDependencyCaseName(VULKAN_1_0_PREFIX, deviceExtensionDependencies_1_0[idx]),
						"",
						execExtensionDependenciesTest,
						ExtensionDependencyCase(testFunction,
												ApiVersion(1, 0, 0),
												deviceExtensionDependencies_1_0[idx].first,
												deviceExtensionDependencies_1_0[idx].second));
	}

	for (int idx = 0; idx < DE_LENGTH_OF_ARRAY(deviceExtensionDependencies_1_1); idx++)
	{
		addFunctionCase(testGroup,
						createExtensionDependencyCaseName(VULKAN_1_1_PREFIX, deviceExtensionDependencies_1_1[idx]),
						"",
						execExtensionDependenciesTest,
						ExtensionDependencyCase(testFunction,
												ApiVersion(1, 1, 0),
												deviceExtensionDependencies_1_1[idx].first,
												deviceExtensionDependencies_1_1[idx].second));
	}
}

} // anonymous

tcu::TestCaseGroup* createExtensionDependenciesTests (tcu::TestContext& testCtx)
{
	de::MovePtr<tcu::TestCaseGroup> extensionDependenciesTests(new tcu::TestCaseGroup(testCtx, "extension_dependencies", "Extension dependencies tests"));

	extensionDependenciesTests->addChild(createTestGroup(testCtx, "instance",	"Instance extension dependencies tests",	createInstanceExtensionDependenciesTests,	checkExtensionDependency));
	extensionDependenciesTests->addChild(createTestGroup(testCtx, "device",		"Device extension dependencies tests",		createDeviceExtensionDependenciesTests,		checkExtensionDependency));

	return extensionDependenciesTests.release();
}

} // api
} // vkt
