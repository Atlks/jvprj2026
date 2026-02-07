package org.example;


import com_uti.SnowflakeIdGenerator;
import org.rocksdb.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class RocksDBWriteTest {

    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws Exception {

        Thread.sleep(5000);
        /**dis wal 70w tps...open wal 20w tps
         * 👉 RocksDB 默认是 低风险 + 低内存 + 低并发
         * 👉 性能潜力只用到了 30% 左右
         *
         * RocksDB 的设计是：
         * Options = DB 打开级别配置
         * WriteOptions = 每次写入时用的配置
         *
         * 所有写入先进入内存（MemTable），延迟、批量刷盘，减少磁盘压力
         *
         * WAL 写入 OS page cache（非常快）
         *
         * MemTable 写满才 flush
         *
         * flush 也被延迟
         *
         * compaction 也被延迟
         *
         * 崩溃时只丢 OS page cache 中的 WAL（通常几十毫秒）
         */
        // RocksDB 配置


        //  Options options2=(Options)writeOptions;

        Options options = new Options()
                .setCreateIfMissing(true);
        options.setUseDirectReads(true);
        options.setUseDirectIoForFlushAndCompaction(true);

        //设置巨大的 MemTable（写入都在内存）
        options
                .setWriteBufferSize(556 * 1024 * 1024) // 256MB
                .setMaxWriteBufferNumber(8)
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

        options.setWalTtlSeconds(1);
        options.setCompactionStyle(CompactionStyle.UNIVERSAL);
        options
                .setCompressionType(CompressionType.LZ4_COMPRESSION);
        // ===== WAL ===== 延迟 flush
        options.setMaxTotalWalSize(600 * 1024 * 1024); // 200mb)
        options.setDelayedWriteRate(0);  //// 不限速
        options.setWalBytesPerSync(50 * 1024 * 1024)     // 批量 fsync
                .setUseFsync(false);                     // 非强一致 fsync

        //✔ 延迟 compaction
        options
                .setCompactionStyle(CompactionStyle.LEVEL)
                .setTargetFileSizeBase(556 * 1024 * 1024)
                .setLevel0FileNumCompactionTrigger(20)
                .setLevel0SlowdownWritesTrigger(20)
                .setLevel0StopWritesTrigger(36);


        // 打开 RocksDB
        try (RocksDB db = RocksDB.open(options, "datax/rocksdb-data" + System.currentTimeMillis())) {

            ObjectMapper mapper = new ObjectMapper();
            SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

            /**
             * 性能评价，默认 20wtps
             * DisableWAL(true) 写入100wtps
             */
            int N = 5_0000; // 写入数量
            long start = System.nanoTime();
            WriteOptions writeOptions = new WriteOptions()
                    .setDisableWAL(true)
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
                db.put(writeOptions, key.getBytes(), value.getBytes());

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
