package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityInfoWithVersion extends ActivityInfo{
    private Boolean old = Boolean.FALSE;

    public ActivityInfoWithVersion(ActivityInfo activityInfo){
        this.setActivityName(activityInfo.getActivityName());
        this.setActivityId(activityInfo.getActivityId());
    }
}
