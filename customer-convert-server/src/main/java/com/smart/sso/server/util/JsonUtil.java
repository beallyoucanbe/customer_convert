package com.smart.sso.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {
    private static ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    static {
        // 容忍json中出现未知的列
        DEFAULT_OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 兼容java中的驼峰的字段名命名
        DEFAULT_OBJECT_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    public static <T> T readValue(String content, TypeReference<T> valueTypeRef) {
        try {
            if (content == null) {
                return null;
            }
            return DEFAULT_OBJECT_MAPPER.readValue(content, valueTypeRef);
        } catch (Exception e) {
            log.error("failed to parse content to list. [type={}]", valueTypeRef.getType(), e);
            throw new RuntimeException("failed to read value by specified type", e);
        }
    }

    public static String serialize(Object object) {
        String serialization = null;
        try {
            serialization = DEFAULT_OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("failed to serialize object", e);
        }
        return serialization;
    }

}
