package org.zhd.purchase.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

import com.baomidou.mybatisplus.annotation.*;

/**
* 采购发货集港
*
* @author samy
*/
@Data
@Entity
@Table(name = "t_purchase_ship")
@TableName(value = "t_purchase_ship")
public class PurchaseShip implements Serializable{
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
    * 合同日期
    */
    private Date contractDate;
    /**
    * 合同月份
    */
    private Date contractMonth;
    /**
    * 对方合同编号
    */
    private String contractOtherCode;
    /**
    * 对方合同批次号
    */
    private String contractOtherBatch;
    /**
    * 对方合同代表
    */
    private String contractOtherRepresent;
    /**
    * 签约地点
    */
    private String contractArea;
    /**
    * 合同类型(1 甲 2 乙 3 丙)
    */
    private Integer contractClass;
    /**
    * 结算方式
    */
    private String settleType;
    /**
    * 付款方式
    */
    private String payType;
    /**
    * 交货方式
    */
    private String deliveryType;
    /**
    * 交货开始日期
    */
    private Date deliveryStartDate;
    /**
    * 交货结束日期
    */
    private Date deliveryEndDate;
    /**
    * 供货商编码
    */
    private String supplyCode;
    /**
    * 供应商名字
    */
    private String supplyName;
    /**
    * 结算单位编码
    */
    private String settleCompanyCode;
    /**
    * 结算单位名称
    */
    private String settleCompanyName;
    /**
    * 币种
    */
    private String currency;
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
    * 无税总金额
    */
    private Double totalAmount;
    /**
    * 含税总金额
    */
    private Double taxTotalAmount;
    /**
    * 理重
    */
    private Double assistWeight;
    /**
    * 付款数量
    */
    private Integer payCount;
    /**
    * 付款重量
    */
    private Double payWeight;
    /**
    * 付款金额
    */
    private Double payAmount;
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
    * 车皮号
    */
    private String carNo;
    /**
    * 来源(1 财务 2 手动创建)
    */
    private Integer source;
    /**
    * 未执行数量
    */
    private Integer execCount;
    /**
    * 未执行重量
    */
    private Double execWeight;
    /**
    * 采购类型
    */
    private String purchaseType;
    /**
    * 业务类别
    */
    private String busiType;
    /**
    * 日利率
    */
    private Double dayRate;
    /**
    * 付款日期
    */
    private Date payDate;
    /**
    * 预付金额
    */
    private Double prepayAmount;
    /**
    * 是否款齐(1 未款齐 2 款齐)
    */
    private Integer finishPay;
    /**
    * 是否票齐(1 未齐 2 票齐)
    */
    private Integer finishBill;
    /**
     * 流程ID
     */
    private String workflowId;
}