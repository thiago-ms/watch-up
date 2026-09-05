#!/bin/bash
port="${1:-8000}"
ip=$(ip route get 1.1.1.1 2>/dev/null | grep -oP 'src \K[\d.]+')
echo "ip: $ip"
echo "url: http://$ip:$port/"
#app=$(echo "${PWD##*/}")
app="WatchUp"
{
  echo '<!DOCTYPE html>'
  echo '<html><head>'
  echo '<meta name="viewport" content="width=device-width, initial-scale=1">'
  echo '</head><body>'
  echo "<h3>$app</h3>"
  echo '<table border="1">'
  for f in dist/*.apk; do
    [ -e "$f" ] || continue
    name=$(basename "$f")
    printf '<tr><td><a href="%s">%s</a></td></tr>\n' "$name" "$name"
  done
  echo '</table>'
  echo '</body></html>'
} > dist/index.html
python3 -m http.server "$port" --bind 0.0.0.0 --directory dist
