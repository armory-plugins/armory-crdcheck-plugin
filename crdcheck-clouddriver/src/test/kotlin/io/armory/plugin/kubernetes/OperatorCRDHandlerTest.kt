package io.armory.plugin.kubernetes

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class OperatorCRDHandlerTest: JUnit5Minutests {

    fun tests() = rootContext {
        context("Execute stage") {
            test("execute fetch artifact stage") {
                expectThat("mock string").isEqualTo("mock string")
            }
        }
    }

}