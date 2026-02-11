import pyodbc
import time
import json
import numpy as np  
# C:\Users\Administrator\AppData\Local\Programs\Python\Python314\Scripts\pip3.exe install pyodbc
# C:\Users\Administrator\AppData\Local\Programs\Python\Python314\Scripts\pip3.exe install pyodbc
# C:\Users\attil\AppData\Local\Programs\Python\Python314\Scripts\pip3.exe install pyodbc
# C:\Users\attil\IdeaProjects\goDbScrpt\.venv\Scripts\pip3.exe install pyodbc
# C:\Users\Administrator\AppData\Local\Programs\Python\Python314\Scripts\pip3.exe install  rocksdict
# 1. 配置连接字符串 (注意：Python ODBC 连接字符串格式稍有不同)
# C:/Users/attil/IdeaProjects/goDbScrpt/.venv/Scripts/python.exe install pyodbc
def get_conn_str():
    # 从文件加载配置
    with open('c:/cfg/config.json', 'r', encoding='utf-8') as f:
        config = json.load(f)
    
    # 动态构建连接字符串
    return (
        f"DRIVER={{ODBC Driver 17 for SQL Server}};"
        f"SERVER={config['server']};"
        f"DATABASE={config['database']};"
        f"UID={config['uid']};"
        f"PWD={config['pwd']};"
        f"Encrypt={config['encrypt']};"
        f"TrustServerCertificate={config['trust_cert']};"
    )
conn_str = get_conn_str()

def run_tps_test(count=50000):
    try:
        # 建立连接
        conn = pyodbc.connect(conn_str)
        cursor = conn.cursor()
        cursor.fast_executemany = True # 即使是存储过程，也能优化底层绑定
        print(f"开始测试: 执行 {count} 次存储过程...")
        
        # 记录开始时间
        start_time = time.perf_counter()

        for i in range(count):
            print(i)
            try:
                # 开启事务 (pyodbc 默认 autocommit=False，所以实际上已经在事务中)
                # 执行存储过程
                cursor.execute("{CALL dbo.instSameUid}")
                # 提交事务
                conn.commit()
            except Exception as e:
                conn.rollback()
                print(f"第 {i} 次执行失败: {e}")
                break

        # 记录结束时间
        end_time = time.perf_counter()
        
        # 计算耗时
        total_seconds = end_time - start_time
        tps = count / total_seconds

        print("-" * 30)
        print(f"总耗时: {total_seconds:.2f} 秒")
        print(f"最终 TPS: {tps:.2f}")
        print("-" * 30)

    except Exception as e:
        print(f"数据库连接失败: {e}")
    finally:
        if 'conn' in locals():
            conn.close()

if __name__ == "__main__":
    run_tps_test(100)