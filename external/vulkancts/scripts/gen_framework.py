# -*- coding: utf-8 -*-

#-------------------------------------------------------------------------
# Vulkan CTS
# ----------
#
# Copyright (c) 2015 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#-------------------------------------------------------------------------

import os
import re
import sys
import copy
from itertools import chain
from collections import OrderedDict

sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "..", "scripts"))

from build.common import DEQP_DIR
from khr_util.format import indentLines, writeInlFile

VULKAN_H	= os.path.join(os.path.dirname(__file__), "src", "vulkan.h.in")
VULKAN_DIR	= os.path.join(os.path.dirname(__file__), "..", "framework", "vulkan")

INL_HEADER = """\
/* WARNING: This is auto-generated file. Do not modify, since changes will
 * be lost! Modify the generating script instead.
 */\
"""

DEFINITIONS			= [
	("VK_API_VERSION_1_0",					"deUint32"),
	("VK_API_VERSION_1_1",					"deUint32"),
	("VK_MAX_PHYSICAL_DEVICE_NAME_SIZE",	"size_t"),
	("VK_MAX_EXTENSION_NAME_SIZE",			"size_t"),
	("VK_MAX_DRIVER_NAME_SIZE_KHR",			"size_t"),
	("VK_MAX_DRIVER_INFO_SIZE_KHR",			"size_t"),
	("VK_UUID_SIZE",						"size_t"),
	("VK_LUID_SIZE",						"size_t"),
	("VK_MAX_MEMORY_TYPES",					"size_t"),
	("VK_MAX_MEMORY_HEAPS",					"size_t"),
	("VK_MAX_DESCRIPTION_SIZE",				"size_t"),
	("VK_MAX_DEVICE_GROUP_SIZE",			"size_t"),
	("VK_ATTACHMENT_UNUSED",				"deUint32"),
	("VK_SUBPASS_EXTERNAL",					"deUint32"),
	("VK_QUEUE_FAMILY_IGNORED",				"deUint32"),
	("VK_QUEUE_FAMILY_EXTERNAL",			"deUint32"),
	("VK_REMAINING_MIP_LEVELS",				"deUint32"),
	("VK_REMAINING_ARRAY_LAYERS",			"deUint32"),
	("VK_WHOLE_SIZE",						"vk::VkDeviceSize"),
	("VK_TRUE",								"vk::VkBool32"),
	("VK_FALSE",							"vk::VkBool32"),
]

PLATFORM_TYPES		= [
	# VK_KHR_xlib_surface
	(["Display","*"],						["XlibDisplayPtr"],				"void*"),
	(["Window"],							["XlibWindow"],					"deUintptr",),
	(["VisualID"],							["XlibVisualID"],				"deUint32"),

	# VK_KHR_xcb_surface
	(["xcb_connection_t", "*"],				["XcbConnectionPtr"],			"void*"),
	(["xcb_window_t"],						["XcbWindow"],					"deUintptr"),
	(["xcb_visualid_t"],					["XcbVisualid"],				"deUint32"),

	# VK_KHR_wayland_surface
	(["struct", "wl_display","*"],			["WaylandDisplayPtr"],			"void*"),
	(["struct", "wl_surface", "*"],			["WaylandSurfacePtr"],			"void*"),

	# VK_KHR_mir_surface
	(["MirConnection", "*"],				["MirConnectionPtr"],			"void*"),
	(["MirSurface", "*"],					["MirSurfacePtr"],				"void*"),

	# VK_KHR_android_surface
	(["ANativeWindow", "*"],				["AndroidNativeWindowPtr"],		"void*"),

	# VK_KHR_win32_surface
	(["HINSTANCE"],							["Win32InstanceHandle"],		"void*"),
	(["HWND"],								["Win32WindowHandle"],			"void*"),
	(["HANDLE"],							["Win32Handle"],				"void*"),
	(["const", "SECURITY_ATTRIBUTES", "*"],	["Win32SecurityAttributesPtr"],	"const void*"),
	(["AHardwareBuffer", "*"],				["AndroidHardwareBufferPtr"],	"void*"),

	# VK_EXT_acquire_xlib_display
	(["RROutput"],							["RROutput"],					"void*")
]

PLATFORM_TYPE_NAMESPACE	= "pt"

TYPE_SUBSTITUTIONS		= [
	("uint8_t",		"deUint8"),
	("uint16_t",	"deUint16"),
	("uint32_t",	"deUint32"),
	("uint64_t",	"deUint64"),
	("int8_t",		"deInt8"),
	("int16_t",		"deInt16"),
	("int32_t",		"deInt32"),
	("int64_t",		"deInt64"),
	("bool32_t",	"deUint32"),
	("size_t",		"deUintptr"),

	# Platform-specific
	("DWORD",		"deUint32"),
	("HANDLE*",		PLATFORM_TYPE_NAMESPACE + "::" + "Win32Handle*"),
	("LPCWSTR",		"char*"),
]

EXTENSION_POSTFIXES				= ["KHR", "EXT", "NV", "NVX", "KHX", "NN", "MVK"]
EXTENSION_POSTFIXES_STANDARD	= ["KHR"]

def prefixName (prefix, name):
	name = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', name[2:])
	name = re.sub(r'([a-zA-Z])([0-9])', r'\1_\2', name)
	name = name.upper()

	name = name.replace("YCB_CR_", "YCBCR_")
	name = name.replace("WIN_32_", "WIN32_")
	name = name.replace("16_BIT_", "16BIT_")
	name = name.replace("D_3_D_12_", "D3D12_")
	name = name.replace("IOSSURFACE_", "IOS_SURFACE_")
	name = name.replace("MAC_OS", "MACOS_")
	name = name.replace("TEXTURE_LOD", "TEXTURE_LOD_")
	name = name.replace("VIEWPORT_W", "VIEWPORT_W_")
	name = name.replace("_IDPROPERTIES", "_ID_PROPERTIES")

	return prefix + name

class Version:
	def __init__ (self, versionTuple):
		self.major = versionTuple[0]
		self.minor = versionTuple[1]
		self.patch = versionTuple[2]

	def getInHex (self):
		if self.major == 1 and self.minor == 0 and self.patch == 0:
			return "VK_API_VERSION_1_0"
		elif self.major == 1 and self.minor == 1 and self.patch == 0:
			return "VK_API_VERSION_1_1"
		else:
			hex = (self.major << 22) | (self.minor << 12) | self.patch
			return '0x%Xu' % (hex)

	def isStandardVersion (self):
		if self.patch != 0:
			return False
		if self.major != 1:
			return False
		if self.minor != 1 and self.minor != 0:
			return False
		return True

	def getBestRepresentation (self):
		if self.isStandardVersion():
			return self.getInHex()
		return self.getDefineName()

	def getDefineName (self):
		return 'VERSION_%d_%d_%d' % (self.major, self.minor, self.patch)

	def __hash__ (self):
		return (self.major << 22) | (self.minor << 12) | self.patch

	def __eq__ (self, other):
		return self.major == other.major and self.minor == other.minor and self.patch == other.patch

	def __str__ (self):
		return self.getBestRepresentation()


class Handle:
	TYPE_DISP		= 0
	TYPE_NONDISP	= 1

	def __init__ (self, type, name):
		self.type		= type
		self.name		= name
		self.alias		= None
		self.isAlias	= False

	def getHandleType (self):
		return prefixName("HANDLE_TYPE_", self.name)

	def checkAliasValidity (self):
		pass

	def __repr__ (self):
		return '%s (%s, %s)' % (self.name, self.alias, self.isAlias)

class Definition:
	def __init__ (self, type, name, value):
		self.type	= type
		self.name	= name
		self.value	= value
		self.alias	= None
		self.isAlias	= False

	def __repr__ (self):
		return '%s = %s (%s)' % (self.name, self.value, self.type)

