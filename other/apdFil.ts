import * as fs from "fs";


/**
 * batch mode只是方式不是 flush()，而是批量 write()。**
 */
//30wtps
// -----------------------------
// Snowflake ID 生成器（TS 版）
// -----------------------------
class SnowflakeIdGenerator {
    private datacenterId: number;
    private workerId: number;
    private sequence = 0;
    private lastTimestamp = -1;

    constructor(datacenterId = 1, workerId = 1) {
        this.datacenterId = datacenterId & 0x1F;
        this.workerId = workerId & 0x1F;
    }

    private timestamp() {
        return Date.now();
    }

    nextId(): number {
        let ts = this.timestamp();

        if (ts === this.lastTimestamp) {
            this.sequence = (this.sequence + 1) & 0xFFF;
            if (this.sequence === 0) {
                while (ts <= this.lastTimestamp) {
                    ts = this.timestamp();
                }
            }
        } else {
            this.sequence = 0;
        }

        this.lastTimestamp = ts;

        return (
            ((ts - 1288834974657) << 22) |
            (this.datacenterId << 17) |
            (this.workerId << 12) |
            this.sequence
        );
    }
}

// -----------------------------
// 主程序：高性能文件写入
// -----------------------------
async function main() {
    await new Promise(r => setTimeout(r, 2000));

    const idGen = new SnowflakeIdGenerator(1, 1);
    const fileName = `orders-${Date.now()}.log`;

    // Node.js 的 writeStream 默认使用 OS 缓冲区
    const stream = fs.createWriteStream(fileName, {
        flags: "a",
        highWaterMark: 1024 * 1024 // ⭐ 1MB 缓冲区
    });

    const N = 500_000;
    const start = process.hrtime.bigint();

    for (let i = 1; i <= N; i++) {
        const order = {
            order_id: idGen.nextId(),
            merchant_id: "M1001",
            user_id: `U${i}`,
            amount: i * 10,
            timestamp: Date.now() / 1000
        };

        const json = JSON.stringify(order) + "\n";

        // 写入 OS page cache，不阻塞
        if (!stream.write(json)) {
            await new Promise(res => stream.once("drain", res));
        }

        // Java 的 flush() 在 Node 中不需要 // Node 会自动 flush 到 OS buffer // 如果你想模拟 flush，可以什么都不做
       // if (i % 2 === 0) { // no-op }

        if (i % 1000 === 0) {
            console.log(`写入订单 ${i}`);
        }
    }

    await new Promise(res => stream.end(res));

    const end = process.hrtime.bigint();
    const sec = Number(end - start) / 1e9;
    const tps = N / sec;

    console.log(`写入 ${N} 条订单，总耗时: ${sec.toFixed(4)} 秒`);
    console.log(`平均 TPS: ${tps.toFixed(2)} 条/秒`);
    console.log("完成写入");
}

main();
