package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MasterCourseFeature extends BaseFeature {
    private Integer process = 0;
    private Integer total = 7;
    private Integer correct = 0;
    private CustomerFeatureResponse.RecordContent records;
    private CustomerFeatureResponse.RecordContent correctRecords;
    private List<String> questionTags;
    private CustomerFeatureResponse.RecordContent questionRecords;

    public MasterCourseFeature(BaseFeature baseFeature) {
        // 复制父类属性
        this.setInquired(baseFeature.getInquired());
        this.setInquiredOriginChat(baseFeature.getInquiredOriginChat());
        this.setCustomerConclusion(baseFeature.getCustomerConclusion());
        this.setCustomerQuestion(baseFeature.getCustomerQuestion());
    }
}
