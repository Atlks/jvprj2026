package org.example;



import org.rocksdb.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RocksDBWriteTest {

    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws Exception {

        // RocksDB 配置
        Options options = new Options()
                .setCreateIfMissing(true);

        // 打开 RocksDB
        try (RocksDB db = RocksDB.open(options, "rocksdb-data")) {

            ObjectMapper mapper = new ObjectMapper();
            SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

            int N = 500_000; // 写入数量
            long start = System.nanoTime();

            for (int i = 1; i <= N; i++) {

                // 构造订单对象
                Map<String, Object> order = new HashMap<>();
                order.put("order_id", idGen.nextId());
                order.put("merchant_id", "M1001");
                order.put("user_id", "U" + i);
                order.put("amount", i * 10);
                order.put("timestamp", System.currentTimeMillis() / 1000.0);

                String key = order.get("order_id").toString();
                String value = mapper.writeValueAsString(order);

                // RocksDB 写入
                db.put(key.getBytes(), value.getBytes());

                if (i % 1000 == 0) {
                    System.out.println("写入订单 " + i + ": " + key);
                }
            }

            long end = System.nanoTime();

            double elapsedSec = (end - start) / 1_000_000_000.0;
            double tps = N / elapsedSec;

            System.out.printf("写入 %d 条订单，总耗时: %.4f 秒%n", N, elapsedSec);
            System.out.printf("平均 TPS: %.2f 条/秒%n", tps);

            System.out.println("完成写入");
        }
    }
}
