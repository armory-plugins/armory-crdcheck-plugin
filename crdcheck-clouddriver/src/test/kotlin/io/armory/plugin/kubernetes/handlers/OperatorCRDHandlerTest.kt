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

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.clouddriver.kubernetes.caching.agent.Front50ApplicationLoader
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties
import com.netflix.spinnaker.clouddriver.kubernetes.description.KubernetesSpinnakerKindMap
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesApiGroup
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesApiVersion
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesKind
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesManifest
import com.netflix.spinnaker.clouddriver.kubernetes.model.Manifest
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesNamedAccountCredentials
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.mockk
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess

class OperatorCRDHandlerTest: JUnit5Minutests {

    fun tests() = rootContext<Fixture> {
        fixture {
            Fixture()
        }

        val stableManifest = OperatorCRDStatus(
                1,
                "test-url",
                mapOf("config" to LastDeployed("test-hash", "test-date"), "kustomize" to LastDeployed("test-hash", "test-date")),
                1,
                listOf(Service("test-image", "test-name", 5, 5)),
                "OK",
                "test-uiurl",
                "test-version"
        )

        val unstableManifest = OperatorCRDStatus(
                1,
                "test-url",
                mapOf("config" to LastDeployed("test-hash", "test-date"), "kustomize" to LastDeployed("test-hash", "test-date")),
                1,
                listOf(Service("test-image", "test-name", 5, 5)),
                "ERROR",
                "test-uiurl",
                "test-version"
        )

        val unstableManifestWithoutAllReplicas = OperatorCRDStatus(
                1,
                "test-url",
                mapOf("config" to LastDeployed("test-hash", "test-date"), "kustomize" to LastDeployed("test-hash", "test-date")),
                1,
                listOf(Service("test-image", "test-name", 2, 5)),
                "OK",
                "test-uiurl",
                "test-version"
        )

        test("marks a CRD manifest with status status=OK and replicas=readyReplicas as stable") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                kind = KubernetesKind.from("SpinnakerService", KubernetesApiGroup.fromString("spinnaker.io"))
                put("status", stableManifest)
            }

            expectThat(subject.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isEqualTo(true)
            }
        }

        test("marks a CRD manifest with status status=ERROR and replicas=readyReplicas as unstable") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                kind = KubernetesKind.from("SpinnakerService", KubernetesApiGroup.fromString("spinnaker.io"))
                put("status", unstableManifest)
            }

            expectThat(subject.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isEqualTo(false)
            }
        }

        test("marks a CRD manifest with status status=OK and replicas!=readyReplicas as unstable") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                kind = KubernetesKind.from("SpinnakerService", KubernetesApiGroup.fromString("spinnaker.io"))
                put("status", unstableManifestWithoutAllReplicas)
            }

            expectThat(subject.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isEqualTo(false)
            }
        }

        test("marks a CRD manifest without a status as unstable") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                kind = KubernetesKind.from("SpinnakerService", KubernetesApiGroup.fromString("spinnaker.io"))
            }

            expectThat(subject.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isEqualTo(false)
            }
        }

        test("builds a caching agent factory") {
            expectCatching {
                subject.buildCachingAgent(
                        creds, mapper, registry, 1, 1, 1L,
                        configurationProperties, kubernetesSpinnakerKindMap, front50ApplicationLoader
                )
            }
                    .isSuccess()
        }
    }

    private class Fixture {
        val subject = OperatorCRDHandler()
        val creds: KubernetesNamedAccountCredentials = mockk(relaxed = true)
        val mapper: ObjectMapper = mockk(relaxed = true)
        val registry: Registry = mockk(relaxed = true)
        val configurationProperties: KubernetesConfigurationProperties = mockk(relaxed = true)
        val kubernetesSpinnakerKindMap: KubernetesSpinnakerKindMap = mockk(relaxed = true)
        val front50ApplicationLoader: Front50ApplicationLoader = mockk(relaxed = true)
    }

    private data class OperatorCRDStatus(
            val accountCount: Int,
            val apiUrl: String,
            val lastDeployed: Map<String, LastDeployed>,
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