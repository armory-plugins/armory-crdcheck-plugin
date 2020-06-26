package io.armory.plugin.kubernetes

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("armory.crd-check")
data class PluginConfig (
        var kind: String? = null,
        var apiGroup: String? = null
)