/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.utils.addIfNotNull


abstract class AbstractClassifierDescriptor(
        private val storageManager: StorageManager,
        private val name_: Name
) : ClassifierDescriptor {

    override fun getName() = name_

    protected val supertypesWithoutCycles: List<KotlinType> get() = supertypes().supertypesWithoutCycles

    private class Supertypes(
            val allSupertypes: Collection<KotlinType>,
            customStorage: MutableList<KotlinType>? = null) {
        val supertypesWithoutCycles: MutableList<KotlinType> = customStorage ?: allSupertypes.toMutableList()
    }

    private val supertypes = storageManager.createLazyValueWithPostCompute(
            {
                val resolvedSupertypes = resolveSupertypes()
                Supertypes(resolvedSupertypes, customResultingSupertypesStorage())
            },
            { Supertypes(listOf(ErrorUtils.createErrorType("Loop in supertypes"))) },
            { supertypes ->
                supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
                        typeConstructor, supertypes.supertypesWithoutCycles,
                        { it.computeNeighbours() },
                        { reportCycleError(it) })

                if (supertypes.supertypesWithoutCycles.isEmpty()) {
                    supertypes.supertypesWithoutCycles.addIfNotNull(defaultSupertypeIfEmpty())
                }
            })

    protected abstract fun resolveSupertypes(): Collection<KotlinType>
    protected open val supertypeLoopChecker: SupertypeLoopChecker get() = SupertypeLoopChecker.EMPTY
    protected open fun reportCycleError(type: KotlinType) {}
    protected open fun getAdditionalNeighbours(): Collection<KotlinType> = emptyList()
    protected open fun defaultSupertypeIfEmpty(): KotlinType? = null
    protected open fun customResultingSupertypesStorage(): MutableList<KotlinType>? = null

    companion object {
        fun TypeConstructor.computeNeighbours(): Collection<KotlinType> {
            val descriptor = declarationDescriptor
            return (descriptor as? AbstractClassifierDescriptor)?.let {
                abstractClassifierDescriptor ->
                abstractClassifierDescriptor.supertypes().allSupertypes +
                    abstractClassifierDescriptor.getAdditionalNeighbours()
            } ?: supertypes
        }
    }
}
