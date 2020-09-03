package io.armory.plugin.kubernetes

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.kubernetes.v2.description.manifest.KubernetesApiGroup
import com.netflix.spinnaker.clouddriver.kubernetes.v2.description.manifest.KubernetesApiVersion
import com.netflix.spinnaker.clouddriver.kubernetes.v2.description.manifest.KubernetesKind
import com.netflix.spinnaker.clouddriver.kubernetes.v2.description.manifest.KubernetesManifest
import com.netflix.spinnaker.clouddriver.kubernetes.v2.model.Manifest
import com.netflix.spinnaker.clouddriver.kubernetes.v2.security.KubernetesV2Credentials
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.armory.plugin.kubernetes.handlers.OperatorCRDHandler
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
                listOf(Service("test-image", "test-name", 5,5)),
                "OK",
                "test-uiurl",
                "test-version"
                )

        val unstableManifest = OperatorCRDStatus(
                1,
                "test-url",
                mapOf("config" to LastDeployed("test-hash", "test-date"), "kustomize" to LastDeployed("test-hash", "test-date")),
                1,
                listOf(Service("test-image", "test-name", 5,5)),
                "ERROR",
                "test-uiurl",
                "test-version"
        )

        val unstableManifestWithoutAllReplicas = OperatorCRDStatus(
                1,
                "test-url",
                mapOf("config" to LastDeployed("test-hash", "test-date"), "kustomize" to LastDeployed("test-hash", "test-date")),
                1,
                listOf(Service("test-image", "test-name", 2,5)),
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
            expectCatching { subject.buildCachingAgent(creds, mapper, registry, 1, 1, 1L) }
                    .isSuccess()
        }
    }

    private class Fixture {
        val subject = OperatorCRDHandler()
        val creds: KubernetesNamedAccountCredentials<KubernetesV2Credentials> = mockk(relaxed = true)
        val mapper: ObjectMapper = mockk(relaxed = true)
        val registry: Registry = mockk(relaxed = true)
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