#!/system/bin/sh
# ==========================================================
# 本地服务器启动脚本
# 启动后浏览器访问: http://localhost:8080/
# ==========================================================

PY=/data/data/com.termux/files/usr/bin/python3

# 项目目录（脚本所在目录）
DIR="$(cd "$(dirname "$0")" 2>/dev/null && pwd)"
[ -z "$DIR" ] && DIR="/storage/emulated/0/AndroidIDEProjects/ThirdParty123pan/Cloud123Lite-web/"
cd "$DIR" || { echo "无法进入项目目录: $DIR"; exit 1; }

PORT="${1:-8080}"

# 检查 8080 端口是否已被占用
if curl -s -o /dev/null --max-time 2 "http://localhost:$PORT"; then
  echo "=============================================="
  echo "服务器已在运行！"
  echo "请在浏览器打开:"
  echo "  http://localhost:$PORT/"
  echo "=============================================="
  exit 0
fi

echo "正在启动本地服务器 (端口 $PORT) ..."
$PY -m http.server "$PORT" --bind 0.0.0.0 >/dev/null 2>&1 &
SERVER_PID=$!
sleep 1

# 验证是否启动成功
if curl -s -o /dev/null --max-time 2 "http://localhost:$PORT"; then
  # 将停止命令写入 stop‑server.sh
  echo "#!/system/bin/sh" > stop-server.sh
  echo "if curl -s -o /dev/null --max-time 2 'http://localhost:$PORT'; then" >> stop-server.sh
  echo "  kill $SERVER_PID" >> stop-server.sh
  echo "  echo '已结束服务器运行！'" >> stop-server.sh
  echo "exit 0" >> stop-server.sh
  echo "fi" >> stop-server.sh

  echo "=============================================="
  echo "✅ 服务器启动成功！(PID: $SERVER_PID)"
  echo ""
  echo "请打开浏览器访问:"
  echo "  http://localhost:$PORT/"
  echo "=============================================="
  echo ""
  echo "停止服务器执行:"
  echo "  sh stop-server.sh"
else
  echo "❌ 服务器启动失败，请检查端口是否被占用。"
  exit 1
fi
