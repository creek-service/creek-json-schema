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

import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static org.creekservice.api.base.type.Preconditions.requireNonBlank;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.creekservice.api.base.annotation.schema.JsonSchemaInject;

final class JsonSchemaGeneratorFactory {

    private static final String INTEGER = "integer";
    private static final String NUMBER = "number";
    private static final String STRING = "string";

    private static final ObjectMapper JSON_MAPPER = JsonMapper.builder().build();

    private static final Map<Class<?>, Mapping> TYPE_MAPPINGS =
            Map.ofEntries(
                    entry(BigDecimal.class, Mapping.toType(NUMBER)),
                    entry(BigInteger.class, Mapping.toType(NUMBER)),
                    entry(URI.class, Mapping.toType(STRING).withDefaultFormat("uri")),
                    entry(
                            LocalTime.class,
                            Mapping.toType(STRING)
                                    .withDefaultPattern(
                                            "^(?:[01]\\d|2[0-3]):(?:[0-5]\\d)(?::(?:[0-5]\\d)(?:\\.\\d{1,9})?)?$")),
                    entry(OffsetTime.class, Mapping.toType(STRING).withDefaultFormat("time")),
                    entry(LocalDate.class, Mapping.toType(STRING).withDefaultFormat("date")),
                    entry(
                            LocalDateTime.class,
                            Mapping.toType(STRING).withDefaultFormat("date-time")),
                    entry(
                            OffsetDateTime.class,
                            Mapping.toType(STRING).withDefaultFormat("date-time")),
                    entry(
                            ZonedDateTime.class,
                            Mapping.toType(STRING).withDefaultFormat("date-time")),
                    entry(Instant.class, Mapping.toType(STRING).withDefaultFormat("date-time")),
                    entry(Duration.class, Mapping.toType(NUMBER)),
                    entry(
                            Period.class,
                            Mapping.toType(STRING)
                                    .withDefaultFormat("duration")
                                    .withDefaultPattern(
                                            "^P(?=\\d)(?:\\d+Y)?(?:\\d+M)?(?:\\d+W)?(?:\\d+D)?$")),
                    entry(
                            MonthDay.class,
                            Mapping.toType(STRING)
                                    .withDefaultPattern(
                                            "^--(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])$")),
                    entry(
                            YearMonth.class,
                            Mapping.toType(STRING)
                                    .withDefaultPattern("^-?\\d{4,}-(?:0[1-9]|1[0-2])$")),
                    entry(Year.class, Mapping.toType(STRING).withDefaultPattern("^-?\\d+$")),
                    entry(
                            byte.class,
                            Mapping.toType(INTEGER)
                                    .withDefaultMinimum(Byte.MIN_VALUE)
                                    .withDefaultMaximum(Byte.MAX_VALUE)),
                    entry(
                            Byte.class,
                            Mapping.toType(INTEGER)
                                    .withDefaultMinimum(Byte.MIN_VALUE)
                                    .withDefaultMaximum(Byte.MAX_VALUE)),
                    entry(
                            short.class,
                            Mapping.toType(INTEGER)
                                    .withDefaultMinimum(Short.MIN_VALUE)
                                    .withDefaultMaximum(Short.MAX_VALUE)),
                    entry(
                            Short.class,
                            Mapping.toType(INTEGER)
                                    .withDefaultMinimum(Short.MIN_VALUE)
                                    .withDefaultMaximum(Short.MAX_VALUE)),
                    entry(
                            int.class,
                            Mapping.toType(INTEGER)
                                    .withDefaultMinimum(Integer.MIN_VALUE)
                                    .withDefaultMaximum(Integer.MAX_VALUE)),
                    entry(
                            Integer.class,
                            Mapping.toType(INTEGER)
                                    .withDefaultMinimum(Integer.MIN_VALUE)
                                    .withDefaultMaximum(Integer.MAX_VALUE)),
                    entry(
                            long.class,
                            Mapping.toType(INTEGER)
                                    .withDefaultMinimum(Long.MIN_VALUE)
                                    .withDefaultMaximum(Long.MAX_VALUE)),
                    entry(
                            Long.class,
                            Mapping.toType(INTEGER)
                                    .withDefaultMinimum(Long.MIN_VALUE)
                                    .withDefaultMaximum(Long.MAX_VALUE)));

    private JsonSchemaGeneratorFactory() {}

