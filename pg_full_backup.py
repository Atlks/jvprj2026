#!/usr/bin/env python3
import os
import subprocess
import sys
from datetime import datetime

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
TIME = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
BACKUP_FILE = f"{BACKUP_DIR}/{DB_NAME}_{DATE}.dump"

def log(msg: str):
    print(msg)
    with open(LOG_FILE, "a") as f:
        f.write(f"[{TIME}] {msg}\n")

def main():
    log(f"START backup {DB_NAME}")

    os.makedirs(BACKUP_DIR, exist_ok=True)

    # ===== 执行备份 =====
    cmd = [
        "sudo", "-u", "postgres",
        PG_BIN, "-Fc", DB_NAME
    ]

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


if __name__ == "__main__":
    main()
