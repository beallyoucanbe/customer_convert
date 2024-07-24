package com.smart.sso.server.common;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PageRequest {

    private int page = 0;
    private int limit = 10;
    private String sortBy;
    private String order;

    public PageRequest(int page, int limit, String sortBy, String order) {
        this.page = page;
        this.limit = limit;
        this.sortBy = "last_update".equals(sortBy) ? "update_time" : sortBy;
        this.order = order;
    }
}
