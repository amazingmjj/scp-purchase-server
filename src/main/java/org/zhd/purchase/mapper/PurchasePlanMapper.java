package org.zhd.purchase.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xy.api.dto.purchase.PurchasePlanDto;
import org.zhd.purchase.entity.PurchasePlan;

import java.util.List;

/**
* 采购计划数据层
*
* @author samy
*/
@Mapper
public interface PurchasePlanMapper extends BaseMapper<PurchasePlan> {

    /**
     * 分页查询
     * @param page
     * @param qw
     * @return
     */
    @Select("select t.* from v_t_purchase_plan t  ${ew.customSqlSegment} ")
    public IPage<PurchasePlanDto> selectByPage(IPage<PurchasePlan> page,@Param(Constants.WRAPPER) QueryWrapper<PurchasePlanDto> qw);


    /**
     * 条件查询
     * @param qw
     * @return
     */
    @Select("select t.* from v_t_purchase_plan t  ${ew.customSqlSegment} ")
    public List<PurchasePlanDto> selectByParam(@Param(Constants.WRAPPER) QueryWrapper<PurchasePlanDto> qw);
}