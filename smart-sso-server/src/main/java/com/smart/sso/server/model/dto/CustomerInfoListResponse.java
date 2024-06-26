package com.smart.sso.server.model.dto;

import com.smart.sso.server.model.VO.CustomerListVO;
import lombok.Data;

import java.util.List;

@Data
public class CustomerInfoListResponse extends CommonPageList{
    private List<CustomerListVO>  customers;
}
