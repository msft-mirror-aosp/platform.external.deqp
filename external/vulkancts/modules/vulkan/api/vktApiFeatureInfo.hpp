#ifndef _VKTAPIFEATUREINFO_HPP
#define _VKTAPIFEATUREINFO_HPP
/*-------------------------------------------------------------------------
 * Vulkan Conformance Tests
 * ------------------------
 *
 * Copyright (c) 2015 Google Inc.
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
 * \brief API Feature Query tests
 *//*--------------------------------------------------------------------*/

#include "tcuDefs.hpp"
#include "tcuTestCase.hpp"

namespace vkt
{
namespace api
{

tcu::TestCaseGroup*		createFeatureInfoTests				(tcu::TestContext& testCtx);
void					createFeatureInfoInstanceTests		(tcu::TestCaseGroup* testGroup);
void					createFeatureInfoDeviceTests		(tcu::TestCaseGroup* testGroup);
void					createFeatureInfoDeviceGroupTests	(tcu::TestCaseGroup* testGroup);

} // api
} // vkt

#endif // _VKTAPIFEATUREINFO_HPP
