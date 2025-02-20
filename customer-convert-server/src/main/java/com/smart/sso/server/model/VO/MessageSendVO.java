package com.smart.sso.server.model.VO;

import com.smart.sso.server.model.TextMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageSendVO {
    private String sendUrl;
    private String activityId;
    private String messageType;
    private TextMessage TextMessage;
}
