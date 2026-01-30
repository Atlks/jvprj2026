import cron from "node-cron";
import sql from "mssql";
import {getDbConfig} from "./conn.ts";

import path from "node:path";
import fs from "fs";

// 构造 mssql 配置
const config: sql.config =getDbConfig();

/**
 * 追加字符串到某个文件，自动追加回车换行
 * @param file
 * @param str
 */
function appendFile2026(file: string, str: string) {
    try {
        // 确保目录存在
        const dir = path.dirname(file);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }

        // 追加内容并自动换行
        fs.appendFileSync(file, str + "\n", { encoding: "utf-8" });
        console.log(`已追加到文件: ${file}`);
    } catch (err) {
        console.error("追加文件失败:", err);
    }
}


// 定义备份任务
async function runDiffBackup() {


    try {
        const pool = await sql.connect(config);

        // 构造备份文件名
        const now = new Date();
        const timestamp = now.toISOString().replace(/[:T]/g, "_").slice(0,16);
        const backupFile = `D:\\Backup\\Diff\\aHxPay_Diff_${timestamp}.bak`;
        console.log(backupFile)
        appendFile2026("/logs/dflogs.log",backupFile);

        const query = `
      BACKUP DATABASE HxPay
      TO DISK = '${backupFile}'
      WITH DIFFERENTIAL, INIT, COMPRESSION, STATS = 10;
    `;
        console.log(query)
        await pool.request().query(query);
        console.log(`差异备份完成: ${backupFile}`);
    } catch (err) {
        console.error("备份失败:", err);
    }
}

// 每小时执行一次任务
// cron.schedule("0 * * * *", () => {
//     runDiffBackup();
// });
runDiffBackup();