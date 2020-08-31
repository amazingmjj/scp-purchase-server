package org.zhd.purchase;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.extension.incrementer.OracleKeyGenerator;
import com.baomidou.mybatisplus.extension.parsers.BlockAttackSqlParser;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * mysql plus config
 *
 * @author juny
 */
@Configuration
@EnableTransactionManagement
@MapperScan({"org.zhd.purchase.mapper", "org.xy.api.mapper"})
public class MybatisPlusConfig {
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        List<ISqlParser> sqlParserList = new ArrayList<>();
        // 攻击 SQL 阻断解析器、加入解析链
        sqlParserList.add(new BlockAttackSqlParser());
        paginationInterceptor.setSqlParserList(sqlParserList);
        return paginationInterceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            private String columnUpdateKey = "updateAt";

            @Override
            public void insertFill(MetaObject metaObject) {
                Date now = new Date();
                String columnCreateKey = "createAt";
                if (metaObject.hasSetter(columnCreateKey)) {
                    this.setFieldValByName(columnCreateKey, now, metaObject);
                }
                if (metaObject.hasSetter(columnUpdateKey)) {
                    this.setFieldValByName(columnUpdateKey, now, metaObject);
                }

            }

            @Override
            public void updateFill(MetaObject metaObject) {
                if (metaObject.hasSetter(columnUpdateKey)) {
                    this.setFieldValByName(columnUpdateKey, new Date(), metaObject);
                }
            }
        };
    }
}
