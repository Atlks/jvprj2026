
import sql from "mssql";

import * as fs from "fs";
import * as path from "path";
import {fileURLToPath} from "node:url";
import {dirname} from "node:path";

/**
  npm install mssql
  npm install --save-dev @types/mssql
 */

 

export function getDbConfig(): sql.config {
    // 在 ES 模块里模拟 __dirname
    const __filename = fileURLToPath(import.meta.url);
    const __dirname = dirname(__filename);

    // 读取配置文件
    const cfgPath = join(__dirname, "cfg", "db.json");
    const rawCfg = fs.readFileSync(cfgPath, "utf-8");
    const dbConfig = JSON.parse(rawCfg);

    // 覆盖用户字段
    dbConfig.user = "Sa";

    // 构造并返回 mssql 配置
    return {
        user: dbConfig.user,
        password: dbConfig.password,
        server: dbConfig.server,
        database: dbConfig.database,
        options: {
            encrypt: false, // 如果是 Azure SQL 要改为 true
            enableArithAbort: true,
        },
    };
}

// 构造 mssql 配置
const config: sql.config =getDbConfig();

async function runStoredProc() {
    try {
        const pool = await sql.connect(config);

        // 执行存储过程
        const result = await pool.request().execute("mntTop5");

        // 输出目录
        const outputDir = path.join(__dirname, "logs_dailyChk");
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir);
        }

        // 每个结果集写入一个 JSON 文件
        result.recordsets.forEach((tableData, index) => {
            const filePath = path.join(outputDir, `table_${index + 1}.json`);
            fs.writeFileSync(filePath, JSON.stringify(tableData, null, 2), "utf-8");
            console.log(`✅ 已保存: ${filePath}`);
        });

        await pool.close();
    } catch (err) {
        console.error("执行存储过程失败:", err);
    }
}

runStoredProc();
