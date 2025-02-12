package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class DoubtInfo {

    @TableId
    private Integer id;
    private String normDoubt;
    private String summary;
    private LocalDateTime updateTime;
    private String ext1;
    private String ext2;
}
