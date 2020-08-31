package org.zhd.purchase.hystrix;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.xy.api.dto.workflow.ProcessDto;
import org.xy.api.hystrix.workflow.ProcessHystrix;

/**
 * 审核流明细数据熔断层
 *
 * @author samy
 */
@Component
public class ProcessHystrixImpl extends ProcessHystrix {

    /**
     * 启动流程
     *
     * @param dto
     * @return
     */
    @Override
    public ProcessDto startIfExists(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("启动流程操作失败，该服务提供者已下线");
        return dto;
    }

    /**
     * 改单
     *
     * @param dto
     * @return
     */
    @Override
    public ProcessDto modify(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("改单流程操作失败，该服务提供者已下线");
        return dto;
    }

    /**
     * 查询流程操作人
     *
     * @param dto
     * @return
     */
    @Override
    public ProcessDto queryTaskUser(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("查询流程操作失败，该服务提供者已下线");
        return dto;
    }

    /**
     * 查询流程历史
     *
     * @param dto
     * @return
     */
    @Override
    public ProcessDto queryHistory(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("操作失败，请稍后重试");
        return dto;
    }

    @Override
    public ProcessDto queryTaskByUser(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("操作失败，请稍后重试");
        return dto;
    }

    @Override
    public ProcessDto queryProcessDiagram(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("操作失败，请稍后重试");
        return dto;
    }

    /**
     * 流程图
     *
     * @param processInstanceId
     * @return
     */
    @Override
    public ResponseEntity genProcessDiagram(String processInstanceId) {
        return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 审核
     *
     * @param dto
     * @return
     */
    @Override
    public ProcessDto audit(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("审核流程操作失败，该服务提供者已下线");
        return dto;
    }

    /**
     * 弃审
     *
     * @param dto
     * @return
     */
    @Override
    public ProcessDto unAudit(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("弃审流程操作失败，该服务提供者已下线");
        return dto;
    }

    /**
     * 删除
     *
     * @param dto
     * @return
     */
    @Override
    public ProcessDto delete(ProcessDto dto) {
        dto.setReturn_code(-1);
        dto.setMessage("删除流程操作失败，该服务提供者已下线");
        return dto;
    }
}