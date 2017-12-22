package cz.inqool.uas.config.change.checker;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetaData;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Application wide gatherer returning all application properties from {@link ConfigurationProperties}
 * annotated beans.
 *
 * Based on ASF 2.0 licenced code from Spring Boot.
 *
 * <p>
 * To protect sensitive information from being exposed, certain property values are masked
 * if their names end with a set of configurable values (default "password" and "secret").
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Matus Zamborsky
 */
@Service
public class Gatherer implements ApplicationContextAware {

    private static final String CGLIB_FILTER_ID = "cglibFilter";

    private final Sanitizer sanitizer = new Sanitizer();

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    /**
     * Extract beans annotated {@link ConfigurationProperties} and serialize into
     * {@link Map}.
     * @return the beans
     */
    public Map<String, Object> extract() {
        // Serialize beans into map structure and sanitize values
        ObjectMapper mapper = new ObjectMapper();
        configureObjectMapper(mapper);
        return extract(context, mapper);
    }

    private Map<String, Object> extract(ApplicationContext context, ObjectMapper mapper) {
        Map<String, Object> result = new LinkedHashMap<>();
        ConfigurationBeanFactoryMetaData beanFactoryMetaData = getBeanFactoryMetaData(
                context);
        Map<String, Object> beans = getConfigurationPropertiesBeans(context,
                beanFactoryMetaData);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            String prefix = extractPrefix(context, beanFactoryMetaData, beanName, bean);
            root.put("prefix", prefix);
            root.put("properties", sanitize(prefix, safeSerialize(mapper, bean, prefix)));
            result.put(beanName, root);
        }
        if (context.getParent() != null) {
            result.put("parent", extract(context.getParent(), mapper));
        }
        return result;
    }

    private ConfigurationBeanFactoryMetaData getBeanFactoryMetaData(
            ApplicationContext context) {
        Map<String, ConfigurationBeanFactoryMetaData> beans = context
                .getBeansOfType(ConfigurationBeanFactoryMetaData.class);
        if (beans.size() == 1) {
            return beans.values().iterator().next();
        }
        return null;
    }

    private Map<String, Object> getConfigurationPropertiesBeans(
            ApplicationContext context,
            ConfigurationBeanFactoryMetaData beanFactoryMetaData) {
        Map<String, Object> beans = new LinkedHashMap<String, Object>();
        beans.putAll(context.getBeansWithAnnotation(ConfigurationProperties.class));
        if (beanFactoryMetaData != null) {
            beans.putAll(beanFactoryMetaData
                    .getBeansWithFactoryAnnotation(ConfigurationProperties.class));
        }
        return beans;
    }

    /**
     * Cautiously serialize the bean to a map (returning a map with an error message
     * instead of throwing an exception if there is a problem).
     * @param mapper the object mapper
     * @param bean the source bean
     * @param prefix the prefix
     * @return the serialized instance
     */
    private Map<String, Object> safeSerialize(ObjectMapper mapper, Object bean,
                                              String prefix) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = new LinkedHashMap<>(mapper.convertValue(bean, LinkedHashMap.class));
            return result;
        }
        catch (Exception ex) {
            return new LinkedHashMap<>(Collections.<String, Object>singletonMap(
                    "error", "Cannot serialize '" + prefix + "'"));
        }
    }

    /**
     * Configure Jackson's {@link ObjectMapper} to be used to serialize the
     * {@link ConfigurationProperties} objects into a {@link Map} structure.
     * @param mapper the object mapper
     */
    protected void configureObjectMapper(ObjectMapper mapper) {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        applyCglibFilters(mapper);
        applySerializationModifier(mapper);
    }

    /**
     * Ensure only bindable and non-cyclic bean properties are reported.
     * @param mapper the object mapper
     */
    private void applySerializationModifier(ObjectMapper mapper) {
        SerializerFactory factory = BeanSerializerFactory.instance
                .withSerializerModifier(new GenericSerializerModifier());
        mapper.setSerializerFactory(factory);
    }

    /**
     * Configure PropertyFilter to make sure Jackson doesn't process CGLIB generated bean
     * properties.
     * @param mapper the object mapper
     */
    private void applyCglibFilters(ObjectMapper mapper) {
        mapper.setAnnotationIntrospector(new CglibAnnotationIntrospector());
        mapper.setFilterProvider(new SimpleFilterProvider().addFilter(CGLIB_FILTER_ID,
                new CglibBeanPropertyFilter()));
    }

    /**
     * Extract configuration prefix from {@link ConfigurationProperties} annotation.
     * @param context the application context
     * @param beanFactoryMetaData the bean factory meta-data
     * @param beanName the bean name
     * @param bean the bean
     * @return the prefix
     */
    private String extractPrefix(ApplicationContext context,
                                 ConfigurationBeanFactoryMetaData beanFactoryMetaData, String beanName,
                                 Object bean) {
        ConfigurationProperties annotation = context.findAnnotationOnBean(beanName,
                ConfigurationProperties.class);
        if (beanFactoryMetaData != null) {
            ConfigurationProperties override = beanFactoryMetaData
                    .findFactoryAnnotation(beanName, ConfigurationProperties.class);
            if (override != null) {
                // The @Bean-level @ConfigurationProperties overrides the one at type
                // level when binding. Arguably we should render them both, but this one
                // might be the most relevant for a starting point.
                annotation = override;
            }
        }
        return (StringUtils.hasLength(annotation.value()) ? annotation.value()
                : annotation.prefix());
    }

    /**
     * Sanitize all unwanted configuration properties to avoid leaking of sensitive
     * information.
     * @param prefix the property prefix
     * @param map the source map
     * @return the sanitized map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitize(String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            String qualifiedKey = (prefix.length() == 0 ? prefix : prefix + ".") + key;
            Object value = entry.getValue();
            if (value instanceof Map) {
                map.put(key, sanitize(qualifiedKey, (Map<String, Object>) value));
            }
            else {
                value = this.sanitizer.sanitize(key, value);
                value = this.sanitizer.sanitize(qualifiedKey, value);
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Extension to {@link JacksonAnnotationIntrospector} to suppress CGLIB generated bean
     * properties.
     */
    @SuppressWarnings("serial")
    private static class CglibAnnotationIntrospector
            extends JacksonAnnotationIntrospector {

        @Override
        public Object findFilterId(Annotated a) {
            Object id = super.findFilterId(a);
            if (id == null) {
                id = CGLIB_FILTER_ID;
            }
            return id;
        }

    }

    /**
     * {@link SimpleBeanPropertyFilter} to filter out all bean properties whose names
     * start with '$$'.
     */
    private static class CglibBeanPropertyFilter extends SimpleBeanPropertyFilter {

        @Override
        protected boolean include(BeanPropertyWriter writer) {
            return include(writer.getFullName().getSimpleName());
        }

        @Override
        protected boolean include(PropertyWriter writer) {
            return include(writer.getFullName().getSimpleName());
        }

        private boolean include(String name) {
            return !name.startsWith("$$");
        }

    }

    /**
     * {@link BeanSerializerModifier} to return only relevant configuration properties.
     */
    protected static class GenericSerializerModifier extends BeanSerializerModifier {

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                         BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
            List<BeanPropertyWriter> result = new ArrayList<BeanPropertyWriter>();
            for (BeanPropertyWriter writer : beanProperties) {
                boolean readable = isReadable(beanDesc, writer);
                if (readable) {
                    result.add(writer);
                }
            }
            return result;
        }

        private boolean isReadable(BeanDescription beanDesc, BeanPropertyWriter writer) {
            String parentType = beanDesc.getType().getRawClass().getName();
            String type = writer.getPropertyType().getName();
            AnnotatedMethod setter = findSetter(beanDesc, writer);
            // If there's a setter, we assume it's OK to report on the value,
            // similarly, if there's no setter but the package names match, we assume
            // that its a nested class used solely for binding to config props, so it
            // should be kosher. This filter is not used if there is JSON metadata for
            // the property, so it's mainly for user-defined beans.
            return (setter != null) || ClassUtils.getPackageName(parentType)
                    .equals(ClassUtils.getPackageName(type));
        }

        private AnnotatedMethod findSetter(BeanDescription beanDesc,
                                           BeanPropertyWriter writer) {
            String name = "set" + StringUtils.capitalize(writer.getName());
            Class<?> type = writer.getPropertyType();
            AnnotatedMethod setter = beanDesc.findMethod(name, new Class<?>[] { type });
            // The enabled property of endpoints returns a boolean primitive but is set
            // using a Boolean class
            if (setter == null && type.equals(Boolean.TYPE)) {
                setter = beanDesc.findMethod(name, new Class<?>[] { Boolean.class });
            }
            return setter;
        }
    }

}

