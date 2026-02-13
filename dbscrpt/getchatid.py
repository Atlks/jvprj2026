import requests
import os
import os

import os





def readFile(file_path):
    """
    读取文本文件内容，自动尝试 utf-8、utf-8-sig、gbk。
    不会去掉中间内容，只去掉首尾空白。
    """
    if not os.path.exists(file_path):
        print(f"文件不存在: {file_path}")
        return None

    encodings = ['utf-8', 'utf-8-sig', 'gbk']
    for enc in encodings:
        try:
            with open(file_path, 'r', encoding=enc) as f:
                content = f.read()
                if content:  # 只去掉首尾空格
                    return content.strip()
        except Exception:
            continue

    print("读取失败或文件为空")
    return None






def curDate():
    return datetime.now().strftime("%Y%m%d")

BOT_TOKEN = readFile(r"C:\cfg\bot_cfg.ini").strip()






url = f"https://api.telegram.org/bot{BOT_TOKEN}/getUpdates"
print(url)

resp = requests.get(url)
print(resp.json())
