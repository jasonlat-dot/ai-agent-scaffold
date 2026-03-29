
package com.jasonlat.ai.infrastructure.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoGenerator {

    // 数据库连接信息
    private static final String URL = "jdbc:mysql://127.0.0.1:13306/ai-agent-station?useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&serverTimezone=UTC&useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "lijiaqiang12@";

    private static final String outputDir =
            System.getProperty("user.dir") + "\\"  + "ai-agent-scaffold-code-infrastructure\\src\\main\\java";

    private static final String parentPacket = "com.jasonlat.ai.infrastructure";

    private static final String mapperXmlPath =
            System.getProperty("user.dir") + "\\" +  "ai-agent-scaffold-code-app\\src\\main\\resources\\mybatis\\mapper";

    public static void main(String[] args) {
        // 需要生成代码的表
        List<String> tables = new ArrayList<>();
        tables.add("ai_agent");
        tables.add("ai_agent_flow_config");
        tables.add("ai_agent_task_schedule");
        tables.add("ai_client");
        tables.add("ai_client_config");
        tables.add("ai_client_advisor");
        tables.add("ai_client_api");
        tables.add("ai_client_model");
        tables.add("ai_client_rag_order");
        tables.add("ai_client_system_prompt");
        tables.add("ai_client_tool_mcp");

        // 配置全局设置
        FastAutoGenerator.create(URL, USERNAME, PASSWORD)
                .globalConfig(builder -> {
                    builder
                            .author("jasonlat")               // 作者名称
                            .outputDir(outputDir)    // 输出目录路径（到java目录）
                            .commentDate("yyyy-MM-dd");  // 注释日期格式


                })
                // 配置包结构
                .packageConfig(builder -> {
                    builder.parent(parentPacket)  // 父包名称
                            .mapper("dao")
                            .entity("dao.po")
                            .xml("mybatis/mapper").pathInfo(Collections.singletonMap(OutputFile.xml, mapperXmlPath));  // XML文件的输出目录路径
                })
                // 核心：模板配置（禁用Controller、Service、Service实现类）
                .templateConfig(builder -> {
                    builder
                            // 禁用不需要的模板：Controller、Service、Service实现类
                            .disable(TemplateType.CONTROLLER)   // 不生成Controller
                            .disable(TemplateType.SERVICE)      // 不生成Service接口
                            .disable(TemplateType.SERVICE_IMPL); // 不生成Service实现类
                    // 保留需要的模板：Entity(PO)、Mapper接口、Mapper XML（默认启用，无需配置）
                })
                // 配置代码生成策略
                .strategyConfig(builder -> {
                    builder.addInclude(tables)
//                            .addTablePrefix("p_") // 指定数据库表名的前缀，生成代码时会自动忽略前缀部分，只生成以指定前缀后面的部分为表名的代码文件
                            .entityBuilder()
                            .formatFileName("%s")
                            .enableLombok()
                            .enableFileOverride()
                            .enableTableFieldAnnotation()  // 启用表字段注释（例如@TableField）

                            .mapperBuilder()
                            .enableBaseResultMap()  // 为所有表启用一个通用的resultMap生成
                            .formatMapperFileName("I%sDao")  // Mapper文件名格式（例如SysUserMapper）
                            .enableBaseResultMap()
                            .enableFileOverride()
                            .formatXmlFileName("%sMapper");  // XML文件名格式（例如SysUserMapper.xml）

                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker模板引擎（默认为Velocity）
                .execute();
    }

}