class Enum:
	def __init__ (self, name, values):
		self.name		= name
		self.values		= values
		self.alias		= None
		self.isAlias	= False

	def checkAliasValidity (self):
		if self.alias != None:
			if len(self.values) != len(self.alias.values):
				raise Exception("%s has different number of flags than its alias %s." % (self.name, self.alias.name))
			for index, value in enumerate(self.values):
				aliasVal = self.alias.values[index]
				if value[1] != aliasVal[1] or not (value[0].startswith(aliasVal[0]) or aliasVal[0].startswith(value[0])):
					raise Exception("Flag %s of %s has different value than %s of %s." % (self.alias.values[index], self.alias.name, value, self.name))

	def __repr__ (self):
		return '%s (%s) %s' % (self.name, self.alias, self.values)

class Bitfield:
	def __init__ (self, name, values):
		self.name		= name
		self.values		= values
		self.alias		= None
		self.isAlias	= False

	def checkAliasValidity (self):
		if self.alias != None:
			if len(self.values) != len(self.alias.values):
				raise Exception("%s has different number of flags than its alias %s." % (self.name, self.alias.name))
			for index, value in enumerate(self.values):
				aliasVal = self.alias.values[index]
				if value[1] != aliasVal[1] or not (value[0].startswith(aliasVal[0]) or aliasVal[0].startswith(value[0])):
					raise Exception("Flag %s of %s has different value than %s of %s." % (self.alias.values[index], self.alias.name, value, self.name))

	def __repr__ (self):
		return '%s (%s)' % (self.name, self.alias)

class Variable:
	def __init__ (self, type, name, arraySize):
		type		= type.replace('*',' *').replace('&',' &')
		for src, dst in TYPE_SUBSTITUTIONS:
			type = type.replace(src, dst)
		self.type	= type.split(' ')
		for platformType, substitute, compat in PLATFORM_TYPES:
			range = self.contains(self.type, platformType)
			if range != None:
				self.type = self.type[:range[0]]+[PLATFORM_TYPE_NAMESPACE + '::' + substitute[0]] + substitute[1:] + self.type[range[1]:]
				break
		self.name		= name
		self.arraySize	= arraySize

	def contains(self, big, small):
		for i in xrange(len(big)-len(small)+1):
			for j in xrange(len(small)):
				if big[i+j] != small[j]:
					break
			else:
				return i, i+len(small)
		return None

	def getType (self):
		return ' '.join(self.type).replace(' *','*').replace(' &','&')

	def getAsString (self, separator):
		return '%s%s%s%s' % (self.getType(), separator, self.name, self.arraySize)

	def __repr__ (self):
		return '<%s> <%s> <%s>' % (self.type, self.name, self.arraySize)

	def __eq__ (self, other):
		if len(self.type) != len(other.type):
			return False
		for index, type in enumerate(self.type):
			if "*" == type or "&" == type or "const" == type or "volatile" == type:
				if type != other.type[index]:
					return False
			elif type != other.type[index] and \
				type not in map(lambda ext: other.type[index] + ext, EXTENSION_POSTFIXES_STANDARD) and \
				other.type[index] not in map(lambda ext: type + ext, EXTENSION_POSTFIXES_STANDARD):
				return False
		return self.arraySize == other.arraySize

	def __ne__ (self, other):
		return not self == other

class CompositeType:
	CLASS_STRUCT	= 0
	CLASS_UNION		= 1

	def __init__ (self, typeClass, name, members):
		self.typeClass	= typeClass
		self.name		= name
		self.members	= members
		self.alias		= None
		self.isAlias	= False

	def getClassName (self):
		names = {CompositeType.CLASS_STRUCT: 'struct', CompositeType.CLASS_UNION: 'union'}
		return names[self.typeClass]

	def checkAliasValidity (self):
		if self.alias != None:
			if len(self.members) != len(self.alias.members):
				raise Exception("%s has different number of members than its alias %s." % (self.name, self.alias.name))
			for index, member in enumerate(self.members ):
				break
				#if member != self.alias.members[index]:
					#raise Exception("Member %s of %s is different than core member %s in %s." % (self.alias.members[index], self.alias.name, member, self.name))
					#raise Exception("Member ",str(self.alias.members[index])," of ", str(self.alias.name)," is different than core member ", str(member)," in ", str(self.name),".")
	def __repr__ (self):
		return '%s (%s)' % (self.name, self.alias)

class Function:
	TYPE_PLATFORM		= 0 # Not bound to anything
	TYPE_INSTANCE		= 1 # Bound to VkInstance
	TYPE_DEVICE			= 2 # Bound to VkDevice

	def __init__ (self, name, returnType, arguments, apiVersion = None):
		self.name		= name
		self.returnType	= returnType
		self.arguments	= arguments
		self.alias		= None
		self.isAlias	= False
		self.apiVersion	= apiVersion

	def getType (self):
		# Special functions
		if self.name == "vkGetInstanceProcAddr":
			return Function.TYPE_PLATFORM
		elif self.name == "vkGetDeviceProcAddr":
			return Function.TYPE_INSTANCE
		assert len(self.arguments) > 0
		firstArgType = self.arguments[0].getType()
		if firstArgType in ["VkInstance", "VkPhysicalDevice"]:
			return Function.TYPE_INSTANCE
		elif firstArgType in ["VkDevice", "VkCommandBuffer", "VkQueue"]:
			return Function.TYPE_DEVICE
		else:
			return Function.TYPE_PLATFORM

	def checkAliasValidity (self):
		if self.alias != None:
			if len(self.arguments) != len(self.alias.arguments):
				raise Exception("%s has different number of arguments than its alias %s." % (self.name, self.alias.name))
			if self.returnType != self.alias.returnType or not (self.returnType.startswith(self.alias.returnType) or self.alias.returnType.startswith(self.returnType)):
				raise Exception("%s has different return value's type than its alias %s." % (self.name, self.alias.name))
			for index, argument in enumerate(self.arguments):
				if argument != self.alias.arguments[index]:
					raise Exception("argument %s: \"%s\" of %s is different than \"%s\" of %s." % (index, self.alias.arguments[index].getAsString(' '), self.alias.name, argument.getAsString(' '), self.name))

	def __repr__ (self):
		return '%s (%s)' % (self.name, self.alias)

class Extension:
	def __init__ (self, name, handles, enums, bitfields, compositeTypes, functions, definitions, additionalDefinitions, versionInCore):
		self.name			= name
		self.definitions	= definitions
		self.additionalDefs = additionalDefinitions
		self.handles		= handles
		self.enums			= enums
		self.bitfields		= bitfields
		self.compositeTypes	= compositeTypes
		self.functions		= functions
		self.versionInCore	= versionInCore

	def __repr__ (self):
		return 'EXT:\n%s ->\nENUMS:\n%s\nCOMPOS:\n%s\nFUNCS:\n%s\nBITF:\n%s\nHAND:\n%s\nDEFS:\n%s\n' % (self.name, self.enums, self.compositeTypes, self.functions, self.bitfields, self.handles, self.definitions, self.versionInCore)

class API:
	def __init__ (self, definitions, handles, enums, bitfields, compositeTypes, functions, extensions):
		self.definitions	= definitions
		self.handles		= handles
		self.enums			= enums
		self.bitfields		= bitfields
		self.compositeTypes	= compositeTypes
		self.functions		= functions # \note contains extension functions as well
		self.extensions		= extensions

def readFile (filename):
	with open(filename, 'rb') as f:
		return f.read()

IDENT_PTRN	= r'[a-zA-Z_][a-zA-Z0-9_]*'
TYPE_PTRN	= r'[a-zA-Z_][a-zA-Z0-9_ \t*&]*'

def fixupEnumValues (values):
	fixed = []
	for name, value in values:
		if "_BEGIN_RANGE" in name or "_END_RANGE" in name:
			continue
		fixed.append((name, value))
	return fixed

def getInterfaceName (function):
	assert function.name[:2] == "vk"
	return function.name[2].lower() + function.name[3:]

