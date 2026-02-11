#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import redis

# ===== Redis 配置 =====
REDIS_CONFIG = {
    "host22": "127.0.0.1",
    "host": "161.248.15.142",         # addr 中的 IP
    "port": 6379,                # addr 中的端口
    "password": "dNF6NsAxkAZpPKwJ",
    "db": 0                       # 程序配置 db
}
# pip3 install redis
# C:\Users\attil\AppData\Local\Programs\Python\Python314\python.exe -m pip install redis
def main():
    # 连接 Redis
    try:
        r = redis.Redis(
            host=REDIS_CONFIG["host"],
            port=REDIS_CONFIG["port"],
        #    password=REDIS_CONFIG["password"],
            db=REDIS_CONFIG["db"],
            socket_timeout=5 , # duxie超时 5 秒
            socket_connect_timeout=5,  # 连接超时
            decode_responses=True   # 自动把 bytes 转 str
        )
        # 测试连接
        pong = r.ping()
        if pong:
            print("✅ Redis 连接成功")

        # 写入 key-value
        key = "k2"
        value = "cn"
        r.set(key, value)
        print(f"✅ 写入成功: {key} = {r.get(key)}")
    except redis.exceptions.TimeoutError:
        print("Redis 连接超时！")
    except Exception as e:
        print(f"❌ Redis 连接或写入失败: {e}")

if __name__ == "__main__":
    main()
