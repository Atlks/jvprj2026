package org.example;

import com_uti.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashMap;
import java.util.Map;

public class RedisWriteTest {

    public static void main(String[] args) throws Exception {
        // 1. 配置连接池（类似 RocksDB Options）
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128); // 最大连接数
        poolConfig.setMaxIdle(64);
        poolConfig.setMinIdle(16);

        // 假设 Redis 部署在本地，如果是云数据库，RTT 对性能影响巨大
        try (JedisPool pool = new JedisPool(poolConfig, "127.0.0.1", 6379)) {

            ObjectMapper mapper = new ObjectMapper();
            SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);
            int N = 50_0000; // 写入数量
            int batchSize = 111; // Pipeline 批次大小

            try (Jedis jedis = pool.getResource()) {
                System.out.println("开始 Redis 写入测试...");
                long start = System.nanoTime();

                Pipeline pipeline = jedis.pipelined(); // 开启流水线

                for (int i = 1; i <= N; i++) {
                    // 构造订单对象
                    Map<String, Object> order = new HashMap<>();
                    String orderId = String.valueOf(idGen.nextId());
                    order.put("order_id", orderId);
                    order.put("merchant_id", "M1001");
                    order.put("user_id", "U" + i);
                    order.put("amount", i * 10);
                    order.put("timestamp", System.currentTimeMillis() / 1000.0);

                    String value = mapper.writeValueAsString(order);

                    // 写入 Pipeline 缓存，不立即发送网络请求
                    pipeline.set(orderId, value);

                    // 每达到 batchSize 提交一次，平衡内存压力与网络开销
                    if (i % batchSize == 0) {
                        pipeline.sync(); // 发送并清空缓冲区
                        pipeline = jedis.pipelined(); // 重启一个新的 pipeline
                        System.out.println("已写入订单: " + i);
                    }
                }

                // 处理剩余的数据
                pipeline.sync();

                long end = System.nanoTime();
                double elapsedSec = (end - start) / 1_000_000_000.0;
                double tps = N / elapsedSec;

                System.out.printf("写入 %d 条订单，总耗时: %.4f 秒%n", N, elapsedSec);
                System.out.printf("平均 TPS: %.2f 条/秒%n", tps);
            }
        }
    }
}