def getFunctionTypeName (function):
	assert function.name[:2] == "vk"
	return function.name[2:] + "Func"

def endsWith (str, postfix):
	return str[-len(postfix):] == postfix

def splitNameExtPostfix (name):
	knownExtPostfixes = EXTENSION_POSTFIXES
	for postfix in knownExtPostfixes:
		if endsWith(name, postfix):
			return (name[:-len(postfix)], postfix)
	return (name, "")

def getBitEnumNameForBitfield (bitfieldName):
	bitfieldName, postfix = splitNameExtPostfix(bitfieldName)
	assert bitfieldName[-1] == "s"
	return bitfieldName[:-1] + "Bits" + postfix

def getBitfieldNameForBitEnum (bitEnumName):
	bitEnumName, postfix = splitNameExtPostfix(bitEnumName)
	assert bitEnumName[-4:] == "Bits"
	return bitEnumName[:-4] + "s" + postfix

def parsePreprocDefinedValue (src, name):
	value = parsePreprocDefinedValueOptional(src, name)
	if value is None:

		raise Exception("No such definition: %s" % name)
	return value

def parsePreprocDefinedValueOptional (src, name):
	definition = re.search(r'#\s*define\s+' + name + r'\s+([^\n]+)\n', src)
	if definition is None:
		return None
	value = definition.group(1).strip()
	if value == "UINT32_MAX":
		value = "(~0u)"
	return value

def parseEnum (name, src):
	keyValuePtrn	= '(' + IDENT_PTRN + r')\s*=\s*([^\s,}]+)\s*[,}]'
	matches			= re.findall(keyValuePtrn, src)

	return Enum(name, fixupEnumValues(matches))

# \note Parses raw enums, some are mapped to bitfields later
def parseEnums (src):
	matches	= re.findall(r'typedef enum(\s*' + IDENT_PTRN + r')?\s*{([^}]*)}\s*(' + IDENT_PTRN + r')\s*;', src)
	enums	= []
	for enumname, contents, typename in matches:
		enums.append(parseEnum(typename, contents))
	return enums



def parseCompositeType (type, name, src):
	typeNamePtrn	= r'(' + TYPE_PTRN + r')(\s+' + IDENT_PTRN + r')((\[[^\]]+\])*)\s*;'
	matches			= re.findall(typeNamePtrn, src)
	members			= [Variable(t.strip(), n.strip(), a.strip()) for t, n, a, _ in matches]
	return CompositeType(type, name, members)

def parseCompositeTypes (src):
	typeMap	= { 'struct': CompositeType.CLASS_STRUCT, 'union': CompositeType.CLASS_UNION }
	matches	= re.findall(r'typedef (struct|union)(\s*' + IDENT_PTRN + r')?\s*{([^}]*)}\s*(' + IDENT_PTRN + r')\s*;', src)
	types	= []
	for type, structname, contents, typename in matches:
		types.append(parseCompositeType(typeMap[type], typename, contents))
	return types

def parseHandles (src):
	matches	= re.findall(r'VK_DEFINE(_NON_DISPATCHABLE|)_HANDLE\((' + IDENT_PTRN + r')\)[ \t]*[\n\r]', src)
	handles	= []
	typeMap	= {'': Handle.TYPE_DISP, '_NON_DISPATCHABLE': Handle.TYPE_NONDISP}
	for type, name in matches:
		handle = Handle(typeMap[type], name)
		handles.append(handle)
	return handles

def parseArgList (src):
	typeNamePtrn	= r'(' + TYPE_PTRN + r')(\s+' + IDENT_PTRN + r')((\[[^\]]+\])*)\s*'
	args			= []
	for rawArg in src.split(','):
		m = re.search(typeNamePtrn, rawArg)
		args.append(Variable(m.group(1).strip(), m.group(2).strip(), m.group(3)))
	return args

def removeTypeExtPostfix (name):
	for extPostfix in EXTENSION_POSTFIXES_STANDARD:
		if endsWith(name, extPostfix):
			return name[0:-len(extPostfix)]
	return None

def populateAliases (objects):
	objectsByName = {}
	for object in objects:
		objectsByName[object.name] = object
	for object in objects:
		withoutPostfix = removeTypeExtPostfix(object.name)
		if withoutPostfix != None and withoutPostfix in objectsByName:
			objectsByName[withoutPostfix].alias = object
			object.isAlias = True
	for object in objects:
		object.checkAliasValidity()

def populateAliasesWithTypedefs (objects, src):
	objectsByName = {}
	for object in objects:
		objectsByName[object.name] = object
		ptrn	= r'\s*typedef\s+' + object.name + r'\s+([^;]+)'
		stash = re.findall(ptrn, src)
		if len(stash) == 1:
			objExt = copy.deepcopy(object)
			objExt.name = stash[0]
			object.alias = objExt
			objExt.isAlias = True
			objects.append(objExt)


def removeAliasedValues (enum):
	valueByName = {}
	for name, value in enum.values:
		valueByName[name] = value

	def removeDefExtPostfix (name):
		for extPostfix in EXTENSION_POSTFIXES:
			if endsWith(name, "_" + extPostfix):
				return name[0:-(len(extPostfix)+1)]
		return None

	newValues = []
	for name, value in enum.values:
		withoutPostfix = removeDefExtPostfix(name)
		if withoutPostfix != None and withoutPostfix in valueByName and valueByName[withoutPostfix] == value:
			continue
		newValues.append((name, value))
	enum.values = newValues

def parseFunctions (src):
	ptrn		= r'VKAPI_ATTR\s+(' + TYPE_PTRN + ')\s+VKAPI_CALL\s+(' + IDENT_PTRN + r')\s*\(([^)]*)\)\s*;'
	matches		= re.findall(ptrn, src)
	functions	= []
	for returnType, name, argList in matches:
		functions.append(Function(name.strip(), returnType.strip(), parseArgList(argList)))
	return functions

def parseFunctionsByVersion (src):
	ptrnVer10	= 'VK_VERSION_1_0 1'
	ptrnVer11	= 'VK_VERSION_1_1 1'
	matchVer10	= re.search(ptrnVer10, src)
	matchVer11	= re.search(ptrnVer11, src)
	ptrn		= r'VKAPI_ATTR\s+(' + TYPE_PTRN + ')\s+VKAPI_CALL\s+(' + IDENT_PTRN + r')\s*\(([^)]*)\)\s*;'
	regPtrn		= re.compile(ptrn)
	matches		= regPtrn.findall(src, matchVer10.start(), matchVer11.start())
	functions	= []
	for returnType, name, argList in matches:
		functions.append(Function(name.strip(), returnType.strip(), parseArgList(argList), 'VK_VERSION_1_0'))
	matches		= regPtrn.findall(src, matchVer11.start())
	for returnType, name, argList in matches:
		functions.append(Function(name.strip(), returnType.strip(), parseArgList(argList), 'VK_VERSION_1_1'))
	return functions

def splitByExtension (src):
	ptrn		= r'#define\s+[A-Z0-9_]+_EXTENSION_NAME\s+"([^"]+)"'
	match		= "#define\s+("
	for part in re.finditer(ptrn, src):
		 match += part.group(1)+"|"
	match = match[:-1] + ")\s+1"
	parts = re.split(match, src)
	# First part is core
	byExtension	= [(None, parts[0])]
	for ndx in range(1, len(parts), 2):
		byExtension.append((parts[ndx], parts[ndx+1]))
	return byExtension

def parseDefinitions (extensionName, src):

	def skipDefinition (extensionName, definition):
		if extensionName == None:
			return True
		if definition[0].startswith(extensionName.upper()):
			return True
		if definition[1].isdigit():
			return True
		return False

	ptrn		= r'#define\s+([^\s]+)\s+([^\r\n]+)'
	matches		= re.findall(ptrn, src)

	return [Definition(None, match[0], match[1]) for match in matches if not skipDefinition(extensionName, match)]

