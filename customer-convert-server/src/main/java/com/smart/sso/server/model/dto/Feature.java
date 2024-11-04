package com.smart.sso.server.model.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Feature {
    private String inquired = "no"; // no-need, no, yes
    private OriginChat inquiredOriginChat;
    private CustomerConclusion customerConclusion;

    @Getter
    @Setter
    public static class CustomerConclusion {
        private Object modelRecord;
        private Object salesManualTag;
        private String salesRecord;
        private String updateTime ;
        private Object compareValue;
        private OriginChat originChat;
    }

}
