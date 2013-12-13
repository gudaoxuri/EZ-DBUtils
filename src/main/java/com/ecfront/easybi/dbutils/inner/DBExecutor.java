package com.ecfront.easybi.dbutils.inner;

import com.ecfront.easybi.dbutils.exchange.Page;
import com.ecfront.easybi.dbutils.inner.dialect.Dialect;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DBExecutor {

    public static final QueryRunner queryRunner = new QueryRunner();

    public static <E> E get(String sql, Object[] params, Class<E> clazz, Connection conn) {
        E object = null;
        try {
            if (params == null) {
                object = (E) queryRunner.query(conn, sql, new BeanHandler(clazz));
            } else {
                object = (E) queryRunner.query(conn, sql, new BeanHandler(clazz), params);
            }
        } catch (SQLException e) {
            logger.error("Get error : " + sql, e);
        }
        return object;
    }

    public static <E> List<E> find(String sql, Object[] params, Class<E> clazz, Connection conn) {
        List<E> list = null;
        try {
            if (null == params) {
                list = (List<E>) queryRunner.query(conn, sql, new BeanListHandler(clazz));
            } else {
                list = (List<E>) queryRunner.query(conn, sql, new BeanListHandler(clazz), params);
            }
        } catch (SQLException e) {
            logger.error("Find error : " + sql, e);
        }
        return list;
    }

    public static <E> Page<E> find(String sql, Object[] params, long pageNumber, long pageSize, Class<E> clazz, Connection conn, Dialect dialect) {
        Page<E> page = new Page<E>();
        String pagedSql = dialect.paging(sql, pageNumber, pageSize);
        page.pageNumber = pageNumber;
        page.pageSize = pageSize;
        page.recordTotal = count(pagedSql, params, conn);
        page.pageTotal = (page.recordTotal + pageSize - 1) / pageSize;
        page.objects = find(pagedSql, params, clazz, conn);
        return page;
    }

    public static Map<String, Object> get(String sql, Object[] params, Connection conn) {
        Map<String, Object> map = null;
        try {
            if (null == params) {
                map = queryRunner.query(conn, sql, new MapHandler());
            } else {
                map = queryRunner.query(conn, sql, new MapHandler(), params);
            }
        } catch (SQLException e) {
            logger.error("Get error : " + sql, e);
        }
        return map;
    }

    public static List<Map<String, Object>> find(String sql, Object[] params, Connection conn) {
        List<Map<String, Object>> list = null;
        try {
            if (null == params) {
                list = queryRunner.query(conn, sql, new MapListHandler());
            } else {
                list = queryRunner.query(conn, sql, new MapListHandler(), params);
            }
        } catch (SQLException e) {
            logger.error("Find error : " + sql, e);
        }
        return list;
    }

    public static Page<Map<String, Object>> find(String sql, Object[] params, long pageNumber, long pageSize, Connection conn, Dialect dialect) {
        Page<Map<String, Object>> page = new Page<Map<String, Object>>();
        String pagedSql = dialect.paging(sql, pageNumber, pageSize);
        page.pageNumber = pageNumber;
        page.pageSize = pageSize;
        page.recordTotal = count(pagedSql, params, conn);
        page.pageTotal = (page.recordTotal + pageSize - 1) / pageSize;
        page.objects = find(pagedSql, params, conn);
        return page;
    }

    public static long count(String sql, Connection conn) {
        return count(sql, null, conn);
    }

    public static long count(String sql, Object[] params, Connection conn) {
        try {
            if (null == params) {
                return (Integer) queryRunner.query(conn, sql, scalarHandler);
            } else {
                return (Integer) queryRunner.query(conn, sql, scalarHandler, params);
            }
        } catch (SQLException e) {
            logger.error("Count error : " + sql, e);
        }
        return -1;
    }

    public static void update(String sql, Object[] params, Connection conn) {
        try {
            if (null == params) {
                queryRunner.update(conn, sql);
            } else {
                queryRunner.update(conn, sql, params);
            }
        } catch (SQLException e) {
            logger.error("Update error : " + sql, e);
        }
    }

    public static void batch(String sql, Object[][] params, Connection conn) {
        try {
            queryRunner.batch(conn, sql, params);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("Batch error : " + sql, e);
        }
    }

    private static ScalarHandler scalarHandler = new ScalarHandler() {
        @Override
        public Object handle(ResultSet rs) throws SQLException {
            Object obj = super.handle(rs);
            if (obj instanceof BigInteger)
                return ((BigInteger) obj).longValue();
            return obj;
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(DBExecutor.class);
}