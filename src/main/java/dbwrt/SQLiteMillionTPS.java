package dbwrt;

import java.sql.*;

public class SQLiteMillionTPS {

    static final String DB_URL = "jdbc:sqlite:c:/db/test.db";

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");

        Connection conn = DriverManager.getConnection(DB_URL);


        try (Statement st = conn.createStatement()) {
            // 极限 PRAGMA
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA synchronous=OFF;");
            st.execute("PRAGMA temp_store=MEMORY;");
            st.execute("PRAGMA locking_mode=EXCLUSIVE;");
            st.execute("PRAGMA cache_size=-200000;"); // ~200MB
        }
        // ✅ PRAGMA 完成后，再关闭 autoCommit
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    v INTEGER
                );
            """);
        }

        String sql = "INSERT INTO log(v) VALUES(?)";
        PreparedStatement ps = conn.prepareStatement(sql);

        final int TOTAL = 10_0_000;
        final int BATCH = 1;

        long start = System.nanoTime();
        int count = 0;

        for (int i = 1; i <= TOTAL; i++) {
            ps.setInt(1, i);
            ps.addBatch();
            count++;

            if (count == BATCH) {
                ps.executeBatch();
                conn.commit();
                count = 0;
            }
        }

        if (count > 0) {
            ps.executeBatch();
            conn.commit();
        }

        long end = System.nanoTime();
        double seconds = (end - start) / 1e9;

        System.out.printf(
                "Inserted %,d rows in %.2f seconds (%.0f ops/sec)%n",
                TOTAL,
                seconds,
                TOTAL / seconds
        );

        ps.close();
        conn.close();
    }
}

