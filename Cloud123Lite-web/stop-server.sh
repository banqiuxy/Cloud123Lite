#!/system/bin/sh
if curl -s -o /dev/null --max-time 2 'http://localhost:8080'; then
  kill 21351
  echo '已结束服务器运行！'
exit 0
fi
