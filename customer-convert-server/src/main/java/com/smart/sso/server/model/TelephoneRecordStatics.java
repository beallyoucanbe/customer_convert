package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TelephoneRecordStatics {

    private String customerId ;
    private String activityId;
    private Integer totalCalls;
    private Timestamp LatestCommunicationTime;
}
