package org.example;



import com.fasterxml.jackson.databind.ObjectMapper;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ChronicleQueueBuilder;
import net.openhft.chronicle.queue.ExcerptAppender;

import java.util.HashMap;
import java.util.Map;

public class ChronicleQueueWriteTest {

    public static void main(String[] args) throws Exception {

        Thread.sleep(2000);

        ObjectMapper mapper = new ObjectMapper();
        SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

        // ⭐ 打开 Chronicle Queue（目录自动创建）
        String path = "cq-data-" + System.currentTimeMillis();
        //hronicleQueue queue = ChronicleQueue.singleBuilder(path).build();


        ChronicleQueue queue = ChronicleQueueBuilder.single(path).build();

        ExcerptAppender appender = queue.acquireAppender();

        int N = 50_0000; // 写入数量
        long start = System.nanoTime();

        for (int i = 1; i <= N; i++) {

            // 构造订单对象
            Map<String, Object> order = new HashMap<>();
            order.put("order_id", idGen.nextId());
            order.put("merchant_id", "M1001");
            order.put("user_id", "U" + i);
            order.put("amount", i * 10);
            order.put("timestamp", System.currentTimeMillis() / 1000.0);

            String json = mapper.writeValueAsString(order);

            // ⭐ Chronicle Queue 写入（append-only）
            appender.writeText(json);

            if (i % 1000 == 0) {
                System.out.println("写入订单 " + i);
            }
        }

        long end = System.nanoTime();

        double elapsedSec = (end - start) / 1_000_000_000.0;
        double tps = N / elapsedSec;

        System.out.printf("写入 %d 条订单，总耗时: %.4f 秒%n", N, elapsedSec);
        System.out.printf("平均 TPS: %.2f 条/秒%n", tps);

        System.out.println("完成写入");

        queue.close();
    }
}

