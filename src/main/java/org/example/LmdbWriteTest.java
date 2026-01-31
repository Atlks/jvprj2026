package org.example;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.lmdbjava.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lmdbjava.DbiFlags.MDB_CREATE;
import static org.lmdbjava.EnvFlags.MDB_NOSYNC;

public class LmdbWriteTest {

    public static void main(String[] args) throws Exception {

        Thread.sleep(2000);

        ObjectMapper mapper = new ObjectMapper();
        SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

        // ⭐ LMDB 环境（目录自动创建）
        String dir = "lmdb-data-" + System.currentTimeMillis();
        File path = new File(dir);

        Env<ByteBuffer> env = Env.create()
                .setMapSize(20L * 1024 * 1024 * 1024) // 20GB
                .setMaxDbs(1)
                .open(path, MDB_NOSYNC); // 更快写入

        // ⭐ 打开数据库
        Dbi<ByteBuffer> db = env.openDbi("orders", MDB_CREATE);

        int N = 50_0000;
        long start = System.nanoTime();

        // 复用 DirectByteBuffer（LMDB 要求 Direct）
        ByteBuffer keyBuf = ByteBuffer.allocateDirect(8);
        ByteBuffer valBuf = ByteBuffer.allocateDirect(4096);

        // ⭐ 批量写入（1000 条一个事务）速度最快
        int batch = 1000;
        Txn<ByteBuffer> txn = env.txnWrite();

        for (int i = 1; i <= N; i++) {

            // 构造订单对象
            Map<String, Object> order = new HashMap<>();
            long orderId = idGen.nextId();
            order.put("order_id", orderId);
            order.put("merchant_id", "M1001");
            order.put("user_id", "U" + i);
            order.put("amount", i * 10);
            order.put("timestamp", System.currentTimeMillis() / 1000.0);

            String json = mapper.writeValueAsString(order);

            // key = long
            keyBuf.clear();
            keyBuf.putLong(orderId).flip();

            // value = json
            valBuf.clear();
            valBuf.put(json.getBytes()).flip();

            // ⭐ LMDB 写入
            db.put(txn, keyBuf, valBuf);

            if (i % batch == 0) {
                txn.commit();
                txn.close();
                txn = env.txnWrite();
            }

            if (i % 1000 == 0) {
                System.out.println("写入订单 " + i + ": " + orderId);
            }
        }

        txn.commit();
        txn.close();

        long end = System.nanoTime();
        double elapsedSec = (end - start) / 1_000_000_000.0;
        double tps = N / elapsedSec;

        System.out.printf("写入 %d 条订单，总耗时: %.4f 秒%n", N, elapsedSec);
        System.out.printf("平均 TPS: %.2f 条/秒%n", tps);

        env.close();
        System.out.println("完成写入");
    }
}