    static com.github.victools.jsonschema.generator.SchemaGenerator createGenerator(
            final ObjectMapper mapper) {
        return new com.github.victools.jsonschema.generator.SchemaGenerator(createConfig(mapper));
    }

    private static SchemaGeneratorConfig createConfig(final ObjectMapper mapper) {

        final SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(
                        mapper, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);

        // Must precede Swagger2Module: it wraps this with @Schema(name=...) priority.
        configBuilder
                .forTypesInGeneral()
                .withDefinitionNamingStrategy(new JsonTypeNameNamingStrategy());

        // Must precede JacksonModule: prevents JsonSubTypesResolver self-referential schemas.
        configBuilder
                .forTypesInGeneral()
                .withCustomDefinitionProvider(new PolymorphicOneOfDefinitionProvider(mapper));

        configBuilder
                .with(
                        new JacksonModule(
                                JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE,
                                JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY,
                                JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                                JacksonOption.RESPECT_JSONPROPERTY_ORDER,
                                JacksonOption.ALWAYS_REF_SUBTYPES))
                .with(new Swagger2Module())
                // Getter-based schema: derive property names from get/is methods.
                .with(
                        Option.GETTER_METHODS,
                        Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS,
                        Option.NONSTATIC_NONVOID_NONGETTER_METHODS,
                        Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                // No fields. No FLATTENED_OPTIONALS (adds nullable); we unwrap Optional below.
                .without(
                        Option.PUBLIC_NONSTATIC_FIELDS,
                        Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
                        Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
                        Option.VALUES_FROM_CONSTANT_FIELDS,
                        Option.FLATTENED_OPTIONALS);

        configureMethodResolvers(configBuilder, mapper);
        configureAutoTitle(configBuilder);
        configureTypeMappings(configBuilder);
        configureJsonSchemaInject(configBuilder);

        return configBuilder.build();
    }

    private static void configureMethodResolvers(
            final SchemaGeneratorConfigBuilder configBuilder, final ObjectMapper mapper) {
        // Workaround: victools crashes on single-char getter names. Must be first.
        configBuilder
                .forMethods()
                .withPropertyNameOverrideResolver(JsonSchemaGeneratorFactory::singleCharGetterName);

        // Delegate visibility, naming, required, defaults to Jackson's BeanDescription.
        configBuilder.forMethods().withIgnoreCheck(method -> shouldIgnoreMethod(method, mapper));
        configBuilder
                .forMethods()
                .withPropertyNameOverrideResolver(method -> jacksonPropertyName(method, mapper));
        configBuilder
                .forMethods()
                .withRequiredCheck(method -> method.getType().getErasedType().isPrimitive());
        configBuilder.forMethods().withRequiredCheck(method -> isJacksonRequired(method, mapper));
        configBuilder.forMethods().withDefaultResolver(method -> jacksonDefault(method, mapper));

        // Unwrap Optional<T> → T without adding nullable.
        configBuilder
                .forMethods()
                .withTargetTypeOverridesResolver(
                        JsonSchemaGeneratorFactory::unwrapOptionalReturnType);
    }

    private static java.util.List<com.fasterxml.classmate.ResolvedType> unwrapOptionalReturnType(
            final MethodScope method) {
        if (!method.getType().isInstanceOf(Optional.class)) {
            return null;
        }
        final com.fasterxml.classmate.ResolvedType inner =
                method.getTypeParameterFor(Optional.class, 0);
        return inner == null ? null : Collections.singletonList(inner);
    }

    private static String singleCharGetterName(final MethodScope method) {
        final String name = method.getDeclaredName();
        if (name.startsWith("get") && name.length() == 4 && Character.isUpperCase(name.charAt(3))) {
            return String.valueOf(Character.toLowerCase(name.charAt(3)));
        }
        if (name.startsWith("is") && name.length() == 3 && Character.isUpperCase(name.charAt(2))) {
            return String.valueOf(Character.toLowerCase(name.charAt(2)));
        }
        return null;
    }

    /**
     * Looks up the Jackson {@link BeanPropertyDefinition} for the given method within its declaring
     * type. Returns {@code null} if the method does not correspond to any Jackson-visible
     * serializable property (e.g. it is {@code @JsonIgnore}d, or not a recognised getter).
     */
    private static BeanPropertyDefinition findJacksonProperty(
            final MethodScope method, final ObjectMapper mapper) {
        final BeanDescription desc =
                mapper.getSerializationConfig()
                        .introspect(
                                mapper.constructType(method.getDeclaringType().getErasedType()));
        final java.lang.reflect.Method rawMethod = method.getRawMember();
        return desc.findProperties().stream()
                .filter(
                        prop ->
                                prop.getGetter() != null
                                        && rawMethod.equals(prop.getGetter().getAnnotated()))
                .findFirst()
                .orElse(null);
    }

    private static boolean shouldIgnoreMethod(final MethodScope method, final ObjectMapper mapper) {
        return findJacksonProperty(method, mapper) == null;
    }

    private static String jacksonPropertyName(final MethodScope method, final ObjectMapper mapper) {
        final BeanPropertyDefinition prop = findJacksonProperty(method, mapper);
        if (prop == null) {
            return null;
        }
        // Only override name for non-getter methods (e.g. @JsonGetter); let victools
        // derive standard getter names (handles acronyms like getURI() → "URI").
        return isStandardGetterName(method.getDeclaredName()) ? null : prop.getName();
    }

    private static boolean isStandardGetterName(final String name) {
        return isGetStyleGetter(name) || isIsStyleGetter(name);
    }

    private static boolean isGetStyleGetter(final String name) {
        return name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3));
    }

    private static boolean isIsStyleGetter(final String name) {
        return name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2));
    }

    private static boolean isJacksonRequired(final MethodScope method, final ObjectMapper mapper) {
        final BeanPropertyDefinition prop = findJacksonProperty(method, mapper);
        return prop != null && prop.isRequired();
    }

    private static Object jacksonDefault(final MethodScope method, final ObjectMapper mapper) {
        final BeanPropertyDefinition prop = findJacksonProperty(method, mapper);
        if (prop == null) {
            return null;
        }
        final String defaultValue = prop.getMetadata().getDefaultValue();
        return defaultValue == null || defaultValue.isEmpty() ? null : defaultValue;
    }

    /**
     * Auto-generates human-readable titles from class simple names (e.g. {@code MyModel} → {@code
     * My Model}), but only for user-defined types (not Java built-ins, primitives or remapped
     * types).
     *
     * <p>Title resolvers are called in registration order; the first non-null result wins. The
     * Swagger2Module registers its title resolver via {@link
     * io.swagger.v3.oas.annotations.media.Schema#title}, so this resolver only needs to handle the
     * auto-title fallback for types that have no explicit {@code @Schema(title=...)} annotation.
     */
    private static void configureAutoTitle(final SchemaGeneratorConfigBuilder configBuilder) {
        configBuilder
                .forTypesInGeneral()
                .withTitleResolver(
                        scope -> {
                            final Class<?> clazz = scope.getType().getErasedType();
                            if (clazz.isPrimitive()) {
                                return null;
                            }
                            if (TYPE_MAPPINGS.containsKey(clazz)) {
                                return null;
                            }
                            if (isSystemPackage(clazz.getPackageName())) {
                                return null;
                            }
                            return toTitleCase(clazz.getSimpleName());
                        });
    }

    private static boolean isSystemPackage(final String pkg) {
        return pkg.startsWith("java.")
                || pkg.startsWith("javax.")
                || pkg.startsWith("com.fasterxml.")
                || pkg.startsWith("kotlin.");
    }

    private static void configureTypeMappings(final SchemaGeneratorConfigBuilder configBuilder) {
        configBuilder
                .forTypesInGeneral()
                .withCustomDefinitionProvider(
                        (type, ctx) -> {
                            final Mapping mapping = TYPE_MAPPINGS.get(type.getErasedType());
                            if (mapping == null) {
                                return null;
                            }
                            final ObjectNode def =
                                    ctx.getGeneratorConfig().getObjectMapper().createObjectNode();
                            def.put(ctx.getKeyword(SchemaKeyword.TAG_TYPE), mapping.jsonType());
                            return new CustomDefinition(
                                    def,
                                    CustomDefinition.DefinitionType.INLINE,
                                    CustomDefinition.AttributeInclusion.YES);
                        });

        // Defaults applied after Swagger2Module; putIfAbsent lets @Schema win.
        configBuilder
                .forFields()
                .withInstanceAttributeOverride(
                        (node, field, ctx) ->
                                applyMappingDefaults(node, field.getType().getErasedType(), ctx));

        configBuilder
                .forMethods()
                .withInstanceAttributeOverride(
                        (node, method, ctx) ->
                                applyMappingDefaults(node, method.getType().getErasedType(), ctx));
    }

    private static void applyMappingDefaults(
            final ObjectNode node, final Class<?> type, final SchemaGenerationContext ctx) {
        final Mapping mapping = TYPE_MAPPINGS.get(type);
        if (mapping == null) {
            return;
        }
        mapping.format()
                .ifPresent(
                        fmt ->
                                node.putIfAbsent(
                                        ctx.getKeyword(SchemaKeyword.TAG_FORMAT),
                                        node.textNode(fmt)));
        mapping.pattern()
                .ifPresent(
                        pat ->
                                node.putIfAbsent(
                                        ctx.getKeyword(SchemaKeyword.TAG_PATTERN),
                                        node.textNode(pat)));
        mapping.minimum()
                .ifPresent(
                        min ->
                                node.putIfAbsent(
                                        ctx.getKeyword(SchemaKeyword.TAG_MINIMUM),
                                        node.numberNode(min)));
        mapping.maximum()
                .ifPresent(
                        max ->
                                node.putIfAbsent(
                                        ctx.getKeyword(SchemaKeyword.TAG_MAXIMUM),
                                        node.numberNode(max)));
    }

    private static void configureJsonSchemaInject(
            final SchemaGeneratorConfigBuilder configBuilder) {
        configBuilder
                .forTypesInGeneral()
                .withTypeAttributeOverride(
                        (node, scope, ctx) ->
                                findInjectJson(scope.getType().getErasedType())
                                        .ifPresent(json -> mergeJson(node, json, JSON_MAPPER)));

        // Skip fake container-item scopes (prevents List getter inject leaking to items).
        configBuilder
                .forMethods()
                .withInstanceAttributeOverride(
                        (node, scope, ctx) -> {
                            if (scope.isFakeContainerItemScope()) {
                                return;
                            }
                            findInjectJson(scope)
                                    .ifPresent(json -> mergeJson(node, json, JSON_MAPPER));
                        });
    }

    private static Optional<String> findInjectJson(final Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(JsonSchemaInject.class))
                .map(JsonSchemaInject::value);
    }

    private static Optional<String> findInjectJson(final MethodScope scope) {
        return Optional.ofNullable(scope.getAnnotation(JsonSchemaInject.class))
                .map(JsonSchemaInject::value);
    }

    private static void mergeJson(
            final ObjectNode node, final String json, final ObjectMapper mapper) {
        if (json.isEmpty()) {
            return;
        }
        try {
            final ObjectNode extra = (ObjectNode) mapper.readTree(json);
            extra.properties().forEach(entry -> node.set(entry.getKey(), entry.getValue()));
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid @JsonSchemaInject JSON: " + json, e);
        }
    }

    /** Converts {@code CamelCase} or {@code camelCase} to {@code Title Case With Spaces}. */
    static String toTitleCase(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) {
                result.append(' ');
            }
            result.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return result.toString();
    }

    /**
     * A {@link SchemaDefinitionNamingStrategy} that uses the {@link JsonTypeName} annotation value
     * (if present and non-empty) as the definition name, falling back to the simple class name.
     *
     * <p>This enables the schema's {@code definitions} keys to use Jackson type identifiers (e.g.
     * {@code "the-explicit-name"}) rather than Java class names, which improves readability and
     * stability across refactors.
     *
     * <p>This strategy is set <em>before</em> {@link Swagger2Module} is applied. Swagger2Module
     * wraps it with {@code Swagger2SchemaDefinitionNamingStrategy}, which checks
     * {@code @Schema(name=...)} first, then delegates here.
     */
    private static final class JsonTypeNameNamingStrategy
            implements SchemaDefinitionNamingStrategy {

        @Override
        public String getDefinitionNameForKey(
                final DefinitionKey key, final SchemaGenerationContext context) {
            final JsonTypeName typeName =
                    key.getType().getErasedType().getAnnotation(JsonTypeName.class);
            if (typeName != null && !typeName.value().isEmpty()) {
                return typeName.value();
            }
            return context.getTypeContext().getSimpleTypeDescription(key.getType());
        }

        @Override
        public void adjustDuplicateNames(
                final Map<DefinitionKey, String> duplicateNames,
                final SchemaGenerationContext context) {
            // Fall back to full type description for duplicates
            duplicateNames.replaceAll(
                    (key, name) -> context.getTypeContext().getFullTypeDescription(key.getType()));
        }
    }

    /**
     * A victools {@link CustomDefinitionProviderV2} that intercepts polymorphic base types (those
     * with a direct {@link JsonTypeInfo} annotation) and generates a {@code oneOf} schema listing
     * all known subtypes.
     *
     * <p>Must be registered <em>before</em> the JacksonModule so that this provider runs first in
     * the chain and prevents {@code JsonSubTypesResolver} from generating a circular reference for
     * implicit base types (those with {@link JsonTypeInfo} but no {@link JsonSubTypes}).
     *
     * <p>For types with known subtypes, returns a {@code oneOf} schema. For types with no subtypes,
     * returns a plain {@code {type: object}} schema.
     *
     * <p>Also intercepts subtypes of bases that use {@link JsonTypeInfo.Id#SIMPLE_NAME} or {@link
     * JsonTypeInfo.Id#MINIMAL_CLASS}, since the JacksonModule's {@code JsonSubTypesResolver} does
     * not handle those modes. For {@link JsonTypeInfo.Id#NAME} and {@link JsonTypeInfo.Id#CLASS}
     * subtypes, returns {@code null} so that JacksonModule handles them.
     */
    private static final class PolymorphicOneOfDefinitionProvider
            implements CustomDefinitionProviderV2 {

        private final ObjectMapper mapper;

        PolymorphicOneOfDefinitionProvider(final ObjectMapper mapper) {
            this.mapper = requireNonNull(mapper, "mapper");
        }

        @Override
        public CustomDefinition provideCustomSchemaDefinition(
                final ResolvedType javaType, final SchemaGenerationContext context) {
            if (javaType == null) {
                return null;
            }
            final Class<?> erasedType = javaType.getErasedType();

            // Only direct @JsonTypeInfo; subtypes defer to JsonSubTypesResolver.
            final JsonTypeInfo typeInfo = erasedType.getAnnotation(JsonTypeInfo.class);
            if (typeInfo != null) {
                return handleBaseType(erasedType, context);
            }

            // Handle SIMPLE_NAME/MINIMAL_CLASS subtypes (JsonSubTypesResolver skips them).
            return handleSubtypeIfNeeded(javaType, erasedType, context);
        }

        private CustomDefinition handleBaseType(
                final Class<?> erasedType, final SchemaGenerationContext context) {

            final List<ResolvedType> subtypes = findSubtypes(erasedType, context.getTypeContext());

            if (subtypes != null && !subtypes.isEmpty()) {
                final ObjectNode definition = context.getGeneratorConfig().createObjectNode();
                final ArrayNode oneOf =
                        definition.withArray(context.getKeyword(SchemaKeyword.TAG_ONEOF));
                subtypes.forEach(
                        subtype ->
                                oneOf.add(
                                        context.createStandardDefinitionReference(subtype, null)));
                // NO: exclude type-level attrs (additionalProperties:false would break oneOf).
                return new CustomDefinition(
                        definition,
                        CustomDefinition.DefinitionType.STANDARD,
                        CustomDefinition.AttributeInclusion.NO);
            }

            // No subtypes found: return a plain object schema to prevent JsonSubTypesResolver
            // from incorrectly generating a self-referential discriminator schema for the base
            // type.
            final ObjectNode placeholder = context.getGeneratorConfig().createObjectNode();
            placeholder.put(
                    context.getKeyword(SchemaKeyword.TAG_TYPE),
                    context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));
            return new CustomDefinition(
                    placeholder,
                    CustomDefinition.DefinitionType.STANDARD,
                    CustomDefinition.AttributeInclusion.YES);
        }

        /**
         * If this type is a subtype of a SIMPLE_NAME or MINIMAL_CLASS polymorphic base, generates a
         * discriminator schema (allOf with standard def + discriminator property). Returns {@code
         * null} for NAME and CLASS subtypes so JacksonModule handles them.
         */
        private CustomDefinition handleSubtypeIfNeeded(
                final ResolvedType javaType,
                final Class<?> erasedType,
                final SchemaGenerationContext context) {
            return findAncestorTypeInfo(erasedType)
                    .flatMap(
                            parentInfo -> {
                                final JsonTypeInfo.Id use = parentInfo.typeInfo().use();
                                if (use != JsonTypeInfo.Id.SIMPLE_NAME
                                        && use != JsonTypeInfo.Id.MINIMAL_CLASS) {
                                    // NAME and CLASS are handled by JacksonModule
                                    return Optional.empty();
                                }
                                return Optional.of(
                                        buildSubtypeDiscriminatorSchema(
                                                javaType, erasedType, parentInfo, context));
                            })
                    .orElse(null);
        }

        private CustomDefinition buildSubtypeDiscriminatorSchema(
                final ResolvedType javaType,
                final Class<?> erasedType,
                final ParentTypeInfo parentInfo,
                final SchemaGenerationContext context) {
            final JsonTypeInfo typeInfo = parentInfo.typeInfo();
            final String propertyName =
                    typeInfo.property().isEmpty()
                            ? typeInfo.use().getDefaultPropertyName()
                            : typeInfo.property();
            final String typeIdentifier =
                    computeTypeIdentifier(erasedType, parentInfo.baseType(), typeInfo.use());

            final ObjectNode definition = context.getGeneratorConfig().createObjectNode();
            final ArrayNode allOf =
                    definition.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF));
            allOf.add(context.createStandardDefinitionReference(javaType, this));
            final ObjectNode discriminatorPart = allOf.addObject();
            discriminatorPart
                    .put(
                            context.getKeyword(SchemaKeyword.TAG_TYPE),
                            context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                    .putObject(context.getKeyword(SchemaKeyword.TAG_PROPERTIES))
                    .putObject(propertyName)
                    .put(context.getKeyword(SchemaKeyword.TAG_CONST), typeIdentifier);
            discriminatorPart
                    .withArray(context.getKeyword(SchemaKeyword.TAG_REQUIRED))
                    .add(propertyName);
            return new CustomDefinition(
                    definition,
                    CustomDefinition.DefinitionType.ALWAYS_REF,
                    CustomDefinition.AttributeInclusion.NO);
        }

        private static String computeTypeIdentifier(
                final Class<?> subtype, final Class<?> baseType, final JsonTypeInfo.Id use) {
            return switch (use) {
                case SIMPLE_NAME -> {
                    final JsonTypeName typeName = subtype.getAnnotation(JsonTypeName.class);
                    yield typeName != null ? typeName.value() : subtype.getSimpleName();
                }
                case MINIMAL_CLASS -> {
                    // Strip the base type's package prefix, leaving a leading "." + remainder.
                    // If the subtype is in a shorter or different package, fall back to the
                    // fully-qualified class name.
                    final String subtypeName = subtype.getName();
                    final String basePackage = baseType.getPackageName();
                    yield subtypeName.length() > basePackage.length()
                                    && subtypeName.startsWith(basePackage)
                            ? subtypeName.substring(basePackage.length())
                            : subtypeName;
                }
                default -> throw new IllegalArgumentException("Unsupported use: " + use);
            };
        }

        /**
         * Finds the nearest {@link JsonTypeInfo} annotation on an ancestor of {@code cls} by
         * walking the class hierarchy up to (but not including) {@link Object}, and also checking
         * implemented interfaces recursively.
         *
         * <p>This handles multi-level hierarchies where only the root base type carries
         * {@code @JsonTypeInfo}: a deep subtype (e.g. {@code Concrete extends Middle extends Base})
         * must still receive the discriminator property even though its immediate parent ({@code
         * Middle}) has no direct {@code @JsonTypeInfo}. It also handles interface-based
         * polymorphism where {@code @JsonTypeInfo} is on an interface rather than a superclass.
         *
         * <p>Types that have {@code @JsonTypeInfo} <em>directly</em> are handled by {@link
         * #handleBaseType} before reaching this method, so there is no risk of double-processing.
         */
        private static Optional<ParentTypeInfo> findAncestorTypeInfo(final Class<?> cls) {
            // Check interfaces implemented by this class
            final Optional<ParentTypeInfo> fromInterface = findTypeInfoInInterfaces(cls);
            if (fromInterface.isPresent()) {
                return fromInterface;
            }

            // Walk superclass hierarchy
            Class<?> ancestor = cls.getSuperclass();
            while (ancestor != null && ancestor != Object.class) {
                final JsonTypeInfo info = ancestor.getAnnotation(JsonTypeInfo.class);
                if (info != null) {
                    return Optional.of(new ParentTypeInfo(ancestor, info));
                }

                // Check interfaces of this superclass
                final Optional<ParentTypeInfo> fromSuperInterface =
                        findTypeInfoInInterfaces(ancestor);
                if (fromSuperInterface.isPresent()) {
                    return fromSuperInterface;
                }

                ancestor = ancestor.getSuperclass();
            }
            return Optional.empty();
        }

        private static Optional<ParentTypeInfo> findTypeInfoInInterfaces(final Class<?> cls) {
            for (final Class<?> iface : cls.getInterfaces()) {
                final JsonTypeInfo info = iface.getAnnotation(JsonTypeInfo.class);
                if (info != null) {
                    return Optional.of(new ParentTypeInfo(iface, info));
                }
                // Recursively check parent interfaces
                final Optional<ParentTypeInfo> fromParent = findTypeInfoInInterfaces(iface);
                if (fromParent.isPresent()) {
                    return fromParent;
                }
            }
            return Optional.empty();
        }

        private record ParentTypeInfo(Class<?> baseType, JsonTypeInfo typeInfo) {}

        private List<ResolvedType> findSubtypes(
                final Class<?> erasedType, final TypeContext typeContext) {
            // Check for explicit subtypes via @JsonSubTypes on the base type
            final JsonSubTypes subTypesAnnotation = erasedType.getAnnotation(JsonSubTypes.class);
            if (subTypesAnnotation != null) {
                return Arrays.stream(subTypesAnnotation.value())
                        .map(entry -> typeContext.resolve(entry.value()))
                        .collect(Collectors.toList());
            }

            // Fall back to subtypes registered on the mapper (implicit polymorphism).
            // Deduplicate (double-registration is possible) and sort by class name for
            // deterministic ordering.
            final BeanDescription beanDesc =
                    mapper.getSerializationConfig().introspectClassAnnotations(erasedType);
            final Collection<NamedType> registered =
                    mapper.getSubtypeResolver()
                            .collectAndResolveSubtypesByClass(
                                    mapper.getSerializationConfig(), beanDesc.getClassInfo());

            if (registered == null || registered.isEmpty()) {
                return null;
            }

            final Map<Class<?>, ResolvedType> unique = new LinkedHashMap<>();
            registered.stream()
                    .filter(nt -> nt.getType() != null && !nt.getType().equals(erasedType))
                    .forEach(
                            nt ->
                                    unique.putIfAbsent(
                                            nt.getType(), typeContext.resolve(nt.getType())));

            final List<ResolvedType> subtypes =
                    unique.entrySet().stream()
                            .sorted(Comparator.comparing(e -> e.getKey().getName()))
                            .map(Map.Entry::getValue)
                            .collect(Collectors.toList());

            return subtypes.isEmpty() ? null : subtypes;
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class Mapping {

        private final String jsonType;
        private final Optional<String> format;
        private final Optional<String> pattern;
        private final Optional<Long> minimum;
        private final Optional<Long> maximum;

        private Mapping(
                final String jsonType,
                final Optional<String> format,
                final Optional<String> pattern,
                final Optional<Long> minimum,
                final Optional<Long> maximum) {
            this.jsonType = requireNonBlank(jsonType, "jsonType");
            this.format = requireNonNull(format, "format");
            this.format.ifPresent(f -> requireNonBlank(f, "format"));
            this.pattern = requireNonNull(pattern, "pattern");
            this.pattern.ifPresent(p -> requireNonBlank(p, "pattern"));
            this.minimum = requireNonNull(minimum, "minimum");
            this.maximum = requireNonNull(maximum, "maximum");
        }

        static Mapping toType(final String jsonType) {
            return new Mapping(
                    jsonType,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        Mapping withDefaultFormat(final String format) {
            return new Mapping(jsonType, Optional.of(format), pattern, minimum, maximum);
        }

        Mapping withDefaultPattern(final String pattern) {
            return new Mapping(jsonType, format, Optional.of(pattern), minimum, maximum);
        }

        Mapping withDefaultMinimum(final long minimum) {
            return new Mapping(jsonType, format, pattern, Optional.of(minimum), maximum);
        }

        Mapping withDefaultMaximum(final long maximum) {
            return new Mapping(jsonType, format, pattern, minimum, Optional.of(maximum));
        }

        String jsonType() {
            return jsonType;
        }

        Optional<String> format() {
            return format;
        }

        Optional<String> pattern() {
            return pattern;
        }

        Optional<Long> minimum() {
            return minimum;
        }

        Optional<Long> maximum() {
            return maximum;
        }
    }
}
