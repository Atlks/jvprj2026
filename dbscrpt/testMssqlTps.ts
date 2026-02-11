import * as fs from "fs";
import * as path from "path";

import { performance } from "perf_hooks";
// import * as sql from "mssql"; // ✅ 推荐
import sql from "mssql"; // 需要 tsconfig 里开 esModuleInterop: true

/**
 * 读取配置并构建 MSSQL 连接配置
 */
function getConnConfig(): sql.config {
    const cfgPath = "c:/cfg/config.json";
    const raw = fs.readFileSync(cfgPath, "utf-8");
    const config = JSON.parse(raw);

    return {
        server: config.server,
        database: config.database,
        user: config.uid,
        password: config.pwd,
        options: {
              encrypt: false, // ✅ 确保是布尔值
            trustServerCertificate: Boolean(config.trust_cert)
        },
        pool: {
            max: 1,      // ⚠️ 对齐 pyodbc 单连接行为
            min: 1,
            idleTimeoutMillis: 30000
        }
    };
}

/**
 * TPS 测试
 */
async function runTpsTest(count: number = 50000) {
    let pool: sql.ConnectionPool | null = null;

    try {
        const connConfig = getConnConfig();
        pool = await sql.connect(connConfig);

        console.log(`开始测试: 执行 ${count} 次存储过程...`);

        const startTime = performance.now();

        for (let i = 0; i < count; i++) {
            console.log(i);

            const transaction = new sql.Transaction(pool);

            try {
                await transaction.begin();

                const request = new sql.Request(transaction);
                await request.execute("dbo.instSameUid");

                await transaction.commit();
            } catch (err) {
                await transaction.rollback();
                console.error(`第 ${i} 次执行失败:`, err);
                break;
            }
        }

        const endTime = performance.now();

        const totalSeconds = (endTime - startTime) / 1000;
        const tps = count / totalSeconds;

        console.log("-".repeat(30));
        console.log(`总耗时: ${totalSeconds.toFixed(2)} 秒`);
        console.log(`最终 TPS: ${tps.toFixed(2)}`);
        console.log("-".repeat(30));

    } catch (err) {
        console.error("数据库连接失败:", err);
    } finally {
        if (pool) {
            await pool.close();
        }
    }
}

// 入口
runTpsTest(100);
