#!/usr/bin/env python3
import os
import subprocess
import sys
from datetime import datetime
from typing import Any
import json
# staic chk  mypy pg_full_backup.py
# ver 211
# ============================================
# PostgreSQL 全量备份（Python版）
# - 不新建 PG 用户
# - 使用 postgres 系统用户
# - 每日全量 pg_dump
# - 自动清理旧备份
# 0 2 * * * python3 /scrpt/pg_full_backup.py

# ============================================

# ===== 基本配置 =====
DB_NAME = "livechat_db"
BACKUP_DIR = "/backup"
RETENTION_DAYS = 7   # 当前脚本里未做清理，保留字段
PG_BIN = "/usr/bin/pg_dump"
LOG_FILE = "/backup/pg_backup.log"

DATE = datetime.now().strftime("%Y-%m-%d")
TIME = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
dtTIME = datetime.now().strftime("%Y-%m-%dT%H_%M_%S")
BACKUP_FILE = f"{BACKUP_DIR}/{DB_NAME}_fullbk_{dtTIME}.dump"
print("bkfl:"+BACKUP_FILE)
def log(msg: str):
    print(msg)
    with open(LOG_FILE, "a") as f:
        f.write(f"[{TIME}] {msg}\n")


def encodeJson(obj: Any) -> str:
    """
    将任意对象编码为 JSON 字符串。

    Args:
        obj (Any): 要编码的对象（支持 dict、list、str、int、float 等）

    Returns:
        str: JSON 格式字符串
    """
    try:
        return json.dumps(obj, ensure_ascii=False)
    except (TypeError, ValueError) as e:
        raise ValueError(f"对象无法编码为 JSON: {e}")


def main():
    log(f"START backup {DB_NAME}")

    os.makedirs(BACKUP_DIR, exist_ok=True)

    # ===== 执行备份 =====
    cmd = [
        "sudo", "-u", "postgres",
        PG_BIN, "-Fc", DB_NAME
    ]
    log(encodeJson(cmd))

    log("")
    try:
        with open(BACKUP_FILE, "wb") as out:
            result = subprocess.run(
                cmd,
                stdout=out,
                stderr=subprocess.PIPE,
                check=False
            )

        if result.returncode != 0:
            log("ERROR backup failed!")
            os.remove(BACKUP_FILE)
            sys.exit(1)

    except Exception as e:
        log(f"ERROR exception: {e}")
        if os.path.exists(BACKUP_FILE):
            os.remove(BACKUP_FILE)
        sys.exit(1)

    # ===== 备份大小 =====
    size = subprocess.check_output(
        ["du", "-h", BACKUP_FILE]
    ).decode().split()[0]

    log(f"SUCCESS backup created: {BACKUP_FILE} ({size})")
    log("-" * 50)

print(f"666")
log(777)


if __name__ == "__main__":
    main()