def parseExtensions (src, allFunctions, allCompositeTypes, allEnums, allBitfields, allHandles, allDefinitions):

	def getCoreVersion (extensionTuple):
		if not extensionTuple[0]:
			return None
		ptrn		= r'\/\/\s*' + extensionTuple[0] + r'\s+(DEVICE|INSTANCE)\s+([0-9_]+)'
		coreVersion = re.search(ptrn, extensionTuple[1], re.I)
		if coreVersion != None:
			return [coreVersion.group(1)] + [int(number) for number in coreVersion.group(2).split('_')[:3]]
		return None

	splitSrc				= splitByExtension(src)
	extensions				= []
	functionsByName			= {function.name: function for function in allFunctions}
	compositeTypesByName	= {compType.name: compType for compType in allCompositeTypes}
	enumsByName				= {enum.name: enum for enum in allEnums}
	bitfieldsByName			= {bitfield.name: bitfield for bitfield in allBitfields}
	handlesByName			= {handle.name: handle for handle in allHandles}
	definitionsByName		= {definition.name: definition for definition in allDefinitions}

	for extensionName, extensionSrc in splitSrc:
		definitions			= [Definition(type, name, parsePreprocDefinedValueOptional(extensionSrc, name)) for name, type in DEFINITIONS]
		definitions			= [definition for definition in definitions if definition.value != None]
		additionalDefinitions = parseDefinitions(extensionName, extensionSrc)
		handles				= parseHandles(extensionSrc)
		functions			= parseFunctions(extensionSrc)
		compositeTypes		= parseCompositeTypes(extensionSrc)
		rawEnums			= parseEnums(extensionSrc)
		bitfieldNames		= parseBitfieldNames(extensionSrc)
		enumBitfieldNames	= [getBitEnumNameForBitfield(name) for name in bitfieldNames]
		enums				= [enum for enum in rawEnums if enum.name not in enumBitfieldNames]

		extCoreVersion		= getCoreVersion((extensionName, extensionSrc))
		extFunctions		= [functionsByName[function.name] for function in functions]
		extCompositeTypes	= [compositeTypesByName[compositeType.name] for compositeType in compositeTypes]
		extEnums			= [enumsByName[enum.name] for enum in enums]
		extBitfields		= [bitfieldsByName[bitfieldName] for bitfieldName in bitfieldNames]
		extHandles			= [handlesByName[handle.name] for handle in handles]
		extDefinitions		= [definitionsByName[definition.name] for definition in definitions]

		extensions.append(Extension(extensionName, extHandles, extEnums, extBitfields, extCompositeTypes, extFunctions, extDefinitions, additionalDefinitions, extCoreVersion))
	return extensions

def parseBitfieldNames (src):
	ptrn		= r'typedef\s+VkFlags\s(' + IDENT_PTRN + r')\s*;'
	matches		= re.findall(ptrn, src)

	return matches

def parseAPI (src):
	definitions		= [Definition(type, name, parsePreprocDefinedValue(src, name)) for name, type in DEFINITIONS]
	handles			= parseHandles(src)
	rawEnums		= parseEnums(src)
	bitfieldNames	= parseBitfieldNames(src)
	enums			= []
	bitfields		= []
	bitfieldEnums	= set([getBitEnumNameForBitfield(n) for n in bitfieldNames if getBitEnumNameForBitfield(n) in [enum.name for enum in rawEnums]])
	compositeTypes	= parseCompositeTypes(src)
	allFunctions	= parseFunctionsByVersion(src)

	for enum in rawEnums:
		if enum.name in bitfieldEnums:
			bitfields.append(Bitfield(getBitfieldNameForBitEnum(enum.name), enum.values))
		else:
			enums.append(enum)

	for bitfieldName in bitfieldNames:
		if not bitfieldName in [bitfield.name for bitfield in bitfields]:
			# Add empty bitfield
			bitfields.append(Bitfield(bitfieldName, []))

	# Populate alias fields
	populateAliasesWithTypedefs(compositeTypes, src)
	populateAliasesWithTypedefs(enums, src)
	populateAliasesWithTypedefs(bitfields, src)
	populateAliases(allFunctions)
	populateAliases(handles)
	populateAliases(enums)
	populateAliases(bitfields)
	populateAliases(compositeTypes)


	for enum in enums:
		removeAliasedValues(enum)

	extensions			= parseExtensions(src, allFunctions, compositeTypes, enums, bitfields, handles, definitions)

	return API(
		definitions		= definitions,
		handles			= handles,
		enums			= enums,
		bitfields		= bitfields,
		compositeTypes	= compositeTypes,
		functions		= allFunctions,
		extensions		= extensions)

def splitUniqueAndDuplicatedEntries (handles):
	listOfUniqueHandles = []
	duplicates			= OrderedDict()
	for handle in handles:
		if handle.alias != None:
			duplicates[handle.alias] = handle
		if not handle.isAlias:
			listOfUniqueHandles.append(handle)
	return listOfUniqueHandles, duplicates

def writeHandleType (api, filename):
	uniqeHandles, duplicatedHandles = splitUniqueAndDuplicatedEntries(api.handles)

	def genHandles ():
		yield "\t%s\t= 0," % uniqeHandles[0].getHandleType()
		for handle in uniqeHandles[1:]:
			yield "\t%s," % handle.getHandleType()
		for duplicate in duplicatedHandles:
			yield "\t%s\t= %s," % (duplicate.getHandleType(), duplicatedHandles[duplicate].getHandleType())
		yield "\tHANDLE_TYPE_LAST\t= %s + 1" % (uniqeHandles[-1].getHandleType())

	def genHandlesBlock ():
		yield "enum HandleType"
		yield "{"

		for line in indentLines(genHandles()):
			yield line

		yield "};"
		yield ""

	writeInlFile(filename, INL_HEADER, genHandlesBlock())

def getEnumValuePrefix (enum):
	prefix = enum.name[0]
	for i in range(1, len(enum.name)):
		if enum.name[i].isupper() and not enum.name[i-1].isupper():
			prefix += "_"
		prefix += enum.name[i].upper()
	return prefix

def parseInt (value):
	if value[:2] == "0x":
		return int(value, 16)
	else:
		return int(value, 10)

def areEnumValuesLinear (enum):
	curIndex = 0
	for name, value in enum.values:
		if parseInt(value) != curIndex:
			return False
		curIndex += 1
	return True

def genEnumSrc (enum):
	yield "enum %s" % enum.name
	yield "{"

	for line in indentLines(["\t%s\t= %s," % v for v in enum.values]):
		yield line

	if areEnumValuesLinear(enum):
		yield ""
		yield "\t%s_LAST" % getEnumValuePrefix(enum)

	yield "};"

def genBitfieldSrc (bitfield):
	if len(bitfield.values) > 0:
		yield "enum %s" % getBitEnumNameForBitfield(bitfield.name)
		yield "{"
		for line in indentLines(["\t%s\t= %s," % v for v in bitfield.values]):
			yield line
		yield "};"
	yield "typedef deUint32 %s;" % bitfield.name

def genCompositeTypeSrc (type):
	yield "%s %s" % (type.getClassName(), type.name)
	yield "{"
	for line in indentLines(['\t'+m.getAsString('\t')+';' for m in type.members]):
		yield line
	yield "};"

def genHandlesSrc (handles):
	uniqeHandles, duplicatedHandles = splitUniqueAndDuplicatedEntries(handles)

	def genLines (handles):
		for handle in uniqeHandles:
			if handle.type == Handle.TYPE_DISP:
				yield "VK_DEFINE_HANDLE\t(%s,\t%s);" % (handle.name, handle.getHandleType())
			elif handle.type == Handle.TYPE_NONDISP:
				yield "VK_DEFINE_NON_DISPATCHABLE_HANDLE\t(%s,\t%s);" % (handle.name, handle.getHandleType())

		for duplicate in duplicatedHandles:
			if duplicate.type == Handle.TYPE_DISP:
				yield "VK_DEFINE_HANDLE\t(%s,\t%s);" % (duplicate.name, duplicatedHandles[duplicate].getHandleType())
			elif duplicate.type == Handle.TYPE_NONDISP:
				yield "VK_DEFINE_NON_DISPATCHABLE_HANDLE\t(%s,\t%s);" % (duplicate.name, duplicatedHandles[duplicate].getHandleType())

	for line in indentLines(genLines(handles)):
		yield line

