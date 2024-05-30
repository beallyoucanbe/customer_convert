package com.smart.sso.server.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommonPageList implements Serializable {
    private long total;
    private int offset;
    private int limit;
}
