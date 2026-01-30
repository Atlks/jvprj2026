import sql from "mssql";
import {fileURLToPath} from "node:url";
import {dirname, join} from "node:path";
import fs from "fs";

export function getDbConfig(): sql.config {
    // 在 ES 模块里模拟 __dirname
    const __filename = fileURLToPath(import.meta.url);
    const __dirname = dirname(__filename);

    // 读取配置文件
    const cfgPath = join("/", "cfg", "db.json");
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