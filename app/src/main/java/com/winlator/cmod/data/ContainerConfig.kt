package com.winlator.cmod.data

data class GraphicsDriverConfig(
    val vulkanVersion: String = "1.3",
    val version: String = "",
    val blacklistedExtensions: String = "",
    val maxDeviceMemory: String = "0",
    val presentMode: String = "mailbox",
    val syncFrame: String = "0",
    val disablePresentWait: String = "0",
    val resourceType: String = "auto",
    val bcnEmulation: String = "auto",
    val bcnEmulationType: String = "software",
    val bcnEmulationCache: String = "0"
) {
    companion object {
        fun parse(config: String): GraphicsDriverConfig {
            val map = config.split(";").associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }
            return GraphicsDriverConfig(
                vulkanVersion = map["vulkanVersion"] ?: "1.3",
                version = map["version"] ?: "",
                blacklistedExtensions = map["blacklistedExtensions"] ?: "",
                maxDeviceMemory = map["maxDeviceMemory"] ?: "0",
                presentMode = map["presentMode"] ?: "mailbox",
                syncFrame = map["syncFrame"] ?: "0",
                disablePresentWait = map["disablePresentWait"] ?: "0",
                resourceType = map["resourceType"] ?: "auto",
                bcnEmulation = map["bcnEmulation"] ?: "auto",
                bcnEmulationType = map["bcnEmulationType"] ?: "software",
                bcnEmulationCache = map["bcnEmulationCache"] ?: "0"
            )
        }
    }

    fun toConfigString(): String {
        return "vulkanVersion=$vulkanVersion;version=$version;blacklistedExtensions=$blacklistedExtensions;" +
                "maxDeviceMemory=$maxDeviceMemory;presentMode=$presentMode;syncFrame=$syncFrame;" +
                "disablePresentWait=$disablePresentWait;resourceType=$resourceType;bcnEmulation=$bcnEmulation;" +
                "bcnEmulationType=$bcnEmulationType;bcnEmulationCache=$bcnEmulationCache"
    }
}

data class DXWrapperConfig(
    val version: String = "2.3.1",
    val framerate: String = "0",
    val async: String = "0",
    val asyncCache: String = "0",
    val vkd3dVersion: String = "None",
    val vkd3dLevel: String = "12_1",
    val ddrawrapper: String = "none"
) {
    companion object {
        fun parse(config: String): DXWrapperConfig {
            val map = config.split(",").associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }
            return DXWrapperConfig(
                version = map["version"] ?: "2.3.1",
                framerate = map["framerate"] ?: "0",
                async = map["async"] ?: "0",
                asyncCache = map["asyncCache"] ?: "0",
                vkd3dVersion = map["vkd3dVersion"] ?: "None",
                vkd3dLevel = map["vkd3dLevel"] ?: "12_1",
                ddrawrapper = map["ddrawrapper"] ?: "none"
            )
        }
    }

    fun toConfigString(): String {
        return "version=$version,framerate=$framerate,async=$async,asyncCache=$asyncCache," +
                "vkd3dVersion=$vkd3dVersion,vkd3dLevel=$vkd3dLevel,ddrawrapper=$ddrawrapper"
    }
}
