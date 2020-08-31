package org.zhd.purchase.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

import com.baomidou.mybatisplus.annotation.*;

/**
 * 采购付款申请
 *
 * @author samy
 */
@Data
@Entity
@Table(name = "t_purchase_pay_apply")
@TableName(value = "t_purchase_pay_apply")
public class PurchasePayApply implements Serializable {
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
     * 付款申请日期
     */
    private Date payDate;
    /**
     * 数量
     */
    private Integer count;
    /**
     * 重量
     */
    private Double weight;
    /**
     * 工作组
     */
    private String workGroup;
    /**
     * 部门编号
     */
    private String dptCode;
    /**
     * 部门名称
     */
    private String dptName;
    /**
     * 业务员编号
     */
    private String employeeCode;
    /**
     * 业务员名称
     */
    private String employeeName;
    /**
     * 机构编号
     */
    private String orgCode;
    /**
     * 机构名称
     */
    private String orgName;
    /**
     * 制单人编号
     */
    private String accountCode;
    /**
     * 制单人名称
     */
    private String accountName;
    /**
     * 备注
     */
    private String remark;
    /**
     * 业务单据类型
     */
    private String billTypeCode;
    /**
     * 审核状态
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
     * 审核备注
     */
    private String auditRemark;
    /**
     * 审核时间
     */
    private Date auditDate;
    /**
     * 含税总金额
     */
    private Double taxTotalAmount;
    /**
     * 无税总金额
     */
    private Double totalAmount;
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
     * 供货商编码
     */
    private String supplyCode;
    /**
     * 供应商名字
     */
    private String supplyName;
    /**
     * 未执行数量
     */
    private Integer execCount;
    /**
     * 未执行重量
     */
    private Double execWeight;
    /**
     * 付款单位编码
     */
    private String payCompanyCode;
    /**
     * 付款单位名称
     */
    private String payCompanyName;
    /**
     * 付款银行名称
     */
    private String payBankName;
    /**
     * 付款银行账号
     */
    private String payBankNo;
    /**
     * 流程ID
     */
    private String workflowId;
}