def genDefinitionsSrc (definitions):
	for line in ["#define %s\t(static_cast<%s>\t(%s))" % (definition.name, definition.type, definition.value) for definition in definitions]:
		yield line

def genDefinitionsAliasSrc (definitions):
	for line in ["#define %s\t%s" % (definition.name, definitions[definition].name) for definition in definitions]:
		if definition.value != definitions[definition].value and definition.value != definitions[definition].name:
			raise Exception("Value of %s (%s) is different than core definition value %s (%s)." % (definition.name, definition.value, definitions[definition].name, definitions[definition].value))
		yield line

def writeBasicTypes (api, filename):

	def gen ():
		definitionsCore, definitionDuplicates = splitUniqueAndDuplicatedEntries(api.definitions)

		for line in indentLines(chain(genDefinitionsSrc(definitionsCore), genDefinitionsAliasSrc(definitionDuplicates))):
			yield line
		yield ""

		for line in genHandlesSrc(api.handles):
			yield line
		yield ""

		for enum in api.enums:
			if not enum.isAlias:
				for line in genEnumSrc(enum):
					yield line
			yield ""

		for bitfield in api.bitfields:
			if not bitfield.isAlias:
				for line in genBitfieldSrc(bitfield):
					yield line
			yield ""
		for line in indentLines(["VK_DEFINE_PLATFORM_TYPE(%s,\t%s);" % (s[0], c) for n, s, c in PLATFORM_TYPES]):
			yield line

		for ext in api.extensions:
			if ext.additionalDefs != None:
				for definition in ext.additionalDefs:
					yield "#define " + definition.name + " " + definition.value
	writeInlFile(filename, INL_HEADER, gen())

def writeCompositeTypes (api, filename):
	def gen ():
		for type in api.compositeTypes:
			type.checkAliasValidity()

			if not type.isAlias:
				for line in genCompositeTypeSrc(type):
					yield line
			yield ""

	writeInlFile(filename, INL_HEADER, gen())

def argListToStr (args):
	return ", ".join(v.getAsString(' ') for v in args)

def writeInterfaceDecl (api, filename, functionTypes, concrete):
	def genProtos ():
		postfix = "" if concrete else " = 0"
		for function in api.functions:
			if not function.getType() in functionTypes:
				continue
			if not function.isAlias:
				yield "virtual %s\t%s\t(%s) const%s;" % (function.returnType, getInterfaceName(function), argListToStr(function.arguments), postfix)

	writeInlFile(filename, INL_HEADER, indentLines(genProtos()))

def writeFunctionPtrTypes (api, filename):
	def genTypes ():
		for function in api.functions:
			yield "typedef VKAPI_ATTR %s\t(VKAPI_CALL* %s)\t(%s);" % (function.returnType, getFunctionTypeName(function), argListToStr(function.arguments))

	writeInlFile(filename, INL_HEADER, indentLines(genTypes()))

def writeFunctionPointers (api, filename, functionTypes):
	def FunctionsYielder ():
		for function in api.functions:
			if function.getType() in functionTypes:
				if function.isAlias:
					if function.getType() == Function.TYPE_INSTANCE and function.arguments[0].getType() == "VkPhysicalDevice":
						yield "%s\t%s;" % (getFunctionTypeName(function), getInterfaceName(function))
				else:
					yield "%s\t%s;" % (getFunctionTypeName(function), getInterfaceName(function))

	writeInlFile(filename, INL_HEADER, indentLines(FunctionsYielder()))

def writeInitFunctionPointers (api, filename, functionTypes, cond = None):
	def makeInitFunctionPointers ():
		for function in api.functions:
			if function.getType() in functionTypes and (cond == None or cond(function)):
				interfaceName = getInterfaceName(function)
				if function.isAlias:
					if function.getType() == Function.TYPE_INSTANCE and function.arguments[0].getType() == "VkPhysicalDevice":
						yield "m_vk.%s\t= (%s)\tGET_PROC_ADDR(\"%s\");" % (getInterfaceName(function), getFunctionTypeName(function), function.name)
				else:
					yield "m_vk.%s\t= (%s)\tGET_PROC_ADDR(\"%s\");" % (getInterfaceName(function), getFunctionTypeName(function), function.name)
					if function.alias != None:
						yield "if (!m_vk.%s)" % (getInterfaceName(function))
						yield "    m_vk.%s\t= (%s)\tGET_PROC_ADDR(\"%s\");" % (getInterfaceName(function), getFunctionTypeName(function), function.alias.name)
	lines = [line.replace('    ', '\t') for line in indentLines(makeInitFunctionPointers())]
	writeInlFile(filename, INL_HEADER, lines)

def writeFuncPtrInterfaceImpl (api, filename, functionTypes, className):
	def makeFuncPtrInterfaceImpl ():
		for function in api.functions:
			if function.getType() in functionTypes and not function.isAlias:
				yield ""
				yield "%s %s::%s (%s) const" % (function.returnType, className, getInterfaceName(function), argListToStr(function.arguments))
				yield "{"
				if function.name == "vkEnumerateInstanceVersion":
					yield "	if (m_vk.enumerateInstanceVersion)"
					yield "		return m_vk.enumerateInstanceVersion(pApiVersion);"
					yield ""
					yield "	*pApiVersion = VK_API_VERSION_1_0;"
					yield "	return VK_SUCCESS;"
				elif function.getType() == Function.TYPE_INSTANCE and function.arguments[0].getType() == "VkPhysicalDevice" and function.alias != None:
					yield "	vk::VkPhysicalDeviceProperties props;"
					yield "	m_vk.getPhysicalDeviceProperties(physicalDevice, &props);"
					yield "	if (props.apiVersion >= VK_API_VERSION_1_1)"
					yield "		%sm_vk.%s(%s);" % ("return " if function.returnType != "void" else "", getInterfaceName(function), ", ".join(a.name for a in function.arguments))
					yield "	else"
					yield "		%sm_vk.%s(%s);" % ("return " if function.returnType != "void" else "", getInterfaceName(function.alias), ", ".join(a.name for a in function.arguments))
				else:
					yield "	%sm_vk.%s(%s);" % ("return " if function.returnType != "void" else "", getInterfaceName(function), ", ".join(a.name for a in function.arguments))
				yield "}"

	writeInlFile(filename, INL_HEADER, makeFuncPtrInterfaceImpl())

def writeStrUtilProto (api, filename):
	def makeStrUtilProto ():
		for line in indentLines(["const char*\tget%sName\t(%s value);" % (enum.name[2:], enum.name) for enum in api.enums if not enum.isAlias]):
			yield line
		yield ""
		for line in indentLines(["inline tcu::Format::Enum<%s>\tget%sStr\t(%s value)\t{ return tcu::Format::Enum<%s>(get%sName, value);\t}" % (e.name, e.name[2:], e.name, e.name, e.name[2:]) for e in api.enums if not e.isAlias]):
			yield line
		yield ""
		for line in indentLines(["inline std::ostream&\toperator<<\t(std::ostream& s, %s value)\t{ return s << get%sStr(value);\t}" % (e.name, e.name[2:]) for e in api.enums if not e.isAlias]):
			yield line
		yield ""
		for line in indentLines(["tcu::Format::Bitfield<32>\tget%sStr\t(%s value);" % (bitfield.name[2:], bitfield.name) for bitfield in api.bitfields if not bitfield.isAlias]):
			yield line
		yield ""
		for line in indentLines(["std::ostream&\toperator<<\t(std::ostream& s, const %s& value);" % (s.name) for s in api.compositeTypes if not s.isAlias]):
			yield line

	writeInlFile(filename, INL_HEADER, makeStrUtilProto())

