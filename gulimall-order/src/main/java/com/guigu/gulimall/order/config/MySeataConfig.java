package com.guigu.gulimall.order.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

//seata来代理我们的数据源
@Configuration
public class MySeataConfig {
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

}
