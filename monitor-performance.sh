#!/bin/bash

# Performance Monitoring Script
echo "🖥️  System Performance Monitor"
echo "=============================="

echo "📊 CPU Information:"
echo "CPU Governor: $(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo 'N/A')"
echo "CPU Frequency: $(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null || echo 'N/A') kHz"
echo "CPU Cores: $(nproc)"

echo -e "\n💾 Memory Information:"
free -h

echo -e "\n🔋 Power Management Status:"
echo "Sleep targets masked: $(systemctl is-masked sleep.target suspend.target hibernate.target hybrid-sleep.target | grep -c masked)/4"

echo -e "\n🌡️  CPU Temperature (if available):"
if command -v sensors &> /dev/null; then
    sensors | grep -i temp | head -5
else
    echo "Install lm-sensors for temperature monitoring: sudo apt install lm-sensors"
fi

echo -e "\n📈 Load Average:"
uptime

echo -e "\n🔄 System Uptime:"
uptime -p

echo -e "\n🚀 Java Processes:"
ps aux | grep java | grep -v grep | head -10

