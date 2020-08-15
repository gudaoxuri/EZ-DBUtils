/*
 * Copyright 2020. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ecfront.dew.dbutils.test;

import com.ecfront.dew.dbutils.DewDBUtils;
import com.ecfront.dew.dbutils.dto.Meta;
import com.ecfront.dew.dbutils.dto.Page;
import com.ecfront.dew.dbutils.DewDB;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DBTest {

    private DewDB db;

    @Before
    public void before() {
        DewDBUtils.init(this.getClass().getResource("/").getPath() + File.separator + "config.yml");
        db = DewDBUtils.use("default");
    }

    @Test
    public void testCreateAndUpdate() throws SQLException, IOException {
        Map<String, String> fields = new HashMap<>();
        fields.put("id", "long");
        fields.put("name", "String");
        fields.put("age", "Int");
        fields.put("height1", "Float");
        fields.put("height2", "Double");
        fields.put("createTime", "Date");
        fields.put("asset", "BigDecimal");
        fields.put("addr", "String");
        fields.put("enable", "Boolean");
        fields.put("txt", "text");
        db.createTableIfNotExist("test", "测试表", fields, new HashMap<String, String>() {{
            put("name", "姓名");
            put("age", "年龄");
        }}, new ArrayList<String>() {{
            add("name");
        }}, new ArrayList<String>() {{
            add("name");
        }}, "id");
        Map<String, Object> values = new HashMap<>();
        values.put("id", 100);
        values.put("name", "gudaoxuri");
        values.put("age", 29);
        values.put("height1", 1.1);
        values.put("height2", 1.1d);
        values.put("asset", new BigDecimal("2.343"));
        values.put("enable", true);
        values.put("addr", "浙江杭州");
        //  values.put("createTime", new java.sql.Date());
        values.put("txt", "浙江杭州");
        db.insert("test", values);
        values.put("name", "孤岛旭日");
        db.modify("test", "id",100, values);
        Map<String, Object> res = db.get("test", "id", 100);
        Assert.assertEquals("孤岛旭日", res.get("name"));
        Assert.assertEquals(29, res.get("age"));
        Assert.assertEquals(1.1, res.get("height1"));
        Assert.assertEquals(1.1, res.get("height2"));
        Assert.assertEquals("浙江杭州", res.get("addr"));
        Assert.assertEquals("浙江杭州", res.get("txt"));
        db.delete("test", "id", 100);
        Assert.assertNull(db.get("test", "id", 100));
    }

    @Test
    public void testMeta() throws Exception {
        testCreateTable(db);
        List<Meta> metas = db.getMetaData("tuser");
        Assert.assertEquals("id", metas.get(0).getLabel());
        Meta meta = db.getMetaData("tuser", "name");
        Assert.assertEquals("name", meta.getLabel());
        testDropTable(db);
    }

    @Test
    public void testFlow() throws SQLException, IOException {
        testCreateTable(db);
        db.update("insert into tuser (id,name,password,age,asset,enable) values ( ? , ? , ? , ? , ? , ? )",
                1, "张三", "123", 22, 2333.22, true);
        db.batch("insert into tuser (id,name,password,age,asset,enable) values ( ? , ? , ? , ? , ? , ? )", new Object[][]{
                {2, "李四", "123", 22, 2333.22, true},
                {3, "王五1", "123", 22, 2333.22, false},
                {4, "王五2", "123", 22, 2333.22, false},
                {5, "王五3", "123", 20, 2333.22, false}
        });
        // get
        Assert.assertEquals(1, db.get("select * from tuser where id = ?", 1).get("id"));
        // count
        Assert.assertEquals(5, db.count("select * from tuser"));
        // find
        Assert.assertEquals(4, db.find("select * from tuser where age = ?", 22).size());
        // page
        Page<Map<String, Object>> pageResult = db.page("select * from tuser", 1, 2);
        Assert.assertEquals(5, pageResult.getRecordTotal());
        Assert.assertEquals(3, pageResult.getPageTotal());
        // get
        User user = db.get("select * from tuser where id = ? ", User.class, 1);
        Assert.assertEquals(1, user.getId());
        // find
        List<User> users = db.find("select * from tuser where age = ?", User.class, 22);
        Assert.assertEquals(4, users.size());

        testDropTable(db);
    }

    @Test
    public void testPool() throws Exception {
        testCreateTable(db);
        db.update("insert into tuser (id,name,password,age,asset,enable) values ( ? , ? , ? , ? , ? , ? )",
                1, "张三", "123", 22, 2333.22, true);
        db.batch("insert into tuser (id,name,password,age,asset,enable) values ( ? , ? , ? , ? , ? , ? )", new Object[][]{
                {2, "李四", "123", 22, 2333.22, true},
                {3, "王五1", "123", 22, 2333.22, false},
                {4, "王五2", "123", 22, 2333.22, false},
                {5, "王五3", "123", 20, 2333.22, false}
        });
        final CountDownLatch watch = new CountDownLatch(10000);
        final AtomicInteger count = new AtomicInteger(0);
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                for (int i1 = 0; i1 < 100; i1++) {
                    try {
                        log.debug(">>>>>>>>>>>>>>" + count.incrementAndGet());
                        watch.countDown();
                        // find
                        Assert.assertEquals(4, db.find("select * from tuser where age = ?", 22).size());
                        // page
                        Page<Map<String, Object>> pageResult = db.page("select * from tuser", 1, 2);
                        Assert.assertEquals(5, pageResult.getRecordTotal());
                        Assert.assertEquals(3, pageResult.getPageTotal());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        watch.await();
        testDropTable(db);
    }


    @Test
    public void testTransaction() throws SQLException {
        testCreateTable(db);
        //rollback test
        db.open();
        db.update("insert into tuser (id,name,password,age,asset,enable) values ( ? , ? , ? , ? , ? , ? )",
                1, "张三", "123", 22, 2333.22, true);
        db.rollback();
        Assert.assertEquals(0, db.count("select * from tuser"));

        //error test
        db.open();
        db.update("insert into tuser (id,name,password,age,asset,enable) values ( ? , ? , ? , ? , ? , ? )",
                1, "张三", "123", 22, 2333.22, true);
        //has error
        try {
            db.update("insert into tuser (id,name,password,age,asset,enable) values ( ? , ? , ? , ? , ? , ? )",
                    1, "张三", "123", 22, 2333.22);
            db.commit();
        } catch (SQLException e) {
            log.warn("[DewDBUtils]Has Error!");
        }
        Assert.assertEquals(0, db.count("select * from tuser"));

        //commit test
        db.open();
        db.update("insert into tuser (id,name,password,age,asset,enable) values ( ? , ? , ? , ? , ? , ? )",
                1, "张三", "123", 22, 2333.22, true);
        db.commit();
        Assert.assertEquals(1, db.count("select * from tuser"));

        testDropTable(db);
    }


    private void testCreateTable(DewDB db) throws SQLException {
        db.ddl("create table tuser(" +
                "id int not null," +
                "name varchar(255)," +
                "password varchar(255)," +
                "age int," +
                "asset decimal," +
                "enable boolean," +
                "primary key(id)" +
                ")");
    }

    private void testDropTable(DewDB db) throws SQLException {
        db.ddl("drop table tuser");
    }

}
