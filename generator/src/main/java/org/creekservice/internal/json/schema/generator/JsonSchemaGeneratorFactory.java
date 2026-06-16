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
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.creekservice.api.base.annotation.schema.JsonSchemaInject;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

final class JsonSchemaGeneratorFactory {

    private static final String INTEGER = "integer";
    private static final String NUMBER = "number";
    private static final String STRING = "string";

    /** ISO 8601 local time pattern (no date/timezone): e.g. {@code 14:30:00.5} */
    private static final String LOCAL_TIME_PATTERN =
            "^(?:[01]\\d|2[0-3]):(?:[0-5]\\d)(?::(?:[0-5]\\d)(?:\\.\\d{1,9})?)?$";

    /** ISO 8601 local date-time pattern (no timezone): e.g. {@code 2026-05-14T15:34:56.466} */
    private static final String LOCAL_DATE_TIME_PATTERN =
            "^\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])"
                    + "T(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d(?:\\.\\d{1,9})?)?$";

    /** ISO 8601 period pattern (date part only, no time): e.g. {@code P1Y2M3D} */
    private static final String PERIOD_PATTERN =
            "^P(?=\\d)(?:\\d+Y)?(?:\\d+M)?(?:\\d+W)?(?:\\d+D)?$";

    /** ISO 8601 month-day pattern: e.g. {@code --05-14} */
    private static final String MONTH_DAY_PATTERN =
            "^--(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])$";

    /** ISO 8601 year-month pattern: e.g. {@code 2026-05} */
    private static final String YEAR_MONTH_PATTERN = "^-?\\d{4,}-(?:0[1-9]|1[0-2])$";

    private static final ObjectMapper JSON_MAPPER = JsonMapper.builder().build();

    private static final Map<Class<?>, Mapping> TYPE_MAPPINGS =
            Map.ofEntries(
                    entry(BigDecimal.class, Mapping.toType(NUMBER)),
                    entry(BigInteger.class, Mapping.toType(NUMBER)),
                    entry(URI.class, Mapping.toType(STRING).withDefaultFormat("uri")),
                    entry(
                            LocalTime.class,
                            Mapping.toType(STRING).withDefaultPattern(LOCAL_TIME_PATTERN)),
                    entry(OffsetTime.class, Mapping.toType(STRING).withDefaultFormat("time")),
                    entry(LocalDate.class, Mapping.toType(STRING).withDefaultFormat("date")),
                    entry(
                            LocalDateTime.class,
                            Mapping.toType(STRING).withDefaultPattern(LOCAL_DATE_TIME_PATTERN)),
                    entry(
                            OffsetDateTime.class,
                            Mapping.toType(STRING).withDefaultFormat("date-time")),
                    entry(
                            ZonedDateTime.class,
                            Mapping.toType(STRING).withDefaultFormat("date-time")),
                    entry(Instant.class, Mapping.toType(STRING).withDefaultFormat("date-time")),
                    entry(Duration.class, Mapping.toType(STRING).withDefaultFormat("duration")),
                    entry(
                            Period.class,
                            Mapping.toType(STRING)
                                    .withDefaultFormat("duration")
                                    .withDefaultPattern(PERIOD_PATTERN)),
                    entry(
                            MonthDay.class,
                            Mapping.toType(STRING).withDefaultPattern(MONTH_DAY_PATTERN)),
                    entry(
                            YearMonth.class,
                            Mapping.toType(STRING).withDefaultPattern(YEAR_MONTH_PATTERN)),
                    entry(Year.class, Mapping.toType(INTEGER)),
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

        // Must precede JacksonSchemaModule: prevents JsonSubTypesResolver self-referential schemas.
        configBuilder
                .forTypesInGeneral()
                .withCustomDefinitionProvider(new PolymorphicOneOfDefinitionProvider(mapper));

        configBuilder
                .with(
                        new JacksonSchemaModule(
                                JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY,
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
                        Option.FLATTENED_OPTIONALS);

        configureMethodResolvers(configBuilder, mapper);
        configureAutoTitle(configBuilder);
        configureTypeMappings(configBuilder);
        configureJsonSchemaInject(configBuilder);

        return configBuilder.build();
    }

    private static void configureMethodResolvers(
            final SchemaGeneratorConfigBuilder configBuilder, final ObjectMapper mapper) {
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

    private static List<ResolvedType> unwrapOptionalReturnType(final MethodScope method) {
        final Class<?> erasedType = method.getType().getErasedType();
        if (erasedType == Optional.class) {
            return List.of(method.getTypeParameterFor(Optional.class, 0));
        }
        if (erasedType == OptionalInt.class) {
            return List.of(method.getContext().resolve(int.class));
        }
        if (erasedType == OptionalLong.class) {
            return List.of(method.getContext().resolve(long.class));
        }
        if (erasedType == OptionalDouble.class) {
            return List.of(method.getContext().resolve(double.class));
        }
        return null;
    }

    private static Optional<BeanPropertyDefinition> findJacksonProperty(
            final MethodScope method, final ObjectMapper mapper) {
        final SerializationConfig config = mapper.serializationConfig();
        final tools.jackson.databind.JavaType javaType =
                mapper.constructType(method.getDeclaringType().getErasedType());
        final AnnotatedClass ac =
                config.classIntrospectorInstance().introspectClassAnnotations(javaType);
        final BeanDescription desc =
                config.classIntrospectorInstance().introspectForSerialization(javaType, ac);
        final java.lang.reflect.Method rawMethod = method.getRawMember();
        return desc.findProperties().stream()
                .filter(prop -> prop.getGetter() != null)
                .filter(prop -> rawMethod.equals(prop.getGetter().getAnnotated()))
                .findFirst();
    }

    private static boolean shouldIgnoreMethod(final MethodScope method, final ObjectMapper mapper) {
        return findJacksonProperty(method, mapper).isEmpty();
    }

    private static String jacksonPropertyName(final MethodScope method, final ObjectMapper mapper) {
        return findJacksonProperty(method, mapper)
                .map(BeanPropertyDefinition::getName)
                .orElse(null);
    }

    private static boolean isJacksonRequired(final MethodScope method, final ObjectMapper mapper) {
        return findJacksonProperty(method, mapper)
                .map(BeanPropertyDefinition::isRequired)
                .orElse(false);
    }

    private static Object jacksonDefault(final MethodScope method, final ObjectMapper mapper) {
        return findJacksonProperty(method, mapper)
                .map(prop -> prop.getMetadata().getDefaultValue())
                .orElse(null);
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
                                        StringNode.valueOf(fmt)));
        mapping.pattern()
                .ifPresent(
                        pat ->
                                node.putIfAbsent(
                                        ctx.getKeyword(SchemaKeyword.TAG_PATTERN),
                                        StringNode.valueOf(pat)));
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
                                Optional.ofNullable(
                                                scope.getType()
                                                        .getErasedType()
                                                        .getAnnotation(JsonSchemaInject.class))
                                        .map(JsonSchemaInject::value)
                                        .ifPresent(json -> mergeJson(node, json)));

