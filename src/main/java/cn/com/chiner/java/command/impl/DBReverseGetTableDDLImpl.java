/*
 * Copyright 2019-2029 FISOK(www.fisok.cn).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.com.chiner.java.command.impl;

import cn.com.chiner.java.dialect.DBDialect;
import cn.com.chiner.java.dialect.DBDialectMatcher;
import cn.com.chiner.java.model.TableEntity;
import cn.fisok.raw.kit.JdbcKit;
import cn.fisok.raw.kit.StringKit;
import cn.fisok.sqloy.core.DBType;
import cn.fisok.sqloy.kit.DBTypeKit;
import cn.com.chiner.java.command.ExecResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : 杨松<yangsong158@qq.com>
 * @date : 2021/6/14
 * @desc : 数据库逆向，解析表清单的字段以及索引
 */
public class DBReverseGetTableDDLImpl extends AbstractDBCommand<ExecResult> {
    public ExecResult exec(Map<String, String> params) {
        super.init(params);
        String tables = params.get("tables").toUpperCase();
        if(StringKit.isBlank(tables)){
            throw new IllegalArgumentException("parameter [tables] not exists");
        }
        List<String> tableList = Arrays.stream(tables.split(","))
                .collect(Collectors.toList());


        ExecResult ret = new ExecResult();

        Connection conn = null;
        try {
            conn = JdbcKit.getConnection(driverClassName, url, username, password);
            List<TableEntity> tableEntities = fillTableEntities(conn,tableList);
            ret.setStatus(ExecResult.SUCCESS);
            ret.setBody(tableEntities);
        } catch (Exception e) {
            ret.setStatus(ExecResult.FAILED);
            ret.setBody(e.getMessage());
            logger.error("", e);
            return ret;
        }

        return ret;
    }


    /**
     * 获取所有数据表实体的字段及索引
     *
     * @param conn
     * @param tableNameList
     * @return
     */
    protected List<TableEntity> fillTableEntities(Connection conn, List<String> tableNameList) {
        List<TableEntity> tableEntities = new ArrayList<TableEntity>();

        try {
            DatabaseMetaData meta = conn.getMetaData();
            DBType dbType = DBTypeKit.getDBType(meta);
            DBDialect dbDialect = DBDialectMatcher.getDBDialect(dbType);
            String schemaPattern = dbDialect.getSchemaPattern(conn);

            for(String tableName : tableNameList){
                ResultSet rs = meta.getTables(null, schemaPattern, tableName, new String[]{"TABLE"});
                if(rs.next()){
                    TableEntity tableEntity = dbDialect.createTableEntity(conn,rs);
                    dbDialect.fillTableEntity(tableEntity,conn);
                    tableEntities.add(tableEntity);
                }
                Statement stmt = rs.getStatement();
                JdbcKit.close(stmt);
                JdbcKit.close(rs);
            }
        } catch (SQLException e) {
            logger.error("读取表清单出错", e);
            throw new RuntimeException("读取表清单出错|"+e.getMessage(),e);
        }

        return tableEntities;
    }
}
