package org.example;

import org.rocksdb.*;

public class RocksDBTransactionExample {

    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws Exception {

        // RocksDB 选项
        Options options = new Options()
                .setCreateIfMissing(true);

        TransactionDBOptions txOptions = new TransactionDBOptions();

        // 打开 TransactionDB
        try (TransactionDB db = TransactionDB.open(options, txOptions, "rocksdb-data")) {

            // 开启事务
            WriteOptions writeOptions = new WriteOptions();
            TransactionOptions txnOptions = new TransactionOptions();

            Transaction txn = db.beginTransaction(writeOptions, txnOptions);

            try {
                // 写入数据
                txn.put("key1".getBytes(), "value1".getBytes());
                txn.put("key2".getBytes(), "value2".getBytes());

                // 读取数据（事务内读）
//                byte[] v = txn.get("key1".getBytes());
//                System.out.println("事务内读取 key1 = " + new String(v));

                // 提交事务
                txn.commit();
                System.out.println("事务提交成功");

            } catch (Exception e) {
                // 回滚事务
                txn.rollback();
                System.out.println("事务回滚");
            } finally {
                txn.close();
            }

            // 事务外读取
            byte[] v2 = db.get("key1".getBytes());
            System.out.println("事务外读取 key1 = " + new String(v2));
        }
    }
}
