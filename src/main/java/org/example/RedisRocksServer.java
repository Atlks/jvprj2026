package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * socket  实现redis协议，连接rocksdb
 * 实现 RESP (Redis Serialization Protocol)。
 */
public class RedisRocksServer {
    private static RocksDB db; // 假设已经在别处初始化

    static {
        try {
            db = getDb();
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    private static RocksDB getDb() throws RocksDBException {
        Options options = new Options().setCreateIfMissing(false);
        String dbPath = "C:\\Users\\attil\\IdeaProjects\\jvprj2026\\datax\\rocksdb-data1770445278593"; // 替换为你的 RocksDB 数据目录

        RocksDB db = RocksDB.open(options, dbPath);
        return db;
    }

    public static void main(String[] args) {
        int port = 6378;

        // 1. 创建虚拟线程执行器：每个任务都会创建一个新的虚拟线程
        // 虚拟线程不需要池化，因为它们是轻量级的（几百字节），创建开销极低
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("🚀 虚拟线程 Socket 服务端已启动，监听端口: " + port);

            while (true) {
                // 2. 阻塞等待客户端连接
                Socket clientSocket = serverSocket.accept();

                // 3. 提交任务到虚拟线程执行器
                // 这里虽然看起来是单线程处理 accept，但 handleClient 内部是完全异步阻塞的
                executor.submit(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) throws IOException {

        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            while (true) {
                // 1. 解析客户端发来的命令 (通常是 Array 格式)
                List<byte[]> commands = parseRespArray(in);
                dbgCmd(commands);
                if (commands == null || commands.isEmpty()) break;

                String cmdName = new String(commands.get(0), StandardCharsets.UTF_8).toUpperCase();
                System.out.println("cmd:" + cmdName);
                switch (cmdName) {

                    case "PEXPIRETIME":
                        handlePEXPIRETIME(out, commands);
                        break;
                    case "MEMORY":
                        handleMemoryUsage(out, commands);
                        break;

                    case "TYPE":
                        handleType(out, commands);
                        break;
                    case "INFO":
                        handleInfo(out);
                        break;
                    case "SCAN":
                        handleScan(out, commands);
                        break;
                    case "GET":
                        handleGet(out, commands);
                        break;
                    case "SET":
                        handleSet(out, commands);
                        break;
                    case "PING":
                        out.write("+PONG\r\n".getBytes());
                        break;
                    case "COMMAND": // 适配 redis-cli 连接时的初始化查询
                        out.write("+OK\r\n".getBytes());
                        break;
                    default:
                        out.write("-ERR unknown command\r\n".getBytes());
                        break;
                }
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }

    }

    private static void handlePEXPIRETIME(OutputStream out, List<byte[]> commands) throws IOException {
        // PEXPIRETIME key
        if (commands.size() != 2) {
            writeError(out, "ERR wrong number of arguments for 'pexpiretime' command");
            return;
        }

        String key = new String(commands.get(1), StandardCharsets.UTF_8);

        // ① 判断 key 是否存在
        if (!exists(key)) {
            writeInteger(out, -2);
            return;
        }

        // ② 取绝对过期时间（毫秒）
        // 约定：不存在过期返回 -1
        long expireAt = getExpireAtMillis(key);

        if (expireAt <= 0) {
            writeInteger(out, -1);
        } else {
            writeInteger(out, expireAt);
        }
    }


    // 判断rocksdb 是否存在key
    private static boolean exists(String key) {
        try {
            byte[] value = db.get(key.getBytes(StandardCharsets.UTF_8));
            return value != null;
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }


    private static byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }


    private static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getLong();
    }


    private static long getExpireAtMillis(String key) {
        // 返回：
        // >0 : expireAtMillis
        // <=0: 没有过期
       return  -1;
    }

    private static void writeError(OutputStream out, String msg) throws IOException {
        out.write('-');
        out.write(msg.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeInteger(OutputStream out, long value) throws IOException {
        out.write(':');
        out.write(Long.toString(value).getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }


    private static void handleMemoryUsage(OutputStream out, List<byte[]> args) throws IOException {
        if (args.size() < 3) { // MEMORY USAGE <key>
            out.write("-ERR wrong number of arguments\r\n".getBytes());
            return;
        }
        byte[] key = args.get(2); // 注意：args[0]="MEMORY", args[1]="USAGE", args[2]=key
        try {
            byte[] value = db.get(key);
            if (value == null) {
                out.write("$-1\r\n".getBytes()); // 或者返回 :0\r\n
            } else {
                // 返回字节数
                out.write((":" + value.length + "\r\n").getBytes());
            }
        } catch (RocksDBException e) {
            out.write("-ERR rocksdb error\r\n".getBytes());
        }
    }
    private static void handleType(OutputStream out, List<byte[]> args) throws IOException {
        byte[] key = args.get(1);
        try {
            byte[] value = db.get(key);
            if (value == null) {
                out.write("+none\r\n".getBytes());
            } else {
                // 默认所有 RocksDB 里的数据都是字符串类型
                out.write("+string\r\n".getBytes());
            }
        } catch (RocksDBException e) {
            out.write("-ERR rocksdb error\r\n".getBytes());
        }
    }

    private static void dbgCmd(List<byte[]> commands) {

        final ObjectMapper jsonMapper = new ObjectMapper();

// ... 在 handleClient 循环内部 ...

        if (commands == null || commands.isEmpty()) return;

// --- 调试输出开始 ---
        try {
            List<String> debugList = new ArrayList<>();
            for (byte[] arg : commands) {
                // 使用 UTF-8 将字节数组转为字符串
                debugList.add(new String(arg, StandardCharsets.UTF_8));
            }

            // 将 List 转为 JSON 字符串并打印
            String jsonDebug = jsonMapper.writeValueAsString(debugList);
            System.out.println("DEBUG [Request JSON]: " + jsonDebug);
        } catch (Exception e) {
            System.err.println("Debug serialization failed: " + e.getMessage());
        }
// --- 调试输出结束 ---


// ... 后续处理 ...

    }

    //  ["SCAN","0","COUNT","1000"]  from client
    private static void handleScan(OutputStream out, List<byte[]> args) throws IOException {
        // 1. 获取客户端传来的游标（SCAN cursor [MATCH pattern] [COUNT count]）
        String cursorStr = new String(args.get(1), StandardCharsets.UTF_8);
        int count = 1000; // 默认返回1000条

        // 简单的参数解析（处理 COUNT）
        for (int i = 2; i < args.size(); i++) {
            if ("COUNT".equalsIgnoreCase(new String(args.get(i)))) {
                count = Integer.parseInt(new String(args.get(i + 1)));
            }
        }

        List<byte[]> keys = new ArrayList<>();
        String nextCursor = "0";

        try (RocksIterator iter = db.newIterator()) {
            // 2. 确定起点
            if (cursorStr.equals("0")) {
                iter.seekToFirst();
            } else {
                // 将游标当作上一次最后读取的 Key
                iter.seek(cursorStr.getBytes(StandardCharsets.UTF_8));
                if (iter.isValid()) iter.next(); // 跳过当前已读取过的 Key
            }

            // 3. 迭代数据
            while (iter.isValid() && keys.size() < count) {
                byte[] key = iter.key();
                keys.add(key);
                nextCursor = new String(key, StandardCharsets.UTF_8); // 更新下一个游标
                iter.next();
            }

            // 如果迭代器走到底了，游标设为 "0"
            if (!iter.isValid()) {
                nextCursor = "0";
            }

            // 4. 按照 RESP 格式写回
            writeScanResponse(out, nextCursor, keys);
        }
    }

    private static void writeScanResponse(OutputStream out, String nextCursor, List<byte[]> keys) throws IOException {
        // *2\r\n
        out.write(("*2\r\n").getBytes());

        // 第一个元素：Next Cursor
        out.write(("$" + nextCursor.length() + "\r\n" + nextCursor + "\r\n").getBytes());

        // 第二个元素：Key Array
        out.write(("*" + keys.size() + "\r\n").getBytes());
        for (byte[] key : keys) {
            out.write(("$" + key.length + "\r\n").getBytes());
            out.write(key);
            out.write("\r\n".getBytes());
        }
    }

    private static void handleInfo(OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();

        // Server 部分
        sb.append("# Server\r\n");
        sb.append("redis_version:7.0.0\r\n"); // 伪装成高版本
        sb.append("redis_mode:standalone\r\n");
        sb.append("os:Linux\r\n");
        sb.append("arch_bits:64\r\n");

        // Clients 部分
        sb.append("# Clients\r\n");
        sb.append("connected_clients:1\r\n");

        // Stats 部分 (可以把 RocksDB 的一些指标放这里)
        sb.append("# Stats\r\n");
        sb.append("total_connections_received:10\r\n");
        sb.append("total_commands_processed:100\r\n");

        // 按照 Bulk String 格式打包
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        out.write(("$" + body.length + "\r\n").getBytes());
        out.write(body);
        out.write("\r\n".getBytes());
    }

    private static void handleGet(OutputStream out, List<byte[]> args) throws IOException {
        if (args.size() < 2) {
            out.write("-ERR wrong number of arguments\r\n".getBytes());
            return;
        }
        try {
            byte[] value = db.get(args.get(1));
            if (value == null) {
                out.write("$-1\r\n".getBytes()); // Null Bulk String
            } else {
                out.write(("$" + value.length + "\r\n").getBytes());
                out.write(value);
                out.write("\r\n".getBytes());
            }
        } catch (RocksDBException e) {
            out.write(("-ERR " + e.getMessage() + "\r\n").getBytes());
        }
    }

    private static void handleSet(OutputStream out, List<byte[]> args) throws IOException {
        if (args.size() < 3) {
            out.write("-ERR wrong number of arguments\r\n".getBytes());
            return;
        }
        try {
            db.put(args.get(1), args.get(2));
            out.write("+OK\r\n".getBytes());
        } catch (RocksDBException e) {
            out.write(("-ERR " + e.getMessage() + "\r\n").getBytes());
        }
    }

    /**
     * 极简 RESP Array 解析器
     */
    private static List<byte[]> parseRespArray(InputStream in) throws IOException {
        int firstByte = in.read();
        if (firstByte == -1) return null;
        if (firstByte != '*') {
            // 简单处理非数组情况（例如 telnet 直接发送命令）
            return null;
        }

        int numElements = Integer.parseInt(readLine(in));
        List<byte[]> payload = new ArrayList<>(numElements);

        for (int i = 0; i < numElements; i++) {
            int type = in.read();
            if (type == '$') {
                int length = Integer.parseInt(readLine(in));
                byte[] data = new byte[length];
                int read = 0;
                while (read < length) {
                    read += in.read(data, read, length - read);
                }
                in.read(); // skip \r
                in.read(); // skip \n
                payload.add(data);
            }
        }
        return payload;
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1 && b != '\r') {
            sb.append((char) b);
        }
        in.read(); // skip \n
        return sb.toString();
    }
}
