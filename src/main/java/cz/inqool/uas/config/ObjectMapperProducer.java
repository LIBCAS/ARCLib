package cz.inqool.uas.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javassist.util.proxy.MethodHandler;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ObjectMapperProducer {
    private static final String JAVASSIST_FILTER_ID = "javassistFilter";

    /**
     * Produces Jackson {@link ObjectMapper} used for JSON serialization/deserialization.
     *
     * <p>
     *     {@link ObjectMapper} is used behind the scenes in Spring MVC object mapping, but can also be used
     *     by developer if serialization/deserialization is needed in place.
     * </p>
     * @param prettyPrint Should the JSON be pretty-printed
     * @param serializeNulls Should the nulls be serialized or skipped
     * @return Produced {@link ObjectMapper}
     */
    @Primary
    @Bean
    public ObjectMapper objectMapper(@Value("${json.prettyPrint:true}") Boolean prettyPrint,
                                     @Value("${json.serializeNulls:false}") Boolean serializeNulls) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        objectMapper.enable(MapperFeature.DEFAULT_VIEW_INCLUSION);

        // disable dates -> unix timestamp, because otherwise LocalDate will be serializes as array [2015, 7, 3]
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        if (prettyPrint != null && prettyPrint) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        if (serializeNulls != null && !serializeNulls) {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        objectMapper.setAnnotationIntrospector(new JavassistAnnotationIntrospector());
        objectMapper.setFilterProvider(new SimpleFilterProvider().addFilter(JAVASSIST_FILTER_ID,
                new JavassistBeanPropertyFilter()));

        return objectMapper;
    }

    private static class JavassistAnnotationIntrospector
            extends JacksonAnnotationIntrospector {

        @Override
        public Object findFilterId(Annotated a) {
            Object id = super.findFilterId(a);
            if (id == null) {
                id = JAVASSIST_FILTER_ID;
            }
            return id;
        }

    }

    /**
     * {@link SimpleBeanPropertyFilter} to filter out all bean properties used with Javaassist.
     */
    private static class JavassistBeanPropertyFilter extends SimpleBeanPropertyFilter {

        @Override
        protected boolean include(BeanPropertyWriter writer) {
            return include(writer.getSerializationType());
        }

        @Override
        protected boolean include(PropertyWriter writer) {
            return include(writer.getType());
        }

        private boolean include(JavaType type) {
            return !type.isTypeOrSubTypeOf(MethodHandler.class) &&
                    !type.isTypeOrSubTypeOf(LazyInitializer.class);
        }

    }
}
