package io.armory.plugin.kubernetes

import com.netflix.spinnaker.kork.plugins.api.spring.PrivilegedSpringPlugin
import org.slf4j.LoggerFactory
import org.pf4j.PluginWrapper
import org.springframework.beans.factory.support.BeanDefinitionRegistry

class CRDCheckPlugin(wrapper: PluginWrapper) : PrivilegedSpringPlugin(wrapper) {

    override fun registerBeanDefinitions(registry: BeanDefinitionRegistry) {
        registerBean(primaryBeanDefinitionFor(OperatorCRDHandler::class.java), registry)
    }

    private val logger = LoggerFactory.getLogger(CRDCheckPlugin::class.java)

    override fun start() {
        logger.info("CRDCheckPlugin.start()")
    }

    override fun stop() {
        logger.info("CRDCheckPlugin.stop()")
    }
}