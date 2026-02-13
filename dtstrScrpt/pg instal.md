🔍 验证
psql -h localhost -U dcuser2026 -d datacenter


输入密码：

25K3fzGNftKw2ls4


⚠️ 如果你需要 远程连接

默认 PostgreSQL 只允许本地连接，需要额外配置。

1️⃣ 修改 postgresql.conf
sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" \
/etc/postgresql/*/main/postgresql.conf

2️⃣ 修改 pg_hba.conf
echo "host all all 0.0.0.0/0 md5" | sudo tee -a /etc/postgresql/*/main/pg_hba.conf

3️⃣ 重启 PostgreSQL
sudo systemctl restart postgresql

🔐 安全提醒（重要）

当前脚本 开放了明文密码

生产建议：

限定 IP

使用强防火墙规则

或用证书登录


# 调优pg 参数性能