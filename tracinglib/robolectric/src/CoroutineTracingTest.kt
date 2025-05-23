/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.test.tracing.coroutines

import android.platform.test.annotations.EnableFlags
import com.android.app.tracing.coroutines.coroutineScopeTraced
import com.android.app.tracing.coroutines.createCoroutineTracingContext
import com.android.app.tracing.coroutines.launchTraced
import com.android.app.tracing.coroutines.nameCoroutine
import com.android.app.tracing.coroutines.traceCoroutine
import com.android.systemui.Flags.FLAG_COROUTINE_TRACING
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test

@EnableFlags(FLAG_COROUTINE_TRACING)
class CoroutineTracingTest : TestBase() {

    override val extraCoroutineContext: CoroutineContext
        get() = createCoroutineTracingContext("main", includeParentNames = true, strictMode = true)

    @Test
    fun simpleTraceSection() = runTest {
        expectD(1, "main:1^")
        traceCoroutine("hello") { expectD(2, "main:1^", "hello") }
        finish(3, "main:1^")
    }

    @Test
    fun simpleNestedTraceSection() = runTest {
        expectD(1, "main:1^")
        traceCoroutine("hello") {
            expectD(2, "main:1^", "hello")
            traceCoroutine("world") { expectD(3, "main:1^", "hello", "world") }
            expectD(4, "main:1^", "hello")
        }
        finish(5, "main:1^")
    }

    @Test
    fun simpleLaunch() = runTest {
        expectD(1, "main:1^")
        traceCoroutine("hello") {
            expectD(2, "main:1^", "hello")
            launch {
                // "hello" is not passed to child scope
                finish(4, "main:1^:1^")
            }
        }
        expect(3, "main:1^")
    }

    @Test
    fun launchWithSuspendingLambda() = runTest {
        val fetchData: suspend () -> String = {
            expect(3, "main:1^:1^span-for-launch")
            delay(1L)
            traceCoroutine("span-for-fetchData") {
                expect(4, "main:1^:1^span-for-launch", "span-for-fetchData")
            }
            "stuff"
        }
        expect(1, "main:1^")
        launchTraced("span-for-launch") {
            assertEquals("stuff", fetchData())
            finish(5, "main:1^:1^span-for-launch")
        }
        expect(2, "main:1^")
    }

    @Test
    fun launchInCoroutineScope() = runTest {
        launchTraced("launch#0") {
            expect("main:1^:1^launch#0")
            delay(1)
            expect("main:1^:1^launch#0")
        }
        coroutineScopeTraced("span-for-coroutineScope-1") {
            launchTraced("launch#1") {
                expect("main:1^:2^launch#1")
                delay(1)
                expect("main:1^:2^launch#1")
            }
            launchTraced("launch#2") {
                expect("main:1^:3^launch#2")
                delay(1)
                expect("main:1^:3^launch#2")
            }
            coroutineScopeTraced("span-for-coroutineScope-2") {
                launchTraced("launch#3") {
                    expect("main:1^:4^launch#3")
                    delay(1)
                    expect("main:1^:4^launch#3")
                }
                launchTraced("launch#4") {
                    expect("main:1^:5^launch#4")
                    delay(1)
                    expect("main:1^:5^launch#4")
                }
            }
        }
        launchTraced("launch#5") {
            expect("main:1^:6^launch#5")
            delay(1)
            expect("main:1^:6^launch#5")
        }
    }

    @Test
    fun namedScopeMerging() = runTest {
        // to avoid race conditions in the test leading to flakes, avoid calling expectD() or
        // delaying before launching (e.g. only call expectD() in leaf blocks)
        expect("main:1^")
        launchTraced("A") {
            expect("main:1^:1^A")
            traceCoroutine("span") { expectD("main:1^:1^A", "span") }
            launchTraced("B") { expectD("main:1^:1^A:1^B") }
            launchTraced("C") {
                expect("main:1^:1^A:2^C")
                launch { expectD("main:1^:1^A:2^C:1^") }
                launchTraced("D") { expectD("main:1^:1^A:2^C:2^D") }
                launchTraced("E") {
                    expect("main:1^:1^A:2^C:3^E")
                    launchTraced("F") { expectD("main:1^:1^A:2^C:3^E:1^F") }
                    expect("main:1^:1^A:2^C:3^E")
                }
            }
            launchTraced("G") { expectD("main:1^:1^A:3^G") }
        }
        launch { launch { launch { expectD("main:1^:2^:1^:1^") } } }
        delay(2)
        launchTraced("H") { launch { launch { expectD("main:1^:3^H:1^:1^") } } }
        delay(2)
        launch {
            launch {
                launch {
                    launch {
                        launch { launchTraced("I") { expectD("main:1^:4^:1^:1^:1^:1^:1^I") } }
                    }
                }
            }
        }
        delay(2)
        launchTraced("J") {
            launchTraced("K") { launch { launch { expectD("main:1^:5^J:1^K:1^:1^") } } }
        }
        delay(2)
        launchTraced("L") {
            launchTraced("M") { launch { launch { expectD("main:1^:6^L:1^M:1^:1^") } } }
        }
        delay(2)
        launchTraced("N") {
            launchTraced("O") { launch { launchTraced("D") { expectD("main:1^:7^N:1^O:1^:1^D") } } }
        }
        delay(2)
        launchTraced("P") {
            launchTraced("Q") { launch { launchTraced("R") { expectD("main:1^:8^P:1^Q:1^:1^R") } } }
        }
        delay(2)
        launchTraced("S") { launchTraced("T") { launch { expectD("main:1^:9^S:1^T:1^") } } }
        delay(2)
        launchTraced("U") { launchTraced("V") { launch { expectD("main:1^:10^U:1^V:1^") } } }
        delay(2)
        expectD("main:1^")
    }

    @Test
    fun launchIntoSelf() = runTest {
        expectD("main:1^")
        val reusedNameContext = nameCoroutine("my-coroutine")
        launch(reusedNameContext) {
            expectD("main:1^:1^my-coroutine")
            launch(reusedNameContext) { expectD("main:1^:1^my-coroutine:1^my-coroutine") }
            expectD("main:1^:1^my-coroutine")
            launch(reusedNameContext) { expectD("main:1^:1^my-coroutine:2^my-coroutine") }
            expectD("main:1^:1^my-coroutine")
        }
        launch(reusedNameContext) { expectD("main:1^:2^my-coroutine") }
        expectD("main:1^")
    }
}
