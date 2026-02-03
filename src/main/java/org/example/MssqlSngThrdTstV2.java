package org.example;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com_uti.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
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

        int totalRequests = 2_0000;
        System.out.println("totalRequests="+totalRequests);
        ExecutorService pool = getExecutorService();


        HikariDataSource ds = getHikariDataSource();
        AtomicInteger counter = new AtomicInteger();
       // int iterations = 100;


        long start = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            pool.submit(() -> {

                try (Connection conn = ds.getConnection()) {                    conn.setAutoCommit(false);
                    execSql(conn," EXEC dbo.InsertTxn;") ;
                  //  execSql(conn," EXEC dbo.instSameUid;") ;
                    conn.commit();
                    counter.incrementAndGet();
                    System.out.println("finish::"+counter.get());

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


    /**
     * 虚拟线程 + HikariCP 可以安全地开更多连接而不受本地 CPU 核心数限制。
     *
     * 本地 12 核开 300 个虚拟线程连接池是合理的。
     *
     * 数据库 64 核可以支撑几百并发写，但注意监控 IO 和锁等待。
     * HikariCP 不控制线程类型
     *
     * 想让数据库操作用虚拟线程，要用虚拟线程提交任务去 getConnection() / 执行 SQL
     * @return
     * @throws Exception
     */
    private static HikariDataSource getHikariDataSource() throws Exception {
        System.out.println("fun ds()");
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
        int maxPoolSize = getCpuCores() * 5;

      //  虚拟现成安全地开更多连接而不受本地 CPU 核心数限制。
        config.setMaximumPoolSize(300);
        config.setMinimumIdle(300);
        config.setKeepaliveTime(60_000); // 1分钟发一次保活
        config.setMaxLifetime(30 * 60_000); // 连接最长存活时间（ms），避免数据库自动关闭老连接
      //  config.setConnectionTimeout(30_000); // 获取连接超时（ms）
      //  config.setInitializationFailTimeout(-1); // 启动就检查连接失败
        // 确保启动时立即初始化所有连接
        config.setInitializationFailTimeout(-1); // 启动时如果连接失败抛异常


        HikariDataSource ds = getDataSourceV2(config);


        // HikariDataSource ds = getHikariDataSource(config);

//        //============= 预热连接池，一次性获取并释放所有连接
//        List<Connection> connections = new ArrayList<>();
//        for (int i = 0; i < config.getMaximumPoolSize(); i++) {
//            connections.add(ds.getConnection());
//            System.out.println("get conn"+i);
//        }
//// 立即释放回池
//        for (Connection conn : connections) {
//            conn.close();
//        }
        System.out.println("end fun");
        return ds;
    }

    @NotNull
    private static HikariDataSource getDataSourceV2(HikariConfig config) throws InterruptedException {
        System.out.println("fun getDataSourceV2");
        HikariDataSource ds = new HikariDataSource(config);
        // 主动触发空闲连接填充。。不需要循环手动触发
        while (ds.getHikariPoolMXBean().getIdleConnections() < config.getMinimumIdle()) {
            System.out.println("当前空闲连接: " + ds.getHikariPoolMXBean().getIdleConnections() + "，等待填满...");
            Thread.sleep(50);
        }

        HikariPoolMXBean poolProxy = ds.getHikariPoolMXBean();
        System.out.println("活跃连接: " + poolProxy.getActiveConnections());
        System.out.println("空闲连接: " + poolProxy.getIdleConnections());
        System.out.println("连接池预热完成！");
        return ds;
    }

    @NotNull
    private static HikariDataSource getHikariDataSource(HikariConfig config) throws InterruptedException {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        HikariDataSource ds = new HikariDataSource(config);

        int poolSize = config.getMaximumPoolSize();
        CountDownLatch latch = new CountDownLatch(poolSize);

        for (int i = 0; i < poolSize; i++) {
            final int idx = i;
            executor.submit(() -> {
                try (Connection conn = ds.getConnection()) {
                    System.out.println("get conn " + idx);
                    // 不做操作，直接归还
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

// 等待所有连接预热完成
        latch.await();
        executor.shutdown();
        HikariPoolMXBean poolProxy = ds.getHikariPoolMXBean();
        System.out.println("活跃连接: " + poolProxy.getActiveConnections());
        System.out.println("空闲连接: " + poolProxy.getIdleConnections());
        System.out.println("连接池预热完成！");
        return ds;
    }

}



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
