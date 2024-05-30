package com.smart.sso.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.sso.server.model.FeatureContent;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FeatureContentTypeHandler extends BaseTypeHandler<FeatureContent> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, FeatureContent parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, convertToJson(parameter));
    }

    @Override
    public FeatureContent getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convertToFeatureContent(rs.getString(columnName));
    }

    @Override
    public FeatureContent getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convertToFeatureContent(rs.getString(columnIndex));
    }

    @Override
    public FeatureContent getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convertToFeatureContent(cs.getString(columnIndex));
    }

    private String convertToJson(FeatureContent featureContent) {
        try {
            return objectMapper.writeValueAsString(featureContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert FeatureContent to JSON", e);
        }
    }

    private FeatureContent convertToFeatureContent(String json) {
        try {
            return objectMapper.readValue(json, FeatureContent.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON to FeatureContent", e);
        }
    }
}

