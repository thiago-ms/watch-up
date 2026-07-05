#!/bin/bash
ip=$(ip route get 1.1.1.1 2>/dev/null | grep -oP 'src \K[\d.]+')
echo "ip: $ip"
echo "url: $ip:8000/watchup-x.x-debug.apk"
python3 -m http.server 8000 --bind 0.0.0.0 --directory dist
