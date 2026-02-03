package com_uti;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Util {

    public static String readFil(String fil) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(fil));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + fil, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DbConfig {
        public String server;
        public String database;
        public String password;
    }

    public static DbConfig loadConfig(String path) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(path), DbConfig.class);
    }

    public static void execSql(Connection conn, String sql) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL: " + sql, e);
        }
    }


    /**
     * 获取本机 CPU 核心数（逻辑核心数）
     * @return CPU 核心数
     */
    public static int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    static void main() {
        System.out.println( getCpuCores());
    }


    /**
     * 12 cpu..50 线程数可以了，多了没用
     * 本地的cpu5倍数
     * @return
     */
    public static ExecutorService getExecutorService() {
        int threadsSuggst50 = getCpuCores()*5;

        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor( );
        return pool;
    }
}
