package org.zhd.purchase.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

import com.baomidou.mybatisplus.annotation.*;

/**
* 采购发货集港明细
*
* @author samy
*/
@Data
@Entity
@Table(name = "t_purchase_ship_detail")
@TableName(value = "t_purchase_ship_detail")
public class PurchaseShipDetail implements Serializable{
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
    * 采购发货集港编号
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
    * 理重
    */
    private Double assistWeight;
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
    * 业务单据类型
    */
    private String billTypeCode;
    /**
    * 计量方式
    */
    private String measure;
    /**
    * 磅计单价
    */
    private Double unitPrice;
    /**
    * 理计单价
    */
    private Double unitAssistPrice;
    /**
    * 含税磅计单价
    */
    private Double taxUnitPrice;
    /**
    * 含税理计单价
    */
    private Double taxUnitAssistPrice;
    /**
    * 金额
    */
    private Double amount;
    /**
    * 含税金额
    */
    private Double taxAmount;
    /**
    * 件数
    */
    private Double branch;
    /**
    * 支数
    */
    private Double piece;
    /**
    * 支件数
    */
    private Double unitNum;
    /**
    * 数量单位
    */
    private String countUnit;
    /**
    * 重量单位
    */
    private String weightUnit;
    /**
    * 税率
    */
    private Double taxRate;
    /**
    * 税额
    */
    private Double taxRateAmount;
    /**
     * 运输方式
     */
    private String transType;
    /**
     * 运输起点
     */
    private String transStartArea;
    /**
     * 运输终点
     */
    private String transEndArea;
    /**
     * 专用线
     */
    private String specialLine;
    /**
     * 中转港
     */
    private String transHarbor;
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
    /**
     * 未执行理重
     */
    private Double execAssistWeight;
    /**
    * 验收数量
    */
    private Integer acceptCount;
    /**
    * 验收重量
    */
    private Double acceptWeight;
    /**
     * 验收理重
     */
    private Double acceptAssistWeight;
    /**
    * 发票数量
    */
    private Integer billCount;
    /**
    * 发票重量
    */
    private Double billWeight;
    /**
    * 发票金额
    */
    private Double billAmount;
    /**
    * 是否款齐(1 未款齐 2 款齐)
    */
    private Integer finishPay;
    /**
    * 是否票齐(1 未齐 2 票齐)
    */
    private Integer finishBill;
    /**
    * 公差
    */
    private Double tolerance;
    /**
    * 是否货齐(1 未齐 2 货齐)
    */
    private Integer finishGoods;
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
     * 变更理重(虚拟)
     */
    @Transient
    @TableField(exist = false)
    private Double changeAssistWeight;

    @Transient
    public Double getChangeAssistWeight() {
        return changeAssistWeight;
    }

    public void setChangeAssistWeight(Double changeAssistWeight) {
        this.changeAssistWeight = changeAssistWeight;
    }
}