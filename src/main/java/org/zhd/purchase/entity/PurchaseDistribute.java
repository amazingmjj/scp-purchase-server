package org.zhd.purchase.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

import com.baomidou.mybatisplus.annotation.*;

/**
 * 采购配货
 *
 * @author samy
 */
@Data
@Entity
@Table(name = "t_purchase_distribute")
@TableName(value = "t_purchase_distribute")
public class PurchaseDistribute implements Serializable {
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
     * 配货日期
     */
    private Date distributeDate;
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
     * 配货类型(1 销售调货 2 临时调货 3 销售合同 4 系统补货)
     */
    private String type;
    /**
     * 外来编号
     */
    private String outerCode;
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
     * 未执行数量
     */
    private Integer execCount;
    /**
     * 未执行重量
     */
    private Double execWeight;
    /**
     * 流程ID
     */
    private String workflowId;
}