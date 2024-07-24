package com.smart.sso.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.smart.sso.server.model.FeatureContent;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class FeatureContentTypeHandler extends BaseTypeHandler<List<FeatureContent>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<FeatureContent> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, convertToJson(parameter));
    }

    @Override
    public List<FeatureContent> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(rs.getString(columnName));
    }

    @Override
    public List<FeatureContent> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(rs.getString(columnIndex));
    }

    @Override
    public List<FeatureContent> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(cs.getString(columnIndex));
    }

    private String convertToJson(List<FeatureContent> featureContent) {
        try {
            return objectMapper.writeValueAsString(featureContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert FeatureContent to JSON", e);
        }
    }

    private List<FeatureContent> convertToFeatureContent(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<FeatureContent>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON to FeatureContent", e);
        }
    }

    public static String convertInquiredField(String jsonString) {
        // 将字符串转换为JSON对象
        JSONObject jsonObject = JSON.parseObject(jsonString);

        // 获取inquired字段的值
        String inquiredValue = jsonObject.getString("inquired");
        Boolean inquiredBoolean = null;
        // 将inquired字段的值转换为布尔类型
        if ("yes".equalsIgnoreCase(inquiredValue)) {
            inquiredBoolean = Boolean.TRUE;
        } else if ("no".equalsIgnoreCase(inquiredValue)) {
            inquiredBoolean = Boolean.FALSE;
        }
        // 设置inquired字段的新值
        jsonObject.put("inquired", inquiredBoolean);

        // 将JSON对象转换回字符串
        return jsonObject.toJSONString();
    }

}

