package org.example;

import java.sql.*;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SqliteWriteTest {

    public static void main(String[] args) throws Exception {

        // SQLite JDBC 连接
        Connection conn = DriverManager.getConnection("jdbc:sqlite:orders3.db");
        Statement stmt = conn.createStatement();

        // 创建表
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS ords (
                id INTEGER PRIMARY KEY,
                k TExt  NOT NULL,
                v TEXT NOT NULL
            )
        """);

        openModeFastInsert(conn);






        ObjectMapper mapper = new ObjectMapper(); // 用于 JSON 序列化
        conn.setAutoCommit(false);
        PreparedStatement ps = conn.prepareStatement("INSERT INTO ords (k, v) VALUES (?, ?)");
        SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);




        int N = 50_0000; // 写入数量
        long start = System.nanoTime();


       for (int i = 1; i <= N; i++) {
            //开始事务
           // conn.beginTrans();
            // 构造订单对象
            Map<String, Object> order = new HashMap<>();
            order.put("order_id", UUID.randomUUID().toString());
            order.put("merchant_id", "M1001");
            order.put("user_id", "U" + i);
            order.put("amount", i * 10);
            order.put("timestamp", System.currentTimeMillis() / 1000.0);

            String k = (String) order.get("order_id");
            String v = mapper.writeValueAsString(order);

            // 写入 SQLite
            ps.setString(1, k);
            ps.setString(2, v);
            ps.executeUpdate();


            conn.commit();

            if (i % 1000 == 0) {
                System.out.println("写入订单 " + i + ": " + k);
            }
        }

        long end = System.nanoTime();

        double elapsedSec = (end - start) / 1_000_000_000.0;
        double tps = N / elapsedSec;

        System.out.printf("写入 %d 条订单，总耗时: %.4f 秒%n", N, elapsedSec);
        System.out.printf("平均 TPS: %.2f 条/秒%n", tps);

        conn.close();
        System.out.println("完成写入");
    }


    /**
     * synchronous = OFF 会导致断电丢数据
     * 适合压测，不适合生产。
     *
     * WAL 是安全的
     * 生产环境也可以用。
     *
     *  7000tps
     */
    //// ⭐ 启用 WAL + 关闭同步（性能提升关键）
    private static void openModeFastInsert(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("PRAGMA journal_mode = WAL;");
        stmt.execute("PRAGMA synchronous = OFF;");
     //   stmt.execute(" PRAGMA locking_mode = EXCLUSIVE;");


        stmt.close();

    }
}
