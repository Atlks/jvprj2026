import json
import time
import os
import orjson
# f.write(orjson.dumps(order))

#  12w tps..bat10 25wtps,bat20 30w tps
# -----------------------------
# Snowflake ID 生成器（Python 版）
# -----------------------------
class SnowflakeIdGenerator:
    def __init__(self, datacenter_id=1, worker_id=1):
        self.datacenter_id = datacenter_id & 0x1F
        self.worker_id = worker_id & 0x1F
        self.sequence = 0
        self.last_timestamp = -1

    def _timestamp(self):
        return int(time.time() * 1000)

    def next_id(self):
        timestamp = self._timestamp()

        if timestamp == self.last_timestamp:
            self.sequence = (self.sequence + 1) & 0xFFF
            if self.sequence == 0:
                while timestamp <= self.last_timestamp:
                    timestamp = self._timestamp()
        else:
            self.sequence = 0

        self.last_timestamp = timestamp

        return ((timestamp - 1288834974657) << 22) | (self.datacenter_id << 17) | (self.worker_id << 12) | self.sequence


# -----------------------------
# 主程序：高性能文件写入
# -----------------------------
def append_file_write_test():
    time.sleep(2)

    id_gen = SnowflakeIdGenerator(1, 1)
    file_name = f"orders-{int(time.time() * 1000)}.log"

    # Python 的缓冲写入：buffering=1MB
    with open(file_name, "ab", buffering=1024 * 1024) as f:

        N = 500_000
        start = time.time()

        for i in range(1, N + 1):
            order = {
                "order_id": id_gen.next_id(),
                "merchant_id": "M1001",
                "user_id": f"U{i}",
                "amount": i * 10,
                "timestamp": time.time()
            }

            json_str = json.dumps(order)
            f.write(json_str.encode("utf-8"))
            f.write(b"\n")

            # flush 每条（和你的 Java 一样）
            # 但 Python flush 成本比 Java 高，不建议频繁 flush
            if i % 20 == 0:
                f.flush()

            if i % 1000 == 0:
                print(f"写入订单 {i}")

        f.flush()

        end = time.time()
        sec = end - start
        tps = N / sec

        print(f"写入 {N} 条订单，总耗时: {sec:.4f} 秒")
        print(f"平均 TPS: {tps:.2f} 条/秒")

    print("完成写入")


if __name__ == "__main__":
    append_file_write_test()
