package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.DateTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 公司职位信息;
 * @author : One Direction
 * @date : 2022-12-13
 */
@Data
@TableName(autoResultMap = true)
public class Job implements Serializable,Cloneable{
    /** 主键 */
    @TableId
    private String id ;
    /** 公司id */
    private String companyId ;
    /** 职位id */
    private String postId ;
    /** 生效状态 */
    @TableField(value = "enable_status", fill = FieldFill.INSERT)
    private Boolean enableStatus ;
    /** 经验要求 */
    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT, typeHandler = DateTypeHandler.class)
    private Date createTime ;
    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE, typeHandler = DateTypeHandler.class)
    private Date updateTime ;
}
