package io.armory.plugin.kubernetes.handlers

import com.netflix.spinnaker.kork.plugins.tck.serviceFixture
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isNotNull

class OperatorCRDHandlerIntegrationTest : JUnit5Minutests {
    fun tests() = rootContext<OperatorCRDHandlerTestFixture> {
        serviceFixture {
            OperatorCRDHandlerTestFixture()
        }

        test("can fetch handler from application context") {
            expectThat(handlers.find { it.kind().toString() == "SpinnakerService.spinnaker.armory.io" }).isNotNull()
        }
    }
}