def writeStrUtilImpl (api, filename):
	def makeStrUtilImpl ():
		for line in indentLines(["template<> const char*\tgetTypeName<%s>\t(void) { return \"%s\";\t}" % (handle.name, handle.name) for handle in api.handles if not handle.isAlias]):
			yield line

		yield ""
		yield "namespace %s" % PLATFORM_TYPE_NAMESPACE
		yield "{"

		for line in indentLines("std::ostream& operator<< (std::ostream& s, %s\tv) { return s << tcu::toHex(v.internal); }" % ''.join(s) for n, s, c in PLATFORM_TYPES):
			yield line

		yield "}"

		for enum in api.enums:
			if enum.isAlias:
				continue
			yield ""
			yield "const char* get%sName (%s value)" % (enum.name[2:], enum.name)
			yield "{"
			yield "\tswitch (value)"
			yield "\t{"
			for line in indentLines(["\t\tcase %s:\treturn \"%s\";" % (n, n) for n, v in enum.values] + ["\t\tdefault:\treturn DE_NULL;"]):
				yield line
			yield "\t}"
			yield "}"

		for bitfield in api.bitfields:
			if bitfield.isAlias:
				continue
			yield ""
			yield "tcu::Format::Bitfield<32> get%sStr (%s value)" % (bitfield.name[2:], bitfield.name)
			yield "{"

			if len(bitfield.values) > 0:
				yield "\tstatic const tcu::Format::BitDesc s_desc[] ="
				yield "\t{"
				for line in indentLines(["\t\ttcu::Format::BitDesc(%s,\t\"%s\")," % (n, n) for n, v in bitfield.values]):
					yield line
				yield "\t};"
				yield "\treturn tcu::Format::Bitfield<32>(value, DE_ARRAY_BEGIN(s_desc), DE_ARRAY_END(s_desc));"
			else:
				yield "\treturn tcu::Format::Bitfield<32>(value, DE_NULL, DE_NULL);"
			yield "}"

		bitfieldTypeNames = set([bitfield.name for bitfield in api.bitfields])

		for type in api.compositeTypes:
			if not type.isAlias:
				yield ""
				yield "std::ostream& operator<< (std::ostream& s, const %s& value)" % type.name
				yield "{"
				yield "\ts << \"%s = {\\n\";" % type.name
				for member in type.members:
					memberName	= member.name
					valFmt		= None
					newLine		= ""
					if member.getType() in bitfieldTypeNames:
						valFmt = "get%sStr(value.%s)" % (member.getType()[2:], member.name)
					elif member.getType() == "const char*" or member.getType() == "char*":
						valFmt = "getCharPtrStr(value.%s)" % member.name
					elif member.arraySize != '':
						if member.name in ["extensionName", "deviceName", "layerName", "description"]:
							valFmt = "(const char*)value.%s" % member.name
						elif member.getType() == 'char' or member.getType() == 'deUint8':
							newLine = "'\\n' << "
							valFmt	= "tcu::formatArray(tcu::Format::HexIterator<%s>(DE_ARRAY_BEGIN(value.%s)), tcu::Format::HexIterator<%s>(DE_ARRAY_END(value.%s)))" % (member.getType(), member.name, member.getType(), member.name)
						else:
							if member.name == "memoryTypes" or member.name == "memoryHeaps":
								endIter = "DE_ARRAY_BEGIN(value.%s) + value.%sCount" % (member.name, member.name[:-1])
							else:
								endIter = "DE_ARRAY_END(value.%s)" % member.name
							newLine = "'\\n' << "
							valFmt	= "tcu::formatArray(DE_ARRAY_BEGIN(value.%s), %s)" % (member.name, endIter)
						memberName = member.name
					else:
						valFmt = "value.%s" % member.name
					yield ("\ts << \"\\t%s = \" << " % memberName) + newLine + valFmt + " << '\\n';"
				yield "\ts << '}';"
				yield "\treturn s;"
				yield "}"
	writeInlFile(filename, INL_HEADER, makeStrUtilImpl())

class ConstructorFunction:
	def __init__ (self, type, name, objectType, iface, arguments):
		self.type		= type
		self.name		= name
		self.objectType	= objectType
		self.iface		= iface
		self.arguments	= arguments

def getConstructorFunctions (api):
	funcs = []
	for function in api.functions:
		if function.isAlias:
			continue
		if (function.name[:8] == "vkCreate" or function.name == "vkAllocateMemory") and not "createInfoCount" in [a.name for a in function.arguments]:
			if function.name == "vkCreateDisplayModeKHR":
				continue # No way to delete display modes (bug?)

			# \todo [pyry] Rather hacky
			iface = None
			if function.getType() == Function.TYPE_PLATFORM:
				iface = Variable("const PlatformInterface&", "vk", "")
			elif function.getType() == Function.TYPE_INSTANCE:
				iface = Variable("const InstanceInterface&", "vk", "")
			else:
				iface = Variable("const DeviceInterface&", "vk", "")

			assert (function.arguments[-2].type == ["const", "VkAllocationCallbacks", "*"])

			objectType	= function.arguments[-1].type[0] #not getType() but type[0] on purpose
			arguments	= function.arguments[:-1]
			funcs.append(ConstructorFunction(function.getType(), getInterfaceName(function), objectType, iface, arguments))
	return funcs

def addVersionDefines(versionSpectrum):
	output = ["#define " + ver.getDefineName() + " " + ver.getInHex() for ver in versionSpectrum if not ver.isStandardVersion()]
	return output

def removeVersionDefines(versionSpectrum):
	output = ["#undef " + ver.getDefineName() for ver in versionSpectrum if not ver.isStandardVersion()]
	return output

def writeRefUtilProto (api, filename):
	functions = getConstructorFunctions(api)

	def makeRefUtilProto ():
		unindented = []
		for line in indentLines(["Move<%s>\t%s\t(%s = DE_NULL);" % (function.objectType, function.name, argListToStr([function.iface] + function.arguments)) for function in functions]):
			yield line

	writeInlFile(filename, INL_HEADER, makeRefUtilProto())

def writeRefUtilImpl (api, filename):
	functions = getConstructorFunctions(api)

	def makeRefUtilImpl ():
		yield "namespace refdetails"
		yield "{"
		yield ""

		for function in api.functions:
			if function.getType() == Function.TYPE_DEVICE \
			and (function.name[:9] == "vkDestroy" or function.name == "vkFreeMemory") \
			and not function.name == "vkDestroyDevice" \
			and not function.isAlias:
				objectType = function.arguments[-2].getType()
				yield "template<>"
				yield "void Deleter<%s>::operator() (%s obj) const" % (objectType, objectType)
				yield "{"
				yield "\tm_deviceIface->%s(m_device, obj, m_allocator);" % (getInterfaceName(function))
				yield "}"
				yield ""

		yield "} // refdetails"
		yield ""

		for function in functions:
			if function.type == Function.TYPE_DEVICE:
				dtorObj = "device"
			elif function.type == Function.TYPE_INSTANCE:
				if function.name == "createDevice":
					dtorObj = "object"
				else:
					dtorObj = "instance"
			else:
				dtorObj = "object"

			yield "Move<%s> %s (%s)" % (function.objectType, function.name, argListToStr([function.iface] + function.arguments))
			yield "{"
			yield "\t%s object = 0;" % function.objectType
			yield "\tVK_CHECK(vk.%s(%s));" % (function.name, ", ".join([a.name for a in function.arguments] + ["&object"]))
			yield "\treturn Move<%s>(check<%s>(object), Deleter<%s>(%s));" % (function.objectType, function.objectType, function.objectType, ", ".join(["vk", dtorObj, function.arguments[-1].name]))
			yield "}"
			yield ""

	writeInlFile(filename, INL_HEADER, makeRefUtilImpl())

