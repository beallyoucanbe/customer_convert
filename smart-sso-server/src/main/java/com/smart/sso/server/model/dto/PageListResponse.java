package com.smart.sso.server.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data

public class PageListResponse<T> implements Serializable {
    private List<T> data;
    private long total;
    private int offset;
    private int limit;
}
