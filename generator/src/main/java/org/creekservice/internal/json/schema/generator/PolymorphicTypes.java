/*
 * Copyright 2022 Creek Contributors (https://github.com/creek-service)
 *
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

package org.creekservice.internal.json.schema.generator;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper for finding subtypes of any polymorphic type annotated with {@code @JsonTypeInfo(use =
 * TYPE)}.
 *
 * <p>Polymorphic types are found by walking the object and property graph using Jackson.
 */
final class PolymorphicTypes {

    static Collection<PolymorphicType<?>> findPolymorphicTypes(
            final Collection<Class<?>> types,
            final Collection<String> allowedPackages,
            final ObjectMapper objectMapper) {
        return new PolymorphicTypes(allowedPackages, objectMapper).findPolymorphicTypes(types);
    }

    static final class PolymorphicType<T> {

        private final Class<T> type;
        private final Set<Class<? extends T>> subTypes;

        PolymorphicType(
                final Class<T> type, final Collection<Class<? extends T>> subTypes) {
            this.type = requireNonNull(type, "type");
            this.subTypes = Set.copyOf(requireNonNull(subTypes, "subTypes"));
        }

        public Class<T> type() {
            return type;
        }

        public Set<Class<? extends T>> subTypes() {
            return subTypes;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final PolymorphicType<?> that = (PolymorphicType<?>) o;
            return Objects.equals(type, that.type) && Objects.equals(subTypes, that.subTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, subTypes);
        }

        @Override
        public String toString() {
            return "PolymorphicType{" + "type=" + type + ", subTypes=" + subTypes + '}';
        }
    }

    private final ObjectMapper objectMapper;
    private final Set<String> allowedPackages;
    private final Map<Class<?>, PolymorphicType<?>> found = new HashMap<>();

    private PolymorphicTypes(
            final Collection<String> allowedPackages, final ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.allowedPackages = Set.copyOf(requireNonNull(allowedPackages, "allowedPackages"));
    }

    private Collection<PolymorphicType<?>> findPolymorphicTypes(final Collection<Class<?>> types) {
        for (final Class<?> type : types) {
            findPolymorphicTypes(type);
        }

        found.entrySet().removeIf(e -> e.getValue().subTypes().isEmpty());
        return found.values();
    }

    private void findPolymorphicTypes(final Class<?> type) {
        try {
            visitType(type);
        } catch (Exception e) {
            throw new PolymorphicExtractorException(type, e);
        }
    }

    private void visitType(final Class<?> type) throws JsonMappingException {
        visitType(objectMapper.constructType(type));
    }

    private void visitType(final JavaType type) throws JsonMappingException {
        objectMapper.acceptJsonFormatVisitor(type, new FormatVisitor());
    }

    private void process(final JavaType type) throws JsonMappingException {
        extractPolyInfo(type.getRawClass());

        for (int i = 0; i < type.containedTypeCount(); i++) {
            final JavaType containedType = type.containedType(i);
            visitType(containedType);
        }
    }

    private <T> void extractPolyInfo(final Class<T> type) throws JsonMappingException {
        if (found.containsKey(type)) {
            return;
        }

        final PolymorphicType<T> poly = new PolymorphicType<>(type, implementationsOf(type));
        found.put(type, poly);

        // Visit subtypes:
        for (final Class<? extends T> subType : poly.subTypes()) {
            // Optimisation: subtypes already found for parent, which is super set of sub type:
            found.put(subType, new PolymorphicType<>(subType, Set.of()));

            visitType(subType);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<Class<? extends T>> implementationsOf(final Class<T> type) {
        final JsonTypeInfo typeInfo = type.getAnnotation(JsonTypeInfo.class);
        if (typeInfo == null || typeInfo.use() != JsonTypeInfo.Id.NAME) {
            // Not using registered types
            return List.of();
        }

        final JsonSubTypes subTypes = type.getAnnotation(JsonSubTypes.class);
        if (subTypes != null) {
            // Using hard-coded subtypes.
            return List.of();
        }

        try (ScanResult sr =
                new ClassGraph()
                        .ignoreClassVisibility()
                        .enableClassInfo()
                        .acceptPackages(allowedPackages.toArray(String[]::new))
                        .scan()) {

            final ClassInfoList found =
                    type.isInterface()
                            ? sr.getClassesImplementing(type.getName())
                            : sr.getSubclasses(type.getName());

            return (List) found.stream().map(ClassInfo::loadClass).collect(Collectors.toList());
        }
    }

    private class FormatVisitor extends JsonFormatVisitorWrapper.Base {

        @Override
        public JsonObjectFormatVisitor expectObjectFormat(final JavaType type)
                throws JsonMappingException {
            process(type);
            return new ObjectVisitor();
        }

        @Override
        public JsonArrayFormatVisitor expectArrayFormat(final JavaType type)
                throws JsonMappingException {
            process(type);
            return null;
        }

        @Override
        public JsonMapFormatVisitor expectMapFormat(final JavaType type)
                throws JsonMappingException {
            process(type);
            return null;
        }

        @Override
        public JsonAnyFormatVisitor expectAnyFormat(final JavaType type)
                throws JsonMappingException {
            process(type);
            return null;
        }
    }

    private class ObjectVisitor extends JsonObjectFormatVisitor.Base {

        @Override
        public void property(final BeanProperty prop) throws JsonMappingException {
            process(prop.getType());
        }

        @Override
        public void property(
                final String name,
                final JsonFormatVisitable handler,
                final JavaType propertyTypeHint)
                throws JsonMappingException {
            process(propertyTypeHint);
        }

        @Override
        public void optionalProperty(final BeanProperty prop) throws JsonMappingException {
            process(prop.getType());
        }

        @Override
        public void optionalProperty(
                final String name,
                final JsonFormatVisitable handler,
                final JavaType propertyTypeHint)
                throws JsonMappingException {
            process(propertyTypeHint);
        }
    }

    private static class PolymorphicExtractorException extends RuntimeException {
        PolymorphicExtractorException(final Class<?> type, final Exception e) {
            super("Failed to extract polymorphic types from " + type, e);
        }
    }
}
