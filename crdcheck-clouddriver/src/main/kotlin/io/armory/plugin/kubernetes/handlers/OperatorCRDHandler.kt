/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.armory.plugin.kubernetes.handlers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.netflix.spinnaker.clouddriver.kubernetes.caching.agent.CustomKubernetesCachingAgentFactory
import com.netflix.spinnaker.clouddriver.kubernetes.caching.agent.KubernetesCachingAgentFactory
import com.netflix.spinnaker.clouddriver.kubernetes.description.SpinnakerKind
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesApiGroup
import com.netflix.spinnaker.clouddriver.kubernetes.op.handler.KubernetesHandler
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesKind
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesManifest
import com.netflix.spinnaker.clouddriver.kubernetes.model.Manifest
import org.slf4j.LoggerFactory

class OperatorCRDHandler(): KubernetesHandler() {

    private val logger = LoggerFactory.getLogger(OperatorCRDHandler::class.java)
    private val mapper = jacksonObjectMapper()
    private val kind = "SpinnakerService"
    private val apiGroup = "spinnaker.armory.io"

    init {
        logger.info("OperatorCRDHandler being initialized...")
    }

    override fun status(manifest: KubernetesManifest?): Manifest.Status {
        return manifest.status()?.let {
            val servicesLoading = it.services.filter { elem -> elem.readyReplicas != elem.replicas }
            if (it.status == "OK" && servicesLoading.isEmpty()) {
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
        return KubernetesKind.from(kind, KubernetesApiGroup.fromString(apiGroup))
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

    override fun cachingAgentFactory(): KubernetesCachingAgentFactory {
        return KubernetesCachingAgentFactory { creds, mapper, registry, agentIndex, agentCount, agentInterval, agentConfig, agentMap ->
            CustomKubernetesCachingAgentFactory.create(kind(), creds, mapper, registry, agentIndex, agentCount, agentInterval, agentConfig, agentMap)
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
            val lastDeployed: Map<String,LastDeployed>,
            val serviceCount: Int,
            val services: List<Service>,
            val status: String,
            val uiUrl: String,
            val version: String
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

