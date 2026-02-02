package org.example;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com_uti.Util;

import java.io.File;
import java.sql.*;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com_uti.Util.*;

public class MssqlSngThrdTstV2 {


    public static void main(String[] args) {
        try {
            runTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static   String sqlInsert =getIstSql();

    private static String getIstSql() {


        return  readFil("c:\\cfg\\instsql.sql");
    }


    //    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");


    /**
     * 目标是：
     *
     * 本机多线程
     *
     * 所有请求打到同一个 User_Id
     *
     * SQL Server 用 UPDLOCK 排队
     *
     * 测试“单用户在真实锁竞争下的有效 TPS”
     *

     *
     * ✔ 多线程制造压力
     * ✔ 每个线程独立连接
     * ✔ 悲观锁让 SQL Server 串行
     * ✔ commit 触发真实事务
     * ✔ counter 统计成功数
     * ✔ awaitTermination 等待所有线程
     * ✔ 用 counter 计算 TPS
     * ✔ 关闭连接池
     * @throws Exception
     */
    public static void runTest() throws Exception {

        int totalRequests = 2_00;
        System.out.println("totalRequests="+totalRequests);
        ExecutorService pool = getExecutorService();


        HikariDataSource ds = getHikariDataSource();
        AtomicInteger counter = new AtomicInteger();
       // int iterations = 100;


        long start = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            pool.submit(() -> {
                try (Connection conn = ds.getConnection()) {
                    conn.setAutoCommit(false);

                   //  悲观锁占位（可选）
//                    try (PreparedStatement lockStmt = conn.prepareStatement(
//                            "SELECT 1 FROM   WITH (UPDLOCK, ROWLOCK) WHERE User_Id = ?")) {
//                        lockStmt.setString(1, "6938FC6F-4036-9901-0018-12127CF4B40D");
//                        lockStmt.executeQuery();
//                    }

//                    Object lock = getLock("6938FC6F-4036-9901-0018-12127CF4B40D");
//                    synchronized (lock) { // 这里的代码对同一个 userId 串行执行
//                        // doDatabaseWork(userId);
//                        try (PreparedStatement stmt = conn.prepareStatement(  sqlInsert)) {
//                            stmt.executeUpdate();
//                        }
//
//                       // ⭐ 必须在锁内
//                        conn.commit();
//                        counter.incrementAndGet();
//                     }

                    execSql(conn," EXEC dbo.instSameUid;") ;


                    conn.commit();
                    counter.incrementAndGet();


                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
// ⭐ 必须等待线程池执行完，否则 TPS 不准
        pool.shutdown(); pool.awaitTermination(1, TimeUnit.HOURS);

        long end = System.currentTimeMillis();
        double elapsedSec = (end - start) / 1000.0;
        // ⭐ 必须用 counter，而不是 totalRequests
        double tps = counter.get() / elapsedSec;



        System.out.println("Elapsed: " + elapsedSec + " sec");
        System.out.println("Total inserts: " + counter.get());
        System.out.println("TPS (single thread): " + tps);

        ds.close();

    }



    private static HikariDataSource getHikariDataSource() throws Exception {
        String configPath = "C:\\cfg\\db.json";
        Util.DbConfig cfg = loadConfig(configPath);

        String connectionString =
                "jdbc:sqlserver://" + cfg.server +
                        ";databaseName=" + cfg.database +
                        ";user=nmluser;password=" + cfg.password +
                        ";trustServerCertificate=true;";

        System.out.println(connectionString);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionString);
        config.setUsername("sa");
        config.setPassword(cfg.password);
        config.setMaximumPoolSize(100);
        config.setMinimumIdle(70);

        HikariDataSource ds = new HikariDataSource(config);
        return ds;
    }
}
