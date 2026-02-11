from flask import Flask, request, jsonify

app = Flask(__name__)
# C:\Users\Administrator\AppData\Local\Programs\Python\Python314\Scripts\pip3.exe install flask
# C:\Users\attil\IdeaProjects\goDbScrpt\.venv\Scripts\pip3.exe -m install flask
# 模拟一个简单的内存数据库
data_store = {}

@app.route('/set', methods=['GET'])
def set_kv():
    # 从 URL 参数中获取 k 和 v
    key = request.args.get('k')
    value = request.args.get('v')

    if not key or not value:
        return jsonify({"error": "请提供参数 k 和 v"}), 400

    # 存入字典
    data_store[key] = value

    return jsonify({
        "status": "success",
        "message": f"已设置 {key} = {value}",
        "current_data": data_store
    })

if __name__ == '__main__':
    # 运行在本地 5000 端口
    #app.run(debug=True)
    app.run(host='0.0.0.0', port=80, debug=True)