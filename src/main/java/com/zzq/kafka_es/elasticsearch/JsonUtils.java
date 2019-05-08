package com.zzq.kafka_es.elasticsearch;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.type.TypeReference;

import java.text.SimpleDateFormat;

public class JsonUtils {
    private static ObjectMapper mapper = new ObjectMapper();

    public JsonUtils() {
    }

    public static String obj2Str(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }

    public static <T> T str2Obj(String content, Class<T> valueType) {
        try {
            return mapper.readValue(content, valueType);
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public static JsonNode str2JsonNode(String content) {
        try {
            return mapper.readTree(content);
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }

    public static <T> T str2Obj(String content, TypeReference typeReference) {
        try {
            return mapper.readValue(content, typeReference);
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }
    }

    static {
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(DeserializationConfig.Feature.AUTO_DETECT_FIELDS, true).configure(org.codehaus.jackson.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true).configure(org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false).setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setSerializationConfig(mapper.getSerializationConfig().withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL));
    }
}
