package io.armory.plugin.kubernetes.handlers

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import dev.minutest.test
import org.springframework.test.context.TestContextManager
import strikt.api.expectThat
import strikt.assertions.isNotNull

class OperatorCRDHandlerIntegrationTest : JUnit5Minutests {
    fun tests() = rootContext<OperatorCRDHandlerTestFixture> {
        fixture {
            OperatorCRDHandlerTestFixture().also {
                TestContextManager(OperatorCRDHandlerTestFixture::class.java).prepareTestInstance(it)
            }
        }

        test("can fetch handler from application context") {
            expectThat(handlers.find { it.kind().toString() == "SpinnakerService.spinnaker.armory.io" }).isNotNull()
        }
    }
}
