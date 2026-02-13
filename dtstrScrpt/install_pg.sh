#!/bin/bash
set -e
# chmod +x install_pg.sh
  #sudo ./install_pg.sh
# ===== 配置 =====
PG_DB="datacenter"
PG_USER="dcuser2026"
PG_PASS="25K3fzGNftKw2ls425K3fzGNftKw2ls4"



echo "=== 安装 PostgreSQL ==="
apt update
apt install -y postgresql postgresql-contrib

echo "=== 启动 PostgreSQL ==="
systemctl enable postgresql
systemctl start postgresql

echo "=== 创建数据库与用户 ==="

sudo -u postgres psql <<EOF
-- 创建用户
CREATE USER ${PG_USER} WITH PASSWORD '${PG_PASS}';

-- 创建数据库
CREATE DATABASE ${PG_DB}
    OWNER ${PG_USER}
    ENCODING 'UTF8'
    LC_COLLATE='C'
    LC_CTYPE='C'
    TEMPLATE template0;

-- 授权
GRANT ALL PRIVILEGES ON DATABASE ${PG_DB} TO ${PG_USER};
EOF

echo "=== PostgreSQL 初始化完成 ==="
echo "数据库: ${PG_DB}"
echo "用户: ${PG_USER}"



