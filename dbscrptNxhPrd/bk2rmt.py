# C:\Users\Administrator\AppData\Local\Programs\Python\Python313\Scripts\pip.exe install paramiko


import paramiko
import os

def upload_file_via_ssh(local_path, remote_path, hostname, port, username, password):
    # 创建 SSH 客户端
    ssh = paramiko.SSHClient()
    # 自动添加策略，保存服务器的 SSH 密钥（防止初次连接报错）
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        # 连接服务器
        print(f"正在连接到 {hostname}...")
        ssh.connect(hostname, port, username, password)
        
        # 创建 SFTP 会话
        sftp = ssh.open_sftp()
        
        # 开始上传
        print(f"正在上传 {local_path} 到 {remote_path}...")
        sftp.put(local_path, remote_path)
        
        print("传输成功！")
        
        # 关闭连接
        sftp.close()
        
    except Exception as e:
        print(f"传输过程中出错: {e}")
    finally:
        ssh.close()

if __name__ == "__main__":
    #//ssh -p 46648 soct@10.0.100.12
    # --- 配置参数 ---
    config = {
        "local_path": r"D:\backup\hxMatching_Full_20260210.bak",      # Windows 本地路径（注意 r 前缀处理转义）
        "remote_path": "/share2026/hxMatching_Full_20260210.bak",   # Linux 目标完整路径
        "hostname": "10.0.100.12",      # Linux 的 IP
        "port": 46648,                       # SSH 端口，默认 22
        "username": "scot",               # 用户名
        "password": "gONM#YxEntrYn45"       # 密码
    }
    
    upload_file_via_ssh(**config)