npm run dev即可了 。。
node serv好像也可
无需自己实现websvr



✅ 3️⃣ SPA fallback 顺序是对的
stat → fallback → 再算 mime → 再算 cache


✔ history 模式
✔ 不会错缓存
✔ index.html 不被 immutable


✅ 4️⃣ 安全边界是“正确姿势”
path.resolve + startsWith(root)


✔ 没有路径穿越
✔ 没有编码绕过
✔ 没有相对路径坑