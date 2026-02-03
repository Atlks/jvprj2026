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
import static java.lang.Thread.sleep;

public class MssqlSngThrdTstV2 {


    public static void main(String[] args) {
        try {
            runTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String sqlInsert = getIstSql();

    private static String getIstSql() {


        return readFil("c:\\cfg\\instsql.sql");
    }


    //    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");


    /**
     * 目标是：
     * <p>
     * 本机多线程
     * <p>
     * 所有请求打到同一个 User_Id
     * <p>
     * SQL Server 用 UPDLOCK 排队
     * <p>
     * 测试“单用户在真实锁竞争下的有效 TPS”
     * <p>
     * <p>
     * <p>
     * ✔ 多线程制造压力
     * ✔ 每个线程独立连接
     * ✔ 悲观锁让 SQL Server 串行
     * ✔ commit 触发真实事务
     * ✔ counter 统计成功数
     * ✔ awaitTermination 等待所有线程
     * ✔ 用 counter 计算 TPS
     * ✔ 关闭连接池
     *
     * @throws Exception
     */
    public static void runTest() throws Exception {

        int totalRequests = 2_0000;
        System.out.println("totalRequests=" + totalRequests);
        ExecutorService pool = getExecutorService();


        HikariDataSource ds = getHikariDataSource();
        AtomicInteger counter = new AtomicInteger();
        // int iterations = 100;


        long start = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            pool.submit(() -> {

                try (Connection conn = ds.getConnection()) {
                    conn.setAutoCommit(false);
                    execSql(conn, " EXEC dbo.InsertTxn;");
                    //  execSql(conn," EXEC dbo.instSameUid;") ;
                    conn.commit();

                    //不要close，如果再次取出来可能问题了
                    //本事这个try 已经自动close了
                    counter.incrementAndGet();
                    System.out.println("finish::" + counter.get());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
// ⭐ 必须等待线程池执行完，否则 TPS 不准
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);

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
     * <p>
     * 本地 12 核开 300 个虚拟线程连接池是合理的。
     * <p>
     * 数据库 64 核可以支撑几百并发写，但注意监控 IO 和锁等待。
     * HikariCP 不控制线程类型
     * <p>
     * 想让数据库操作用虚拟线程，要用虚拟线程提交任务去 getConnection() / 执行 SQL
     *
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

//禁用 SQL Server JDBC metadata 查询（大幅加速）  连接速度会快 30%–70%。
        //去加密 加速效果：巨大（30%–60%）
        config.addDataSourceProperty("disableStatementPooling", "true");
        config.addDataSourceProperty("sendTimeAsDatetime", "true");
        config.addDataSourceProperty("sendStringParametersAsUnicode", "false");
  config.addDataSourceProperty("authenticationScheme", "nativeAuthentication");
        config.addDataSourceProperty("encrypt", "false"); // 内网可关
        config.addDataSourceProperty("encrypt", "false");
        config.addDataSourceProperty("trustServerCertificate", "true");
        config.addDataSourceProperty("transparentNetworkIPResolution", "false");
       //⭐ 6. 禁用 MultiSubnetFailover（如果你没有 AG）
        config.addDataSourceProperty("multiSubnetFailover", "false");
        config.addDataSourceProperty("columnEncryptionSetting", "Disabled");
        config.addDataSourceProperty("loginTimeout", "5");



        //  虚拟现成安全地开更多连接而不受本地 CPU 核心数限制。
        config.setMaximumPoolSize(200);
        config.setMinimumIdle(200);
        config.setKeepaliveTime(60_000); // 1分钟发一次保活
        config.setMaxLifetime(30 * 60_000); // 连接最长存活时间（ms），避免数据库自动关闭老连接
          config.setConnectionTimeout(3000_000); // 获取连接超时（ms）
        //  config.setInitializationFailTimeout(-1); // 启动就检查连接失败
        // 确保启动时立即初始化所有连接
        config.setInitializationFailTimeout(-1); // 启动时如果连接失败抛异常


        HikariDataSource ds = getDataSourceV2(config);


        //  HikariDataSource ds = getHikariDataSource(config);

//
        System.out.println("end fun");
        return ds;
    }


    @NotNull
    private static HikariDataSource getDataSourceV2(HikariConfig config) throws InterruptedException, SQLException {
        System.out.println("fun getDataSourceV2");
        HikariDataSource ds = new HikariDataSource(config);


//============= 预热连接池，一次性获取并释放所有连接
        int warmCount = config.getMinimumIdle();
        List<Connection> connections = new ArrayList<>();
        for (int i = 0; i < warmCount; i++) {
            connections.add(ds.getConnection());
            System.out.println("get conn" + i);
        }

        // 立即释放回池
        for (Connection conn : connections) {
            conn.close();
        }


        HikariPoolMXBean poolProxy = ds.getHikariPoolMXBean();
        //  但 Hikari 的状态更新是异步的，可能还没刷新。
        //你 sleep(2000) 其实没意义，因为 Hikari 的 housekeeping 默认 30 秒才跑一次
        sleep(2000);
        System.out.println("活跃连接: " + poolProxy.getActiveConnections());
        System.out.println("空闲连接: " + poolProxy.getIdleConnections());
        System.out.println("连接池预热完成！");
        return ds;
    }


    /**
     *  这个方式只是建议模式，可能ds不自动创建，导致死循环不能退出，应该采用强制for conn方式
     *
     *  // 主动触发空闲连接填充。。不需要循环手动触发
     *
     *             long start = System.currentTimeMillis();
     *             long timeout = 120_000; // 10 秒
     *         while (ds.getHikariPoolMXBean().getIdleConnections() < config.getMinimumIdle()) {
     *
     *             if (System.currentTimeMillis() - start > timeout) {
     *                 System.out.println("预热超时，跳过");
     *                 break;
     *             }
     *             System.out.println("当前空闲连接: " + ds.getHikariPoolMXBean().getIdleConnections() + "，等待填满...");
     *             sleep(50);
     *         }
     */

    /**
     * HikariCP 的连接创建本身是同步的，虚拟线程只是让你能开很多线程，但底层还是串行创建连接。
     * <p>
     * 你开 poolSize 个虚拟线程并不会让 Hikari 更快建立连接。
     *
     * @param config
     * @return
     * @throws InterruptedException
     * @throws SQLException
     */
    @NotNull
    private static HikariDataSource getHikariDataSourceDep(HikariConfig config) throws InterruptedException, SQLException {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        HikariDataSource ds = new HikariDataSource(config);

        int poolSize = config.getMaximumPoolSize();
        CountDownLatch latch = new CountDownLatch(poolSize);
        List<Connection> connections = new CopyOnWriteArrayList<>();

        for (int i = 0; i < poolSize; i++) {
            final int idx = i;
            int finalI = i;
            executor.submit(() -> {

                //Connection conn = ds.getConnection();
                try {
                    connections.add(ds.getConnection());
                } catch (SQLException e) {
                    e.printStackTrace();
                    // throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
                System.out.println("get conn" + finalI);

            });
        }

// 等待所有连接预热完成
        latch.await();
        executor.shutdown();
//executor.awaitTermination(...)

        // 释放回池
        for (Connection conn : connections) {
            conn.close();
        }
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
