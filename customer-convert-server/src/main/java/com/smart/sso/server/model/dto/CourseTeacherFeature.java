package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class CourseTeacherFeature extends BaseFeature {
    private Boolean teacherProfession;
    private OriginChat teacherProfessionChat ;

    public CourseTeacherFeature(BaseFeature baseFeature) {
        // 复制父类属性
        this.setInquired(baseFeature.getInquired());
        this.setInquiredOriginChat(baseFeature.getInquiredOriginChat());
        this.setCustomerConclusion(baseFeature.getCustomerConclusion());
        this.setCustomerQuestion(baseFeature.getCustomerQuestion());
    }
}
