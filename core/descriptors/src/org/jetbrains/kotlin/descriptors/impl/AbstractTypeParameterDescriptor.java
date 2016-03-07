/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.impl;

import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.TypeIntersectionScope;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTypeParameterDescriptor extends AbstractClassifierDescriptor implements TypeParameterDescriptor {
    private final Variance variance;
    private final boolean reified;
    private final int index;

    private final NotNullLazyValue<TypeConstructor> typeConstructor;
    private final NotNullLazyValue<KotlinType> defaultType;
    private final DeclarationDescriptor containingDeclaration;
    private final Annotations annotations;
    private final SourceElement source;

    protected AbstractTypeParameterDescriptor(
            @NotNull final StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull final Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index,
            @NotNull SourceElement source
    ) {
        super(storageManager, name);
        this.variance = variance;
        this.reified = isReified;
        this.index = index;
        this.containingDeclaration = containingDeclaration;
        this.annotations = annotations;
        this.source = source;

        this.typeConstructor = storageManager.createLazyValue(new Function0<TypeConstructor>() {
            @Override
            public TypeConstructor invoke() {
                return createTypeConstructor();
            }
        });
        this.defaultType = storageManager.createLazyValue(new Function0<KotlinType>() {
            @Override
            public KotlinType invoke() {
                return KotlinTypeImpl.create(
                        Annotations.Companion.getEMPTY(),
                        getTypeConstructor(), false, Collections.<TypeProjection>emptyList(),
                        new LazyScopeAdapter(storageManager.createLazyValue(
                                new Function0<MemberScope>() {
                                    @Override
                                    public MemberScope invoke() {
                                        return TypeIntersectionScope.create("Scope for type parameter " + name.asString(), getUpperBounds());
                                    }
                                }
                        ))
                );
            }
        });
    }

    @Override
    @NotNull
    protected abstract SupertypeLoopChecker getSupertypeLoopChecker();
    @Override
    protected abstract void reportCycleError(@NotNull KotlinType type);

    @NotNull
    @Override
    protected Collection<KotlinType> resolveSupertypes() {
        return resolveUpperBounds();
    }

    @Nullable
    @Override
    protected KotlinType defaultSupertypeIfEmpty() {
        return ErrorUtils.createErrorType("Cyclic upper bounds");
    }

    @NotNull
    protected abstract List<KotlinType> resolveUpperBounds();

    @NotNull
    protected abstract TypeConstructor createTypeConstructor();

    @NotNull
    @Override
    public Variance getVariance() {
        return variance;
    }

    @Override
    public boolean isReified() {
        return reified;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean isCapturedFromOuterDeclaration() {
        return false;
    }

    @NotNull
    @Override
    public List<KotlinType> getUpperBounds() {
        return getSupertypesWithoutCycles();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor.invoke();
    }

    @NotNull
    @Override
    public KotlinType getDefaultType() {
        return defaultType.invoke();
    }

    @NotNull
    @Override
    @Deprecated
    public TypeParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException("Don't call substitute() on type parameters");
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterDescriptor(this, data);
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return annotations;
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitTypeParameterDescriptor(this, null);
    }

    @NotNull
    @Override
    public DeclarationDescriptorWithSource getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public SourceElement getSource() {
        return source;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }
}
