package org.example;



import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com_uti.MysqlProptUti.*;
import static com_uti.MysqlProptUti.okPacket;
import static com_uti.MysqlProptUti.readPacket;
import static com_uti.MysqlProptUti.writePacket;
import static org.example.MiniMysqlServer.sendSelect1;


/**
 * socket mysql prpt
 */
public class VirtualThreadSocketServer {

    public static void main(String[] args) {
        int port = 3307;

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
        String threadInfo = Thread.currentThread().toString();
        System.out.println("✅ 处理连接: " + socket.getRemoteSocketAddress() + " | " + threadInfo);

        System.out.println("Client connected");

        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        int seq = 0;

        // Step 1: send handshake
        byte[] authSeed = randomBytes(20);
        writePacket(out, buildHandshake(authSeed), seq++);

        // Step 2: read auth response (ignore content)
        readPacket(in);

        // send OK packet (login success)
        // reset seq for server response
        seq = 2;
        writePacket(out, okPacket(), seq);

        // command loop
        while (true) {
            Packet p = readPacket(in);
            if (p == null) continue;

            byte cmd = p.payload[0];

            if (cmd == 0x03) { // COM_QUERY
                String sql = new String(p.payload, 1, p.payload.length - 1, StandardCharsets.UTF_8);
                System.out.println("SQL: " + sql);

                if (sql.trim().equalsIgnoreCase("SELECT 1")) {
                    sendSelect1(out, seq);
                    seq += 4;
                } else {
                    writePacket(out, okPacket(), seq++);
                }
            } else if (cmd == 0x01) { // COM_QUIT
               // break;
                socket.close();
            }
        }


    }
}
