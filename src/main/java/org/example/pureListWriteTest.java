package org.example;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class pureListWriteTest {

    public static void main(String[] args) throws Exception {

        Thread.sleep(2000);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

        int N = 500_0000; // 写入数量
        long start = System.nanoTime();

        // ⭐ 改成写入 List
        List<Map<String, Object>> orders = new ArrayList<>(N);

        for (int i = 1; i <= N; i++) {

            Map<String, Object> order = new HashMap<>();
            order.put("order_id", idGen.nextId());
            order.put("merchant_id", "M1001");
            order.put("user_id", "U" + i);
            order.put("amount", i * 10);
            order.put("timestamp", System.currentTimeMillis() / 1000.0);

            orders.add(order);

            if (i % 1000 == 0) {
                System.out.println("生成订单 " + i);
            }
        }

        long end = System.nanoTime();
        double elapsedSec = (end - start) / 1_000_000_000.0;
        double tps = N / elapsedSec;

        System.out.printf("生成 %d 条订单，总耗时: %.4f 秒%n", N, elapsedSec);
        System.out.printf("平均生成速度: %.2f 条/秒%n", tps);

        // ⭐ 最后一次性写入 JSON 文件
        String fileName = "/db/orders-" + System.currentTimeMillis() + ".json";
        mapper.writeValue(new File(fileName), orders);

        System.out.println("订单已保存到文件: " + fileName);
    }
}
