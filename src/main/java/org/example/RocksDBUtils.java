package org.example;

import org.rocksdb.*;
import java.util.*;

public class RocksDBUtils {

    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        String dbPath = "C:\\Users\\attil\\IdeaProjects\\jvprj2026\\rocksdb-data1770366064462"; // 替换为你的 RocksDB 数据目录
        ;
        String startKey = "2019687788127064230";

        // 测试倒序，默认返回 10 条
        List<Map.Entry<String, String>> lastKeys = rangeQuery(dbPath, startKey, "desc", 10);
        System.out.println("=== Desc ===");
        for (Map.Entry<String, String> entry : lastKeys) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        System.out.println(" dsc finsh...");

        // 测试正序
        List<Map.Entry<String, String>> ascKeys = rangeQuery(dbPath, startKey, "asc", 10);
        System.out.println("=== Asc ===");
        for (Map.Entry<String, String> entry : ascKeys) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

    }



    /**
     * RocksDB 范围查询
     * @param dbPath RocksDB 数据目录
     * @param startKey 起始 key（包含）
     * @param order 排序方式，"asc" 或 "desc"，默认 "desc"
     * @param limit 最大返回条数，默认 10
     * @return 范围内的 key/value 列表
     */
    public static List<Map.Entry<String, String>> rangeQuery(String dbPath, String startKey, String order, int limit) throws RocksDBException {

        Options options = new Options().setCreateIfMissing(false);

        try (RocksDB db = RocksDB.open(options, dbPath);
             RocksIterator iterator = db.newIterator()) {

            // 默认值处理
            if (order == null || (!order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc"))) {
                order = "desc";
            }
            if (limit <= 0) {
                limit = 10;
            }

            if (order.equalsIgnoreCase("asc")) {
                return qryRangAsc(startKey, limit,  iterator);
            } else {
                return qryRangDesc(startKey, limit, iterator);
            }

        }

    }

    /**
     * 数据查询 范围查询 算法 ，游标查询算法
     * 查询dv数据库
     * @param startKey
     * @param limit
     * @param iterator
     * @return
     */
    private static List<Map.Entry<String, String>> qryRangDesc(String startKey, int limit, RocksIterator iterator ) {
        List<Map.Entry<String, String>> result = new ArrayList<>();

        if( startKey =="@last")
        {
            // 定位到最后一个 key
            iterator.seekToLast();
        } else if( startKey =="@first")
        {

            return result;
        }

        else{
            // 倒序遍历
            iterator.seek(startKey.getBytes());
        }


        // 如果 startKey 不存在，先定位到小于 startKey 的最大 key
        if (!iterator.isValid() || !new String(iterator.key()).equals(startKey)) {
            iterator.seekForPrev(startKey.getBytes());
        }

        int count = 0;
        while (iterator.isValid() && count < limit) {
            String key = new String(iterator.key());
            String value = new String(iterator.value());
            result.add(new AbstractMap.SimpleEntry<>(key, value));
            iterator.prev();
            count++;
        }
        return result;
    }

    private static List<Map.Entry<String, String>> qryRangAsc(String startKey, int limit,   RocksIterator iterator) {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        if( startKey =="@last")
        {
            return result;
            // 定位到第一个 key
           // iterator.seekToFirst();
        } else if( startKey =="@first")
        {

             iterator.seekToFirst();
        }

        else{
            // 正序遍历
            iterator.seek(startKey.getBytes());
        }
        int count = 0;
        while (iterator.isValid() && count < limit) {
            String key = new String(iterator.key());
            String value = new String(iterator.value());
            result.add(new AbstractMap.SimpleEntry<>(key, value));
            iterator.next();
            count++;
        }
        return result;
    }




    /**
     * 列出 RocksDB 最后 N 个 key 和 value
     */
    public static List<Map.Entry<String, String>> listLastKeys(String dbPath, int n) throws RocksDBException {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        Options options = new Options().setCreateIfMissing(false);

        try (RocksDB db = RocksDB.open(options, dbPath);
             RocksIterator iterator = db.newIterator()) {

            // 定位到最后一个 key
            iterator.seekToLast();

            int count = 0;
            while (iterator.isValid() && count < n) {
                String key = new String(iterator.key());
                String value = new String(iterator.value());
                result.add(new AbstractMap.SimpleEntry<>(key, value));

                iterator.prev(); // 反向遍历
                count++;
            }
        }

        // 反转顺序，使结果从最旧到最新（可选）
        //  Collections.reverse(result);

        return result;
    }


    /**
     *
     * @param dbPath
     * @param nextKey
     * @param n
     * @return
     * @throws RocksDBException
     */
    @Deprecated
    public static List<Map.Entry<String, String>> listLastKeys(String dbPath, String nextKey, int n) throws RocksDBException {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        Options options = new Options().setCreateIfMissing(false);

        try (RocksDB db = RocksDB.open(options, dbPath);
             RocksIterator iterator = db.newIterator()) {

            // 定位到最后一个 key
            iterator.seekToLast();

            int count = 0;
            while (iterator.isValid() && count < n) {
                String key = new String(iterator.key());
                String value = new String(iterator.value());


                // 判断 key 是否小于 nextKey
                if (key.compareTo(nextKey) <= 0)
                {
                    result.add(new AbstractMap.SimpleEntry<>(key, value));

                    count++;
                }


                iterator.prev(); // 反向遍历

            }
        }

        // 反转顺序，使结果从最旧到最新（可选）
        //  Collections.reverse(result);

        return result;
    }

}
