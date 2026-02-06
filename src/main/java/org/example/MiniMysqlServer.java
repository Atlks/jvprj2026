package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import  static  com_uti.MysqlProptUti.*;

public class MiniMysqlServer {

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(3307);
        System.out.println("Mini MySQL Server listening on 3307...");

        while (true) {
            Socket socket = server.accept();
            processSocket(socket);
        }
      //  server.close();
    }

    private static void processSocket(Socket socket) throws IOException {
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
            if (p == null) break;

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
                break;
            }
        }

        socket.close();
    }


    static void sendSelect1(OutputStream out, int seq) throws IOException {
        // --- 1. Column Count (ResultSet Header) ---
        // 表示返回 1 列
        writePacket(out, new byte[]{0x01}, seq++);

        // --- 2. Column Definition ---
        ByteArrayOutputStream col = new ByteArrayOutputStream();
        writeLenString(col, "def");    // catalog: 总是 "def"
        writeLenString(col, "");       // schema
        writeLenString(col, "");       // table
        writeLenString(col, "");       // org_table
        writeLenString(col, "1");      // name: 显示的列名
        writeLenString(col, "1");      // org_name: 原始列名

        col.write(0x0c);               // [关键] Next fields length: 固定 0x0c (12 bytes)

        writeShortLE(col, 33);         // character set: utf8_general_ci (0x21)
        writeIntLE(col, 11);           // column length: 11 (int11 的显示长度)
        col.write(0x03);               // type: MYSQL_TYPE_LONG (0x03)
        writeShortLE(col, 0x0001);     // flags: NOT_NULL (0x0001)
        col.write(0);                  // decimals
        col.write(new byte[2]);        // filler: 2 bytes

        writePacket(out, col.toByteArray(), seq++);

        // --- 3. EOF Packet (Column Definition 结束) ---
        // 注意：如果是 MySQL 8.0 客户端，可能需要处理 CLIENT_DEPRECATE_EOF 标志
        writePacket(out, new byte[]{(byte)0xfe, 0, 0, 0x22, 0, 0, 0}, seq++);

        // --- 4. Row Data ---
        ByteArrayOutputStream row = new ByteArrayOutputStream();
        writeLenString(row, "1");      // 每一列都是字符串格式传输 (Text Protocol)
        writePacket(out, row.toByteArray(), seq++);

        // --- 5. EOF Packet (Row Data 结束) ---
        writePacket(out, new byte[]{(byte)0xfe, 0, 0, 0x22, 0, 0, 0}, seq++);
    }

}
