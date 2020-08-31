package org.zhd.purchase.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

import com.baomidou.mybatisplus.annotation.*;

/**
 * 采购计划
 *
 * @author samy
 */
@Data
@Entity
@Table(name="t_purchase_plan")
@TableName(value="t_purchase_plan")
public class PurchasePlan implements Serializable {
    /**
     * 序列号
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 单据号(根据规则自动生成)
     */
    private String code;
    /**
     * 集团编号
     */
    private String memberCode;
    /**
     * 计划日期
     */
    private Date planDate;
    /**
     * 计划月份
     */
    private Date planMonth;
    /**
     * 计划数值
     */
    private String planValue;
    /**
     * 计划类型(与原表重复，暂时的解决方案)
     */
    private String planType;
    /**
     * 计划类型(业务类别)
     */
    private String type;
    /**
     * 采购类型
     */
    private String purchaseType;
    /**
     * 结算方式
     */
    private String settleType;
    /**
     * 付款方式
     */
    private String payType;
    /**
     * 供货编号
     */
    private String companyCode;
    /**
     * 员工编号
     */
    private String employeeCode;
    /**
     * 制单人编号
     */
    private String accountCode;
    /**
     * 制单人名称
     */
    private String accountName;
    /**
     * 机构编号
     */
    private String orgCode;
    /**
     * 部门编号
     */
    private String dptCode;
    /**
     * 销售计划编号(引入销售计划不能为空)
     */
    private String salePlanCode;
    /**
     * 数量
     */
    private Integer count;
    /**
     * 重量
     */
    private Double weight;
    /**
     * 含税金额
     */
    private Double taxAmount;
    /**
     * 审核状态(1 未审核 2 已审 3 在审 -1 弃审)
     */
    private Integer auditStatus;
    /**
     * 审核人
     */
    private String auditCode;
    /**
     * 审核人
     */
    private String auditName;
    /**
     * 审核时间
     */
    private Date auditDate;
    /**
     * 审核备注
     */
    private String auditRemark;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建时间(系统自动记录)
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createAt;
    /**
     * 更新时间(系统自动记录)
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateAt;
    /**
     * 业务单号
     */
    private String billTypeCode;
    /**
     * 流程ID
     */
    private String workflowId;
}