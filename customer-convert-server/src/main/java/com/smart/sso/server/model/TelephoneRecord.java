package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.CommunicationContentTypeHandler;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName(autoResultMap = true)
public class TelephoneRecord {
    @TableId
    private String id;
    private String customerId;
    private String customerName;
    private String ownerId;
    private String ownerName;
    private Integer communicationDuration;
    private Timestamp communicationTime;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeFundsVolume;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeEarningDesire;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeCourseTeacherApproval;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeSoftwareFunctionClarity;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeStockSelectionMethod;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeSelfIssueRecognition;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeLearnNewMethodApproval;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeContinuousLearnApproval;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeSoftwareValueApproval;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeSoftwarePurchaseAttitude;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeCustomerIssuesQuantified;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private CommunicationContent timeSoftwareValueQuantified;
}