        // Skip fake container-item scopes (prevents List getter inject leaking to items).
        configBuilder
                .forMethods()
                .withInstanceAttributeOverride(
                        (node, scope, ctx) -> {
                            if (scope.isFakeContainerItemScope()) {
                                return;
                            }
                            Optional.ofNullable(scope.getAnnotation(JsonSchemaInject.class))
                                    .map(JsonSchemaInject::value)
                                    .ifPresent(json -> mergeJson(node, json));
                        });
    }

    private static void mergeJson(final ObjectNode node, final String json) {
        if (json.isEmpty()) {
            return;
        }
        try {
            final ObjectNode extra = (ObjectNode) JSON_MAPPER.readTree(json);
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
            if (typeName != null) {
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
     * <p>Must be registered <em>before</em> the JacksonSchemaModule so that this provider runs
     * first in the chain and prevents {@code JsonSubTypesResolver} from generating a circular
     * reference for implicit base types (those with {@link JsonTypeInfo} but no {@link
     * JsonSubTypes}).
     *
     * <p>For types with known subtypes, returns a {@code oneOf} schema. For types with no subtypes,
     * returns a plain {@code {type: object}} schema.
     *
     * <p>Also intercepts subtypes of bases that use {@link JsonTypeInfo.Id#SIMPLE_NAME} or {@link
     * JsonTypeInfo.Id#MINIMAL_CLASS}, since the JacksonSchemaModule's {@code JsonSubTypesResolver}
     * does not handle those modes. For {@link JsonTypeInfo.Id#NAME} and {@link
     * JsonTypeInfo.Id#CLASS} subtypes, returns {@code null} so that JacksonSchemaModule handles
     * them.
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

            if (!subtypes.isEmpty()) {
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

            // No subtypes: plain object prevents self-referential schema.
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
         * null} for NAME and CLASS subtypes so JacksonSchemaModule handles them.
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
                                    // NAME and CLASS are handled by JacksonSchemaModule
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
                    // Strip base package prefix; fall back to FQCN if package different.
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

        private static Optional<ParentTypeInfo> findAncestorTypeInfo(final Class<?> cls) {
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
                        .toList();
            }

            // Fall back to mapper-registered subtypes; deduplicate and sort for stability.
            final SerializationConfig config = mapper.serializationConfig();
            final AnnotatedClass annotatedClass =
                    config.classIntrospectorInstance()
                            .introspectClassAnnotations(mapper.constructType(erasedType));
            final Collection<NamedType> registered =
                    config.getSubtypeResolver()
                            .collectAndResolveSubtypesByClass(config, annotatedClass);

            return registered.stream()
                    .filter(nt -> !erasedType.equals(nt.getType()))
                    .sorted(Comparator.comparing(nt -> nt.getType().getName()))
                    .map(nt -> typeContext.resolve(nt.getType()))
                    .toList();
        }
    }

    private record Mapping(
            String jsonType,
            Optional<String> format,
            Optional<String> pattern,
            Optional<Long> minimum,
            Optional<Long> maximum) {

        private Mapping {
            requireNonBlank(jsonType, "jsonType");
            requireNonNull(format, "format");
            format.ifPresent(f -> requireNonBlank(f, "format"));
            requireNonNull(pattern, "pattern");
            pattern.ifPresent(p -> requireNonBlank(p, "pattern"));
            requireNonNull(minimum, "minimum");
            requireNonNull(maximum, "maximum");
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
    }
}
