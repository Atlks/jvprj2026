package com_uti;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MysqlProptUti {

    public   static byte[] eofPacket() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(0xFE);
        writeShortLE(buf, 0);
        writeShortLE(buf, 0);
        return buf.toByteArray();
    }

    public   static Packet readPacket(InputStream in) throws IOException {
        byte[] header = in.readNBytes(4);
        if (header.length < 4) return null;

        int len = (header[0] & 0xff) |
                ((header[1] & 0xff) << 8) |
                ((header[2] & 0xff) << 16);

        byte[] payload = in.readNBytes(len);
        return new Packet(payload);
    }

    public   static void writePacket(OutputStream out, byte[] payload, int seq) throws IOException {
        int len = payload.length;
        out.write(len & 0xff);
        out.write((len >> 8) & 0xff);
        out.write((len >> 16) & 0xff);
        out.write(seq & 0xff);
        out.write(payload);
        out.flush();
    }


    // ---------------- protocol helpers ----------------


    public static byte[] buildHandshake(byte[] seed) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        buf.write(0x0A); // protocol version 10
        buf.write("5.7.0-rocksdb-ati\0".getBytes());

        writeIntLE(buf, 1); // connection id

        buf.write(seed, 0, 8);
        buf.write(0x00);

        int caps =
                0x0200 |      // CLIENT_PROTOCOL_41
                        0x8000 |      // CLIENT_SECURE_CONNECTION
                        0x00080000;   // CLIENT_PLUGIN_AUTH

        writeShortLE(buf, caps & 0xFFFF);
        buf.write(33); // charset utf8_general_ci
        writeShortLE(buf, 0); // status flags
        writeShortLE(buf, (caps >> 16) & 0xFFFF);

        buf.write(21); // auth plugin data length
        buf.write(new byte[10]); // reserved

        buf.write(seed, 8, 12);
        buf.write(0x00);

        buf.write("mysql_native_password\0".getBytes());

        return buf.toByteArray();
    }



    public  static byte[] okPacket() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(0x00); // OK
        buf.write(0x00); // affected rows
        buf.write(0x00); // last insert id
        writeShortLE(buf, 0); // status flags
        writeShortLE(buf, 0); // warnings
        return buf.toByteArray();
    }

    // ---------------- utils ----------------

    public  static void writeIntLE(OutputStream out, int v) throws IOException {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
        out.write((v >> 16) & 0xff);
        out.write((v >> 24) & 0xff);
    }

    public   static void writeShortLE(OutputStream out, int v) throws IOException {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
    }

    public  static void writeLenString(OutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write(b.length);
        out.write(b);
    }

    public   static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new Random().nextBytes(b);
        return b;
    }

    public   static class Packet {
        public byte[] payload;
        public Packet(byte[] payload) {
            this.payload = payload;
        }
    }
}
