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
package com.example.tracing.demo.experiments

import com.example.tracing.demo.FixedThreadB
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn

@Singleton
class CancellableSharedFlow
@Inject
constructor(@FixedThreadB private var dispatcherB: CoroutineDispatcher) : Experiment {

    override val description: String = "Create shared flows that can be cancelled by the parent"

    override suspend fun start() {
        // GOOD - launched into child scope, parent can cancel this
        coroutineScope {
            coldCounterFlow("good")
                .flowOn(dispatcherB)
                .shareIn(this, SharingStarted.Eagerly, replay = 10)
        }
    }
}
