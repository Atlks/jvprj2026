package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import com_uti.SnowflakeIdGenerator;
import org.rocksdb.*;

import java.util.HashMap;
import java.util.Map;

public class RocksDBWriteTest2 {

    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws Exception {

        Thread.sleep(3000);
        /**
         * 👉 RocksDB 默认是 低风险 + 低内存 + 低并发
         * 👉 性能潜力只用到了 30% 左右
         *
         * RocksDB 的设计是：
         * Options = DB 打开级别配置
         * WriteOptions = 每次写入时用的配置
         */
        // RocksDB 配置



        //  Options options2=(Options)writeOptions;

        Options options = new Options()
                .setCreateIfMissing(true);
        options   .setUseDirectReads(true);
        options    .setUseDirectIoForFlushAndCompaction(true);
        options
                .setWriteBufferSize(256 * 1024 * 1024) // 256MB
                .setMaxWriteBufferNumber(4)
                .setMinWriteBufferNumberToMerge(2);
        options
                .setIncreaseParallelism(Runtime.getRuntime().availableProcessors())
                .setMaxBackgroundCompactions(4)
                .setMaxBackgroundFlushes(2);
        // ===== 并发 & CPU =====
        options.setIncreaseParallelism(12)              // 吃满 12 核
                .setMaxBackgroundJobs(12);
        options
                .setParanoidChecks(false)
                .setSkipStatsUpdateOnDbOpen(true);
     //   options.setDisableWAL(true);
        options.setWalTtlSeconds(1);
        options.setCompactionStyle(CompactionStyle.UNIVERSAL);
        options
                .setCompressionType(CompressionType.LZ4_COMPRESSION);
        // ===== WAL =====
        options .setWalBytesPerSync(8 * 1024 * 1024)     // 批量 fsync
                .setUseFsync(false);                     // 非强一致 fsync

        options
                .setCompactionStyle(CompactionStyle.LEVEL)
                .setTargetFileSizeBase(256 * 1024 * 1024)
                .setLevel0FileNumCompactionTrigger(4)
                .setLevel0SlowdownWritesTrigger(20)
                .setLevel0StopWritesTrigger(36);


        // 打开 RocksDB
        try (RocksDB db = RocksDB.open(options, "rocksdb-data"+System.currentTimeMillis())) {

            ObjectMapper mapper = new ObjectMapper();
            SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

            int N = 500_0000; // 写入数量
            long start = System.nanoTime();
            WriteOptions writeOptions = new WriteOptions()
                    .setDisableWAL(false)
                    .setSync(false);


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
                db.put(writeOptions,key.getBytes(), value.getBytes());

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
