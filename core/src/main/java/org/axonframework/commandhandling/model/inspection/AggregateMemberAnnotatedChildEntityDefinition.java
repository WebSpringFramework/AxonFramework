/*
 * Copyright (c) 2010-2017. Axon Framework
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.commandhandling.model.inspection;

import org.axonframework.commandhandling.model.AggregateMember;
import org.axonframework.commandhandling.model.ForwardingMode;
import org.axonframework.common.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.axonframework.common.annotation.AnnotationUtils.findAnnotationAttributes;

/**
 * Implementation of a {@link ChildEntityDefinition} that is used to detect single entities annotated with
 * {@link AggregateMember}. If such a field is found a {@link ChildEntity} is created that delegates to the entity.
 */
public class AggregateMemberAnnotatedChildEntityDefinition implements ChildEntityDefinition {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<ChildEntity<T>> createChildDefinition(Field field, EntityModel<T> declaringEntity) {
        Map<String, Object> attributes = findAnnotationAttributes(field, AggregateMember.class).orElse(null);
        if (attributes == null
                || Iterable.class.isAssignableFrom(field.getType())
                || Map.class.isAssignableFrom(field.getType())) {
            return Optional.empty();
        }

        EntityModel entityModel = declaringEntity.modelOf(field.getType());

        ForwardingMode eventRoutingMode = eventRoutingMode((Boolean) attributes.get("forwardEvents"),
                                                           (ForwardingMode) attributes.get("eventRoutingMode"));
        return Optional.of(new AnnotatedChildEntity<>(
                entityModel,
                (Boolean) attributes.get("forwardCommands"),
                eventRoutingMode,
                (String) attributes.get("eventRoutingKey"),
                (msg, parent) -> ReflectionUtils.getFieldValue(field, parent),
                (msg, parent) -> {
                    Object fieldVal = ReflectionUtils.getFieldValue(field, parent);
                    return fieldVal == null ? emptyList() : singleton(fieldVal);
                }
        ));
    }

    private ForwardingMode eventRoutingMode(Boolean forwardEvents, ForwardingMode eventRoutingMode) {
        return !forwardEvents ? ForwardingMode.NONE : eventRoutingMode;
    }
}
