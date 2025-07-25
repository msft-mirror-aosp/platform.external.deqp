#ifndef _VKDEBUGREPORTUTIL_HPP
#define _VKDEBUGREPORTUTIL_HPP
/*-------------------------------------------------------------------------
 * Vulkan CTS Framework
 * --------------------
 *
 * Copyright (c) 2016 Google Inc.
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
 * \brief VK_EXT_debug_report utilities
 *//*--------------------------------------------------------------------*/

#include "vkDefs.hpp"
#include "vkRef.hpp"
#include "deAppendList.hpp"

#include <ostream>

namespace vk
{

#ifndef CTS_USES_VULKANSC

struct DebugReportMessage
{
    VkDebugReportFlagsEXT flags;
    VkDebugReportObjectTypeEXT objectType;
    uint64_t object;
    size_t location;
    int32_t messageCode;
    std::string layerPrefix;
    std::string message;

    DebugReportMessage(void)
        : flags(0)
        , objectType((VkDebugReportObjectTypeEXT)0)
        , object(0)
        , location(0)
        , messageCode(0)
    {
    }

    DebugReportMessage(VkDebugReportFlagsEXT flags_, VkDebugReportObjectTypeEXT objectType_, uint64_t object_,
                       size_t location_, int32_t messageCode_, const std::string &layerPrefix_,
                       const std::string &message_)
        : flags(flags_)
        , objectType(objectType_)
        , object(object_)
        , location(location_)
        , messageCode(messageCode_)
        , layerPrefix(layerPrefix_)
        , message(message_)
    {
    }

    bool isError() const
    {
        static const vk::VkDebugReportFlagsEXT errorFlags = vk::VK_DEBUG_REPORT_ERROR_BIT_EXT;
        return ((flags & errorFlags) != 0u);
    }

    bool shouldBeLogged() const
    {
        // \note We are not logging INFORMATION and DEBUG messages
        static const vk::VkDebugReportFlagsEXT otherFlags =
            vk::VK_DEBUG_REPORT_WARNING_BIT_EXT | vk::VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT;
        return (isError() || ((flags & otherFlags) != 0u));
    }
};

std::ostream &operator<<(std::ostream &str, const DebugReportMessage &message);

class DebugReportRecorder
{
public:
    using MessageList = de::AppendList<DebugReportMessage>;

    DebugReportRecorder(bool printValidationErrors);
    ~DebugReportRecorder(void);

    MessageList &getMessages(void)
    {
        return m_messages;
    }
    void clearMessages(void)
    {
        m_messages.clear();
    }
    bool errorPrinting(void) const
    {
        return m_print_errors;
    }

    VkDebugReportCallbackCreateInfoEXT makeCreateInfo(void);
    Move<VkDebugReportCallbackEXT> createCallback(const InstanceInterface &vki, VkInstance instance);

private:
    MessageList m_messages;
    const bool m_print_errors;
};

#endif // CTS_USES_VULKANSC

bool isDebugReportSupported(const PlatformInterface &vkp);

} // namespace vk

#endif // _VKDEBUGREPORTUTIL_HPP
