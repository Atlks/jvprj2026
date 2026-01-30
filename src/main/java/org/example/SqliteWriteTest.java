package org.example;

import java.sql.*;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * memory表插入可以达到20w了。。。文件表5w tps
 */
public class SqliteWriteTest {

    public static void main(String[] args) throws Exception {

        // SQLite JDBC 连接
      //  Connection conn = DriverManager.getConnection("jdbc:sqlite:orders3.db");
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");

        Statement stmt = conn.createStatement();

        // 创建表
        String sqlCrtTab = """
                    CREATE TABLE IF NOT EXISTS ords (
                        id INTEGER PRIMARY KEY,
                        k TExt  NOT NULL,
                        v TEXT NOT NULL
                    )
                """;
        stmt.execute(sqlCrtTab);

        openModeFastInsert(conn);






        ObjectMapper mapper = new ObjectMapper(); // 用于 JSON 序列化
        conn.setAutoCommit(false);
        PreparedStatement ps = conn.prepareStatement("INSERT INTO ords (k, v) VALUES (?, ?)");
        SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);




        int N = 5_0000; // 写入数量
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
             if(i==1)
                 System.out.println("写入订单 " + i + ": " + k);
            if (i % 1000 == 0) {
                System.out.println("写入订单 " + i + ": " + k);
            }
        }

        long end = System.nanoTime();

        double elapsedSec = (end - start) / 1_000_000_000.0;
        double tps = N / elapsedSec;

        System.out.printf("写入 %d 条订单，总耗时: %.4f 秒%n", N, elapsedSec);
        System.out.printf("平均 TPS: %.2f 条/秒%n", tps);

       // Statement stmt = conn.createStatement();
         flush2dsk(conn,stmt, sqlCrtTab );
        conn.commit();

        conn.close();
        System.out.println("完成写入");


    }

    private static void flush2dsk(Connection conn, Statement stmt1, String sqlCrtTab2 ) throws SQLException {

        Statement stmt = conn.createStatement();
        System.out.println("fun flsdsk()");
        String sql="ATTACH DATABASE 'ordersFnl.db' AS diskdb;";
        // 3. 挂载磁盘数据库
        stmt.execute(sql);

        // 4. 在磁盘数据库中建表（如果不存在）
        stmt.execute("DROP TABLE IF EXISTS diskdb.ords;");
        conn.commit();

        String sqlCrtTab = """
                    CREATE TABLE IF NOT EXISTS diskdb.ords (
                        id INTEGER  KEY,
                        k TExt  NOT NULL,
                        v TEXT NOT NULL
                    )
                """;
        System.out.println(sqlCrtTab);
        stmt.execute(sqlCrtTab);
        conn.commit();

        // 5. 定期刷盘：把内存数据写入磁盘数据库
        String sql1="INSERT INTO diskdb.ords SELECT * FROM ords;";
        stmt.execute(sql1);
        conn.commit();
    }


    /**
     * synchronous = OFF 会导致断电丢数据
     * 适合压测，不适合生产。
     *
     * WAL 是安全的
     * 生产环境也可以用。
     *
     *  7000tps
     *
     *  synchronous=off  52tps
     *
     */
    //// ⭐ 启用 WAL + 关闭同步（性能提升关键）
    private static void openModeFastInsert(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("PRAGMA journal_mode = WAL;");
        stmt.execute("PRAGMA synchronous = OFF;");
// 设置缓存为 500MB
stmt.execute("PRAGMA cache_size = -512000;");

        stmt.execute(" PRAGMA wal_checkpoint(FULL); ");
        stmt.execute(" PRAGMA temp_store = MEMORY;");

        //   stmt.execute(" PRAGMA locking_mode = EXCLUSIVE;");


        stmt.close();

    }

    /**
     *
     * 缓存作用有限
     *
     * cache_size 和 temp_store=MEMORY 主要优化查询和临时表。
     *
     * 写入性能的瓶颈在 WAL 日志和磁盘刷盘，不在 page cache。
     */
}
