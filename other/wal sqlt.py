import sqlite3
import json
import time
import uuid

# 连接 SQLite（没有则自动创建）
conn = sqlite3.connect("orders.db")
cursor = conn.cursor()

# 创建表（如果不存在）
cursor.execute("""
               CREATE TABLE IF NOT EXISTS ords (
                                                   k TEXT PRIMARY KEY,
                                                   v TEXT NOT NULL
               )
               """)
conn.commit()

# 模拟订单对象
def create_order(i):
    return {
        "order_id": str(uuid.uuid4()),
        "merchant_id": "M1001",
        "user_id": f"U{i}",
        "amount": i * 10,
        "timestamp": time.time()
    }


N = 1000  # 写入数量，可自行调整

start = time.time()
# 循环写入
for i in range(1, N+1):
    order = create_order(i)
    k = order["order_id"]
    v = json.dumps(order, ensure_ascii=False)

    cursor.execute("INSERT INTO ords (k, v) VALUES (?, ?)", (k, v))
    conn.commit()

    if(i mod 100==0)
      print(f"写入订单 {i}: {k}")


end = time.time()

elapsed = end - start
tps = N / elapsed

print(f"写入 {N} 条订单，总耗时: {elapsed:.4f} 秒")
print(f"平均 TPS: {tps:.2f} 条/秒")

conn.close()
print("完成写入")
