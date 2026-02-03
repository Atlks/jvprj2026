package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * 30w tps..if bat 2 ..50 w tps
 * bat10,better,,99w tps
 *buffering  1m 10m same
 */
public class AppendFileWriteTest {

    public static void main(String[] args) throws Exception {

        Thread.sleep(2000);

        ObjectMapper mapper = new ObjectMapper();
        SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

        String fileName = "orders-" + System.currentTimeMillis() + ".log";

        // ⭐ 1MB 缓冲区，写入速度极快
        try (FileOutputStream fos = new FileOutputStream(fileName, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 1024)) {

            int N = 500_000;
            long start = System.nanoTime();

            for (int i = 1; i <= N; i++) {

                Map<String, Object> order = new HashMap<>();
                order.put("order_id", idGen.nextId());
                order.put("merchant_id", "M1001");
                order.put("user_id", "U" + i);
                order.put("amount", i * 10);
                order.put("timestamp", System.currentTimeMillis() / 1000.0);

                String json = mapper.writeValueAsString(order);

                bos.write(json.getBytes());
                bos.write('\n');

                if (i % 2 == 0) {
                    bos.flush();
                }

                if (i % 1000 == 0) {
                    System.out.println("写入订单 " + i);
                }
            }

            bos.flush();
            /*
            ✔️ 2. FileOutputStream.write() 是直接调用 native write()
Java 的 flush() 不会强制 fsync
不会强制落盘
只是把 buffer 推给 OS
             */

            long end = System.nanoTime();
            double sec = (end - start) / 1e9;
            double tps = N / sec;

            System.out.printf("写入 %d 条订单，总耗时: %.4f 秒%n", N, sec);
            System.out.printf("平均 TPS: %.2f 条/秒%n", tps);
        }

        System.out.println("完成写入");
    }
}
