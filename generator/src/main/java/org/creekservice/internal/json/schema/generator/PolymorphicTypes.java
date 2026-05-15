/*
 * Copyright 2022-2026 Creek Contributors (https://github.com/creek-service)
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
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.creekservice.api.json.schema.generator.GeneratorOptions.TypeScanningSpec;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;

/**
 * Helper for finding subtypes of any polymorphic type annotated with {@code @JsonTypeInfo(use =
 * TYPE)}.
 *
 * <p>Polymorphic types are found by walking the object and property graph using Jackson.
 */
final class PolymorphicTypes {

    static Collection<PolymorphicType<?>> findPolymorphicTypes(
            final Collection<Class<?>> types,
            final TypeScanningSpec subtypeScanning,
            final ObjectMapper objectMapper) {
        return new PolymorphicTypes(subtypeScanning, objectMapper).findPolymorphicTypes(types);
    }

    static final class PolymorphicType<T> {

        private final Class<T> type;
        private final Set<Class<? extends T>> subTypes;

        PolymorphicType(final Class<T> type, final Collection<Class<? extends T>> subTypes) {
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
            final List<Class<?>> sortedSubTypes =
                    subTypes.stream()
                            .sorted(Comparator.comparing(Class::getName))
                            .collect(Collectors.toUnmodifiableList());
            return "PolymorphicType{" + "type=" + type + ", subTypes=" + sortedSubTypes + '}';
        }
    }

    private final ObjectMapper objectMapper;
    private final TypeScanningSpec subtypeScanning;
    private final Map<Class<?>, PolymorphicType<?>> found = new HashMap<>();

    private PolymorphicTypes(
            final TypeScanningSpec subtypeScanning, final ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.subtypeScanning = requireNonNull(subtypeScanning, "subtypeScanning");
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

    private void visitType(final Class<?> type) {
        visitType(objectMapper.constructType(type));
    }

    private void visitType(final JavaType type) {
        objectMapper.acceptJsonFormatVisitor(type, new FormatVisitor());
    }

    private void process(final JavaType type) {
        extractPolyInfo(type.getRawClass());

        for (int i = 0; i < type.containedTypeCount(); i++) {
            final JavaType containedType = type.containedType(i);
            visitType(containedType);
        }
    }

    private <T> void extractPolyInfo(final Class<T> type) {
        if (found.containsKey(type)) {
            return;
        }

        final PolymorphicType<T> poly = new PolymorphicType<>(type, implementationsOf(type));
        found.put(type, poly);

        // Visit subtypes:
        for (final Class<? extends T> subType : poly.subTypes()) {
            // Optimisation: subtypes already found for parent, which is super set of subtype:
            found.put(subType, new PolymorphicType<>(subType, Set.of()));

            visitType(subType);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<Class<? extends T>> implementationsOf(final Class<T> type) {
        final JsonTypeInfo typeInfo = type.getAnnotation(JsonTypeInfo.class);
        if (typeInfo == null) {
            // Not a polymorphic type
            return List.of();
        }

        final JsonSubTypes subTypes = type.getAnnotation(JsonSubTypes.class);
        if (subTypes != null) {
            // Using hard-coded subtypes.
            return List.of();
        }

        final JsonTypeInfo.Id use = typeInfo.use();
        if (use == JsonTypeInfo.Id.NONE || use == JsonTypeInfo.Id.CUSTOM) {
            // No registered type scanning needed
            return List.of();
        }

        try (ScanResult sr =
                new ClassGraph()
                        .ignoreClassVisibility()
                        .enableClassInfo()
                        .acceptModules(subtypeScanning.moduleWhiteList().toArray(String[]::new))
                        .acceptPackages(subtypeScanning.packageWhiteList().toArray(String[]::new))
                        .scan()) {

            final ClassInfoList found =
                    type.isInterface()
                            ? sr.getClassesImplementing(type.getName())
                            : sr.getSubclasses(type.getName());

            return (List) found.stream().map(ClassInfo::loadClass).collect(Collectors.toList());
        }
    }

    private final class FormatVisitor extends JsonFormatVisitorWrapper.Base {

        @Override
        public JsonObjectFormatVisitor expectObjectFormat(final JavaType type) {
            process(type);
            return new ObjectVisitor(getContext());
        }

        @Override
        public JsonArrayFormatVisitor expectArrayFormat(final JavaType type) {
            process(type);
            return null;
        }

        @Override
        public JsonMapFormatVisitor expectMapFormat(final JavaType type) {
            process(type);
            return null;
        }

        @Override
        public JsonAnyFormatVisitor expectAnyFormat(final JavaType type) {
            process(type);
            return null;
        }
    }

    private final class ObjectVisitor extends JsonObjectFormatVisitor.Base {

        ObjectVisitor(final SerializationContext ctx) {
            super(ctx);
        }

        @Override
        public void property(final BeanProperty prop) {
            process(prop.getType());
        }

        @Override
        public void property(
                final String name,
                final JsonFormatVisitable handler,
                final JavaType propertyTypeHint) {
            process(propertyTypeHint);
        }

        @Override
        public void optionalProperty(final BeanProperty prop) {
            process(prop.getType());
        }

        @Override
        public void optionalProperty(
                final String name,
                final JsonFormatVisitable handler,
                final JavaType propertyTypeHint) {
            process(propertyTypeHint);
        }
    }

    private static class PolymorphicExtractorException extends RuntimeException {
        PolymorphicExtractorException(final Class<?> type, final Exception e) {
            super("Failed to extract polymorphic types from " + type, e);
        }
    }
}
