package org.zhd.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 采购计划明细
 *
 * @author samy
 */
@Data
@Entity
@Table(name="t_purchase_plan_detail")
@TableName("t_purchase_plan_detail")
public class PurchasePlanDetail implements Serializable {
    /**
     * 序列号
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 集团编号
     */
    private String memberCode;
    /**
     * 明细编号(根据规则自动生成)
     */
    private String code;
    /**
     * 采购计划编号
     */
    private String parentCode;
    /**
     * 品名大类
     */
    private String goodsParentName;
    /**
     * 品名
     */
    private String goodsName;
    /**
     * 规格
     */
    private String goodsSpec;
    /**
     * 材质
     */
    private String goodsMaterial;
    /**
     * 产地
     */
    private String goodsArea;
    /**
     * 重量范围
     */
    private String goodsWeightRange;
    /**
     * 公差范围
     */
    private String goodsToleranceRange;

    /**
     * 长度
     */
    private String goodsLength;
    /**
     * 米重
     */
    private String goodsPerWeight;
    /**
     * 数量
     */
    private Integer count;
    /**
     * 重量
     */
    private Double weight;
    /**
     * 含税单价
     */
    private Double taxUnitPrice;
    /**
     * 含税金额
     */
    private Double taxAmount;
    /**
     * 备注
     */
    private String remark;
    /**
     * 业务单号
     */
    private String billTypeCode;
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
     * dataFlag
     * 操作标识
     * 2 删除
     */
    @Transient
    @TableField(exist = false)
    private Integer dataFlag;

    @Transient
    public Integer getDataFlag() {
        return 1;
    }

    public void setDataFlag(Integer dataFlag) {
        this.dataFlag = dataFlag;
    }
}