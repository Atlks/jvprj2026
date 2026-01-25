package com_uti;

import java.util.concurrent.ConcurrentHashMap;

public class LockUti {

    // 全局锁池：每个 userId 对应一个锁对象
    private static final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();



    /**
     *
     *   // 获取某个 userId 的锁对象
     *     //原因不是语法，而是 Java 字符串的引用机制会让你的锁完全失效。
     *     //为什么必须要对象，不直接uid。。因为要保证同一对象才可
     * ✔ 同一个 uid → 一定拿到同一个锁对象
     * 因为 computeIfAbsent 保证只创建一次。
     *
     * ✔ 不同 uid → 一定是不同锁对象
     * 不会互相阻塞。
     *
     * ✔ 不依赖 String 引用
     * 你锁的是 Object，不是字符串。
     * @param userId
     * @return
     */
    public static Object getLock(String userId) {
        return userLocks.computeIfAbsent(userId, k -> new Object());
    }
}
