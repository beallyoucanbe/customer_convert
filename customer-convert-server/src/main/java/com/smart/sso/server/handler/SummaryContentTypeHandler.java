package com.smart.sso.server.handler;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.smart.sso.server.model.SummaryContent;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SummaryContentTypeHandler extends BaseTypeHandler<List<SummaryContent>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<SummaryContent> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, convertToJson(parameter));
    }

    @Override
    public List<SummaryContent> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(rs.getString(columnName));
    }

    @Override
    public List<SummaryContent> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(rs.getString(columnIndex));
    }

    @Override
    public List<SummaryContent> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return convertToFeatureContent(cs.getString(columnIndex));
    }

    private String convertToJson(List<SummaryContent> featureContent) {
        try {
            return objectMapper.writeValueAsString(featureContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert FeatureContent to JSON", e);
        }
    }

    private List<SummaryContent> convertToFeatureContent(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<SummaryContent>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON to FeatureContent", e);
        }
    }

}

