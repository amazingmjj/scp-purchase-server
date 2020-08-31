package org.zhd.purchase.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

import com.baomidou.mybatisplus.annotation.*;

/**
 * 采购询价明细
 *
 * @author samy
 */
@Data
@Entity
@Table(name = "t_purchase_query_detail")
@TableName(value = "t_purchase_query_detail")
public class PurchaseQueryDetail implements Serializable {
    /**
     * 序列号
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 明细编号(根据规则自动生成)
     */
    private String code;
    /**
     * 采购询价编号
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
     * 集团编号
     */
    private String memberCode;
    /**
     * 用于页面增删改(虚拟字段)
     */
    @Transient
    @TableField(exist = false)
    private Integer dataFlag;

    @Transient
    public Integer getDataFlag() {
        return dataFlag;
    }

    public void setDataFlag(Integer dataFlag) {
        this.dataFlag = dataFlag;
    }

    /**
     * 变更数量(虚拟)
     */
    @Transient
    @TableField(exist = false)
    private Integer changeNum;

    @Transient
    public Integer getChangeNum() {
        return changeNum;
    }

    public void setChangeNum(Integer changeNum) {
        this.changeNum = changeNum;
    }

    /**
     * 变更重量(虚拟)
     */
    @Transient
    @TableField(exist = false)
    private Double changeWeight;

    @Transient
    public Double getChangeWeight() {
        return changeWeight;
    }

    public void setChangeWeight(Double changeWeight) {
        this.changeWeight = changeWeight;
    }

    /**
     * 业务单据类型
     */
    private String billTypeCode;
    /**
     * 计量方式
     */
    private String measure;
    /**
     * 供货商编码
     */
    private String supplyCode;
    /**
     * 供应商名字
     */
    private String supplyName;
    /**
     * 单价
     */
    private Double unitPrice;
    /**
     * 金额
     */
    private Double amount;
    /**
     * 到货周期
     */
    private String arrivalPeriod;
    /**
     * 付款周期
     */
    private String payPeriod;
    /**
     * 开票周期
     */
    private String billPeriod;
    /**
     * 外来编号
     */
    private String outerCode;
    /**
     * 外来父类编号
     */
    private String outerParentCode;
    /**
     * 未执行数量
     */
    private Integer execCount;
    /**
     * 未执行重量
     */
    private Double execWeight;
}