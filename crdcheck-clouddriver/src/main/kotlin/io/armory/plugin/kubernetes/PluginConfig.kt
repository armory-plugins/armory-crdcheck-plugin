package io.armory.plugin.kubernetes

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("spinnaker.extensibility.plugins.armory.CRDCheck.config")
data class PluginConfig (
        val kind: String,
        val apiGroup: String
)