package org.zhd.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Date;

@Data
@Entity
@Table(name = "undo_log")
@TableName(value = "undo_log")
public class UndoLog implements Serializable {
    /**
     * 序列号
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long  id;

    @NotNull
    private Long branch_id;

    @NotNull
    private String xid;

    @NotNull
    private Blob rollbackInfo;

    @NotNull
    private Integer logStatus;

    @NotNull
    private Date logCreated;

    @NotNull
    private Date logModified;

    private String ext;
}