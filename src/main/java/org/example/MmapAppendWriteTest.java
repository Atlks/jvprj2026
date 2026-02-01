package org.example;



import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class MmapAppendWriteTest {

    public static void main(String[] args) throws Exception {

        Thread.sleep(2000);

        ObjectMapper mapper = new ObjectMapper();
        SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

        String fileName = "orders-" + System.currentTimeMillis() + ".log";

        System.out.println(fileName);
        // ⭐ 预分配 1GB（可根据需要调整）
        long mmapSize = 1L * 1024 * 1024 * 1024;
        mmapSize = 1L  * 1024;
        File file = new File(fileName);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel channel = raf.getChannel()) {

            // ⭐ 创建 MMAP 映射
            MappedByteBuffer mbb = channel.map(FileChannel.MapMode.READ_WRITE, 0, mmapSize);

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
                byte[] bytes = json.getBytes();

                // ⭐ 写入 JSON
                mbb.put(bytes);
                mbb.put((byte) '\n');

                if (i % 1000 == 0) {
                    System.out.println("写入订单 " + i);
                }
            }

            // ⭐ 强制刷盘（可选）
            mbb.force();

            long end = System.nanoTime();
            double sec = (end - start) / 1e9;
            double tps = N / sec;

            System.out.printf("写入 %d 条订单，总耗时: %.4f 秒%n", N, sec);
            System.out.printf("平均 TPS: %.2f 条/秒%n", tps);
        }

        System.out.println("完成写入");
    }
}