def writeStructTraitsImpl (api, filename):
	def gen ():
		for type in api.compositeTypes:
			if type.getClassName() == "struct" and type.members[0].name == "sType" and not type.isAlias:
				yield "template<> VkStructureType getStructureType<%s> (void)" % type.name
				yield "{"
				yield "\treturn %s;" % prefixName("VK_STRUCTURE_TYPE_", type.name)
				yield "}"
				yield ""

	writeInlFile(filename, INL_HEADER, gen())

def writeNullDriverImpl (api, filename):
	def genNullDriverImpl ():
		specialFuncNames	= [
				"vkCreateGraphicsPipelines",
				"vkCreateComputePipelines",
				"vkGetInstanceProcAddr",
				"vkGetDeviceProcAddr",
				"vkEnumeratePhysicalDevices",
				"vkEnumerateInstanceExtensionProperties",
				"vkEnumerateDeviceExtensionProperties",
				"vkGetPhysicalDeviceFeatures",
				"vkGetPhysicalDeviceFeatures2KHR",
				"vkGetPhysicalDeviceProperties",
				"vkGetPhysicalDeviceProperties2KHR",
				"vkGetPhysicalDeviceQueueFamilyProperties",
				"vkGetPhysicalDeviceMemoryProperties",
				"vkGetPhysicalDeviceFormatProperties",
				"vkGetPhysicalDeviceImageFormatProperties",
				"vkGetDeviceQueue",
				"vkGetBufferMemoryRequirements",
				"vkGetBufferMemoryRequirements2KHR",
				"vkGetImageMemoryRequirements",
				"vkGetImageMemoryRequirements2KHR",
				"vkAllocateMemory",
				"vkMapMemory",
				"vkUnmapMemory",
				"vkAllocateDescriptorSets",
				"vkFreeDescriptorSets",
				"vkResetDescriptorPool",
				"vkAllocateCommandBuffers",
				"vkFreeCommandBuffers",
				"vkCreateDisplayModeKHR",
				"vkCreateSharedSwapchainsKHR",
				"vkGetPhysicalDeviceExternalBufferPropertiesKHR",
				"vkGetPhysicalDeviceImageFormatProperties2KHR",
				"vkGetMemoryAndroidHardwareBufferANDROID",
			]

		coreFunctions		= [f for f in api.functions if not f.isAlias]
		specialFuncs		= [f for f in coreFunctions if f.name in specialFuncNames]
		createFuncs			= [f for f in coreFunctions if (f.name[:8] == "vkCreate" or f.name == "vkAllocateMemory") and not f in specialFuncs]
		destroyFuncs		= [f for f in coreFunctions if (f.name[:9] == "vkDestroy" or f.name == "vkFreeMemory") and not f in specialFuncs]
		dummyFuncs			= [f for f in coreFunctions if f not in specialFuncs + createFuncs + destroyFuncs]

		def getHandle (name):
			for handle in api.handles:
				if handle.name == name[0]:
					return handle
			raise Exception("No such handle: %s" % name)

		for function in createFuncs:
			objectType	= function.arguments[-1].type[:-1]
			argsStr		= ", ".join([a.name for a in function.arguments[:-1]])

			yield "VKAPI_ATTR %s VKAPI_CALL %s (%s)" % (function.returnType, getInterfaceName(function), argListToStr(function.arguments))
			yield "{"
			yield "\tDE_UNREF(%s);" % function.arguments[-2].name

			if getHandle(objectType).type == Handle.TYPE_NONDISP:
				yield "\tVK_NULL_RETURN((*%s = allocateNonDispHandle<%s, %s>(%s)));" % (function.arguments[-1].name, objectType[0][2:], objectType[0], argsStr)
			else:
				yield "\tVK_NULL_RETURN((*%s = allocateHandle<%s, %s>(%s)));" % (function.arguments[-1].name, objectType[0][2:], objectType[0], argsStr)
			yield "}"
			yield ""

		for function in destroyFuncs:
			objectArg	= function.arguments[-2]

			yield "VKAPI_ATTR %s VKAPI_CALL %s (%s)" % (function.returnType, getInterfaceName(function), argListToStr(function.arguments))
			yield "{"
			for arg in function.arguments[:-2]:
				yield "\tDE_UNREF(%s);" % arg.name

			if getHandle(objectArg.type).type == Handle.TYPE_NONDISP:
				yield "\tfreeNonDispHandle<%s, %s>(%s, %s);" % (objectArg.getType()[2:], objectArg.getType(), objectArg.name, function.arguments[-1].name)
			else:
				yield "\tfreeHandle<%s, %s>(%s, %s);" % (objectArg.getType()[2:], objectArg.getType(), objectArg.name, function.arguments[-1].name)

			yield "}"
			yield ""

		for function in dummyFuncs:
			yield "VKAPI_ATTR %s VKAPI_CALL %s (%s)" % (function.returnType, getInterfaceName(function), argListToStr(function.arguments))
			yield "{"
			for arg in function.arguments:
				yield "\tDE_UNREF(%s);" % arg.name
			if function.returnType != "void":
				yield "\treturn VK_SUCCESS;"
			yield "}"
			yield ""

		def genFuncEntryTable (type, name):
			funcs = [f for f in api.functions if f.getType() == type]
			refFuncs = {}
			for f in api.functions:
				if f.alias != None:
					refFuncs[f.alias] = f

			yield "static const tcu::StaticFunctionLibrary::Entry %s[] =" % name
			yield "{"
			for line in indentLines(["\tVK_NULL_FUNC_ENTRY(%s,\t%s)," % (function.name, getInterfaceName(function if not function.isAlias else refFuncs[function])) for function in funcs]):
				yield line
			yield "};"
			yield ""

		# Func tables
		for line in genFuncEntryTable(Function.TYPE_PLATFORM, "s_platformFunctions"):
			yield line

		for line in genFuncEntryTable(Function.TYPE_INSTANCE, "s_instanceFunctions"):
			yield line

		for line in genFuncEntryTable(Function.TYPE_DEVICE, "s_deviceFunctions"):
			yield line

	writeInlFile(filename, INL_HEADER, genNullDriverImpl())

def writeTypeUtil (api, filename):
	# Structs filled by API queries are not often used in test code
	QUERY_RESULT_TYPES = set([
			"VkPhysicalDeviceFeatures",
			"VkPhysicalDeviceLimits",
			"VkFormatProperties",
			"VkImageFormatProperties",
			"VkPhysicalDeviceSparseProperties",
			"VkQueueFamilyProperties",
			"VkMemoryType",
			"VkMemoryHeap",
		])
	COMPOSITE_TYPES = set([t.name for t in api.compositeTypes if not t.isAlias])

	def isSimpleStruct (type):
		def hasArrayMember (type):
			for member in type.members:
				if member.arraySize != '':
					return True
			return False

		def hasCompositeMember (type):
			for member in type.members:
				if member.getType() in COMPOSITE_TYPES:
					return True
			return False

		return type.typeClass == CompositeType.CLASS_STRUCT and \
		type.members[0].getType() != "VkStructureType" and \
		not type.name in QUERY_RESULT_TYPES and \
		not hasArrayMember(type) and \
		not hasCompositeMember(type)

	def gen ():
		for type in api.compositeTypes:
			if not isSimpleStruct(type) or type.isAlias:
				continue

			yield ""
			yield "inline %s make%s (%s)" % (type.name, type.name[2:], argListToStr(type.members))
			yield "{"
			yield "\t%s res;" % type.name
			for line in indentLines(["\tres.%s\t= %s;" % (m.name, m.name) for m in type.members]):
				yield line
			yield "\treturn res;"
			yield "}"

	writeInlFile(filename, INL_HEADER, gen())

