import os
import requests
from datetime import datetime
import os
# ===== 配置 =====


def readFile(file_path):
    """
    读取指定文件内容，如果文件不存在返回 None
    :param file_path: 文件完整路径
    :return: 文件内容字符串 或 None
    """
    if not os.path.exists(file_path):
        print(f"文件不存在: {file_path}")
        return None

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception as e:
        print(f"读取文件失败: {e}")
        return None



def curDate():
    return datetime.now().strftime("%Y%m%d")

BOT_TOKEN = readFile("/cfg/bot_cfg.ini")  # 例如 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ
CHAT_ID = "-1003854160906"           # 你的个人或群组ID

curDate=curDate()
BACKUP_FILE = f"D:\backup\hxMatching_Full_{curDate}.bak"

# ===== 检查文件 =====
if os.path.exists(BACKUP_FILE):
    message = f"文件备份成功 ✅\n路径: {BACKUP_FILE}\nprj:内循环项目"
else:
    message = f"备份文件不存在 ❌\n路径: {BACKUP_FILE}"

# ===== 发送 Telegram 消息 =====
url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
payload = {
    "chat_id": CHAT_ID,
    "text": message
}

try:
    resp = requests.post(url, data=payload)
    if resp.status_code == 200:
        print("消息发送成功")
    else:
        print(f"发送失败: {resp.status_code} {resp.text}")
except Exception as e:
    print(f"请求异常: {e}")
