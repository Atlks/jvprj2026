// @ts-ignore
import Database from "better-sqlite3";
import { randomUUID } from "crypto";

/**
 * 为什么同样写入sqlite 同样代码逻辑。。ts写的性能是28万tps，，java版本是五万
 * 这个现象完全正常，而且背后的原因非常有意思。
 * 你看到：
 *
 * TypeScript（better‑sqlite3）≈ 28 万 TPS
 *
 * Java（JDBC + sqlite-jdbc）≈ 5 万 TPS
 *
 * 不是因为 TypeScript 更快，而是因为：
 *
 * better‑sqlite3 的实现方式比 sqlite-jdbc 更接近 SQLite 的原生性能，调用栈更短、锁更少、开销更低。
 */
// Snowflake ID 生成器（与 Java 版本结构一致）
class SnowflakeIdGenerator {
    private workerId: number;
    private datacenterId: number;
    private sequence: number = 0;
    private lastTimestamp: number = -1;

    constructor(workerId: number, datacenterId: number) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    private timestamp() {
        return Date.now();
    }

    nextId(): string {
        let ts = this.timestamp();

        if (ts === this.lastTimestamp) {
            this.sequence = (this.sequence + 1) & 0xfff;
            if (this.sequence === 0) {
                while (ts <= this.lastTimestamp) {
                    ts = this.timestamp();
                }
            }
        } else {
            this.sequence = 0;
        }

        this.lastTimestamp = ts;

        const id =
            ((ts - 1288834974657) << 22) |
            (this.datacenterId << 17) |
            (this.workerId << 12) |
            this.sequence;

        return id.toString();
    }
}

// 打开 SQLite（等价于 JDBC）
const db = new Database("orders3.db");

// WAL + synchronous=OFF（与 Java 一致）
db.pragma("journal_mode = WAL");
db.pragma("synchronous = OFF");

// 创建表
db.exec(`
    CREATE TABLE IF NOT EXISTS ords (
        id INTEGER PRIMARY KEY,
        k TEXT NOT NULL,
        v TEXT NOT NULL
    )
`);

const insertStmt = db.prepare("INSERT INTO ords (k, v) VALUES (?, ?)");

const idGen = new SnowflakeIdGenerator(1, 1);

const count = 500_000;
const start = process.hrtime.bigint();

  for (let i = 1; i <= count; i++) {
        const order = {
            order_id: randomUUID(),
            merchant_id: "M1001",
            user_id: "U" + i,
            amount: i * 10,
            timestamp: Date.now() / 1000
        };

        const k = order.order_id;
        const v = JSON.stringify(order);
        db.exec("BEGIN");
        insertStmt.run(k, v);
        db.exec("commit");
        if (i % 1000 === 0) {
            console.log(`写入订单 ${i}: ${k}`);
        }
    }




const end = process.hrtime.bigint();

const elapsedSec = Number(end - start) / 1_000_000_000;
const tps = count / elapsedSec;

console.log(`写入 ${count} 条订单，总耗时: ${elapsedSec.toFixed(4)} 秒`);
console.log(`平均 TPS: ${tps.toFixed(2)} 条/秒`);
console.log("完成写入");
