package io.armory.plugin.kubernetes

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.netflix.spinnaker.clouddriver.kubernetes.description.SpinnakerKind
import com.netflix.spinnaker.clouddriver.kubernetes.v2.caching.agent.CustomKubernetesCachingAgentFactory
import com.netflix.spinnaker.clouddriver.kubernetes.v2.caching.agent.KubernetesV2CachingAgentFactory
import com.netflix.spinnaker.clouddriver.kubernetes.v2.description.manifest.KubernetesApiGroup
import com.netflix.spinnaker.clouddriver.kubernetes.v2.description.manifest.KubernetesKind
import com.netflix.spinnaker.clouddriver.kubernetes.v2.description.manifest.KubernetesManifest
import com.netflix.spinnaker.clouddriver.kubernetes.v2.model.Manifest
import com.netflix.spinnaker.clouddriver.kubernetes.v2.op.handler.KubernetesHandler
import org.slf4j.LoggerFactory

class OperatorCRDHandler : KubernetesHandler() {

    private val logger = LoggerFactory.getLogger(OperatorCRDHandler::class.java)
    private val mapper = jacksonObjectMapper()

    init {
        logger.info("OperatorCRDHandler being initialized...")
    }

    override fun status(manifest: KubernetesManifest?): Manifest.Status {
        logger.info("manifest is " + manifest.toString())
        return manifest.status()?.let {
            if (it.status == "OK") {
                Manifest.Status().apply {
                    stable("This manifest is stable!")
                }
            } else {
                Manifest.Status().apply {
                    unstable("This manifest is not stable!")
                }
            }
        } ?: Manifest.Status.noneReported()
    }

    override fun kind(): KubernetesKind {
        return KubernetesKind.from("SpinnakerService", KubernetesApiGroup.fromString("spinnaker.io"))
    }

    override fun versioned(): Boolean {
        return false
    }

    override fun spinnakerKind(): SpinnakerKind {
        return SpinnakerKind.UNCLASSIFIED
    }

    override fun deployPriority(): Int {
        return DeployPriority.LOWEST_PRIORITY.value
    }

    override fun cachingAgentFactory(): KubernetesV2CachingAgentFactory {
        return KubernetesV2CachingAgentFactory { creds, mapper, registry, agentIndex, agentCount, agentInterval ->
            CustomKubernetesCachingAgentFactory.create(kind(), creds, mapper, registry, agentIndex, agentCount, agentInterval)
        }
    }

    private fun KubernetesManifest?.status(): OperatorCRDStatus? {
        return this?.get("status")?.let {
            mapper.convertValue(it)
        }
    }

    private data class OperatorCRDStatus(
            val accountCount: Int,
            val apiUrl: String,
            val lastDeployed: Config,
            val serviceCount: Int,
            val services: List<Service>,
            val status: String,
            val uiUrl: String,
            val version: String
    )

    private data class Config(
            val config: LastDeployed,
            val kustomize: LastDeployed
    )

    private data class LastDeployed(
            val hash: String,
            val lastUpdatedAt: String
    )

    private data class Service(
            val image: String,
            val name: String,
            val readyReplicas: Int,
            val replicas: Int
    )
}

