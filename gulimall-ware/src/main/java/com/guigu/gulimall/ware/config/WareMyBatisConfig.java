package com.guigu.gulimall.ware.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

//开启事务
@EnableTransactionManagement
@MapperScan("com.guigu.gulimall.ware.dao")
@Configuration
public class WareMyBatisConfig {

    @Autowired
    DataSourceProperties dataSourceProperties;//数据源的配置属性

    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        //HikariDataSource默认的数据源
        HikariDataSource dataSource =
                dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();//得到数据源
        if(StringUtils.hasText(dataSourceProperties.getName())){
            dataSource.setPoolName(dataSourceProperties.getName());
        }

        //这个数据源要被seata来包装
        return new DataSourceProxy(dataSource); //自定义数据源的
    }

    //引入分页插件
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
        paginationInterceptor.setOverflow(true);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setLimit(1000);
        return paginationInterceptor;
    }
}
