package com.smart.sso.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.smart.sso.server.model.CommunicationContent;
import org.apache.ibatis.type.JdbcType;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CommunicationContentTypeHandler extends JsonTypeHandler<List<CommunicationContent>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<CommunicationContent> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, convertToJson(parameter));
    }

    @Override
    public List<CommunicationContent> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(rs.getString(columnName));
    }

    @Override
    public List<CommunicationContent> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(rs.getString(columnIndex));
    }

    @Override
    public List<CommunicationContent> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(cs.getString(columnIndex));
    }

    private String convertToJson(List<CommunicationContent> featureContent) {
        try {
            return objectMapper.writeValueAsString(featureContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert FeatureContent to JSON", e);
        }
    }

    private List<CommunicationContent> convertToFeatureContent(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<CommunicationContent>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON to FeatureContent", e);
        }
    }

}