def writeSupportedExtenions(api, filename):

	def writeExtensionsForVersions(map):
		result = []
		for version in map:
			result.append("	if (coreVersion >= " + str(version) + ")")
			result.append("	{")
			for extension in map[version]:
				result.append('		dst.push_back("' + extension.name + '");')
			result.append("	}")

		return result

	instanceMap		= {}
	deviceMap		= {}
	versionSet		= set()

	for ext in api.extensions:
		if ext.versionInCore != None:
			if ext.versionInCore[0] == 'INSTANCE':
				list = instanceMap.get(Version(ext.versionInCore[1:]))
				instanceMap[Version(ext.versionInCore[1:])] = list + [ext] if list else [ext]
			else:
				list = deviceMap.get(Version(ext.versionInCore[1:]))
				deviceMap[Version(ext.versionInCore[1:])] = list + [ext] if list else [ext]
			versionSet.add(Version(ext.versionInCore[1:]))

	lines = addVersionDefines(versionSet) + [
	"",
	"void getCoreDeviceExtensionsImpl (deUint32 coreVersion, ::std::vector<const char*>&%s)" % (" dst" if len(deviceMap) != 0 else ""),
	"{"] + writeExtensionsForVersions(deviceMap) + [
	"}",
	"",
	"void getCoreInstanceExtensionsImpl (deUint32 coreVersion, ::std::vector<const char*>&%s)" % (" dst" if len(instanceMap) != 0 else ""),
	"{"] + writeExtensionsForVersions(instanceMap) + [
	"}",
	""] + removeVersionDefines(versionSet)
	writeInlFile(filename, INL_HEADER, lines)

def writeCoreFunctionalities(api, filename):
	functionOriginValues = ["FUNCTIONORIGIN_PLATFORM", "FUNCTIONORIGIN_INSTANCE", "FUNCTIONORIGIN_DEVICE"]

	lines = addVersionDefines([Version((1, 0, 0)), Version((1, 1, 0))]) + [
	"",
	'enum FunctionOrigin', '{'] + [line for line in indentLines([
	'\t' + functionOriginValues[0] + '\t= 0,',
	'\t' + functionOriginValues[1] + ',',
	'\t' + functionOriginValues[2]])] + [
	"};",
	"",
	"typedef ::std::pair<const char*, FunctionOrigin> FunctionInfo;",
	"typedef ::std::vector<FunctionInfo> FunctionInfosList;",
	"typedef ::std::map<deUint32, FunctionInfosList> ApisMap;",
	"",
	"void initApisMap (ApisMap& apis)",
	"{",
	"	apis.clear();",
	"	apis.insert(::std::pair<deUint32, FunctionInfosList>(" + str(Version((1, 0, 0))) + ", FunctionInfosList()));",
	"	apis.insert(::std::pair<deUint32, FunctionInfosList>(" + str(Version((1, 1, 0))) + ", FunctionInfosList()));",
	""]

	def list10Funcs ():
		for fun in api.functions:
			if fun.apiVersion == 'VK_VERSION_1_0':
				insert = '	apis[' + str(Version((1, 0, 0))) + '].push_back(FunctionInfo("' + fun.name + '",\t' + functionOriginValues[fun.getType()] + '));'
				yield insert

	def listAllFuncs ():
		for fun in api.extensions[0].functions:
			insert = '	apis[' + str(Version((1, 1, 0))) + '].push_back(FunctionInfo("' + fun.name + '",\t' + functionOriginValues[fun.getType()] + '));'
			yield insert

	lines = lines + [line for line in indentLines(list10Funcs())]
	lines.append("")
	lines = lines + [line for line in indentLines(listAllFuncs())]

	lines.append("}")
	lines.append("")
	lines = lines + removeVersionDefines([Version((1, 0, 0)), Version((1, 1, 0))])

	writeInlFile(filename, INL_HEADER, lines)

if __name__ == "__main__":
	src				= readFile(VULKAN_H)
	api				= parseAPI(src)

	platformFuncs	= [Function.TYPE_PLATFORM]
	instanceFuncs	= [Function.TYPE_INSTANCE]
	deviceFuncs		= [Function.TYPE_DEVICE]

	writeHandleType				(api, os.path.join(VULKAN_DIR, "vkHandleType.inl"))
	writeBasicTypes				(api, os.path.join(VULKAN_DIR, "vkBasicTypes.inl"))
	writeCompositeTypes			(api, os.path.join(VULKAN_DIR, "vkStructTypes.inl"))
	writeInterfaceDecl			(api, os.path.join(VULKAN_DIR, "vkVirtualPlatformInterface.inl"),		platformFuncs,	False)
	writeInterfaceDecl			(api, os.path.join(VULKAN_DIR, "vkVirtualInstanceInterface.inl"),		instanceFuncs,	False)
	writeInterfaceDecl			(api, os.path.join(VULKAN_DIR, "vkVirtualDeviceInterface.inl"),			deviceFuncs,	False)
	writeInterfaceDecl			(api, os.path.join(VULKAN_DIR, "vkConcretePlatformInterface.inl"),		platformFuncs,	True)
	writeInterfaceDecl			(api, os.path.join(VULKAN_DIR, "vkConcreteInstanceInterface.inl"),		instanceFuncs,	True)
	writeInterfaceDecl			(api, os.path.join(VULKAN_DIR, "vkConcreteDeviceInterface.inl"),		deviceFuncs,	True)
	writeFunctionPtrTypes		(api, os.path.join(VULKAN_DIR, "vkFunctionPointerTypes.inl"))
	writeFunctionPointers		(api, os.path.join(VULKAN_DIR, "vkPlatformFunctionPointers.inl"),		platformFuncs)
	writeFunctionPointers		(api, os.path.join(VULKAN_DIR, "vkInstanceFunctionPointers.inl"),		instanceFuncs)
	writeFunctionPointers		(api, os.path.join(VULKAN_DIR, "vkDeviceFunctionPointers.inl"),			deviceFuncs)
	writeInitFunctionPointers	(api, os.path.join(VULKAN_DIR, "vkInitPlatformFunctionPointers.inl"),	platformFuncs,	lambda f: f.name != "vkGetInstanceProcAddr")
	writeInitFunctionPointers	(api, os.path.join(VULKAN_DIR, "vkInitInstanceFunctionPointers.inl"),	instanceFuncs)
	writeInitFunctionPointers	(api, os.path.join(VULKAN_DIR, "vkInitDeviceFunctionPointers.inl"),		deviceFuncs)
	writeFuncPtrInterfaceImpl	(api, os.path.join(VULKAN_DIR, "vkPlatformDriverImpl.inl"),				platformFuncs,	"PlatformDriver")
	writeFuncPtrInterfaceImpl	(api, os.path.join(VULKAN_DIR, "vkInstanceDriverImpl.inl"),				instanceFuncs,	"InstanceDriver")
	writeFuncPtrInterfaceImpl	(api, os.path.join(VULKAN_DIR, "vkDeviceDriverImpl.inl"),				deviceFuncs,	"DeviceDriver")
	writeStrUtilProto			(api, os.path.join(VULKAN_DIR, "vkStrUtil.inl"))
	writeStrUtilImpl			(api, os.path.join(VULKAN_DIR, "vkStrUtilImpl.inl"))
	writeRefUtilProto			(api, os.path.join(VULKAN_DIR, "vkRefUtil.inl"))
	writeRefUtilImpl			(api, os.path.join(VULKAN_DIR, "vkRefUtilImpl.inl"))
	writeStructTraitsImpl		(api, os.path.join(VULKAN_DIR, "vkGetStructureTypeImpl.inl"))
	writeNullDriverImpl			(api, os.path.join(VULKAN_DIR, "vkNullDriverImpl.inl"))
	writeTypeUtil				(api, os.path.join(VULKAN_DIR, "vkTypeUtil.inl"))
	writeSupportedExtenions		(api, os.path.join(VULKAN_DIR, "vkSupportedExtensions.inl"))
	writeCoreFunctionalities	(api, os.path.join(VULKAN_DIR, "vkCoreFunctionalities.inl"))
