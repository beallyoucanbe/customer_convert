package com.smart.sso.server.common;

import lombok.Data;

@Data
public class PageRequest {

    private long page = 0;
    private long limit = 10;
    private String sortBy;
    private String order;
}
