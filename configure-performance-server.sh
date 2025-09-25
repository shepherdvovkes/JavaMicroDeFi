#!/bin/bash

# Performance Server Configuration Script
# This script configures the system to prevent sleep and optimize for server performance

set -e

echo "🚀 Configuring system for performance server operation..."

# Function to check if running as root
check_root() {
    if [[ $EUID -eq 0 ]]; then
        echo "⚠️  This script should NOT be run as root. Please run as regular user."
        echo "   The script will use sudo when needed."
        exit 1
    fi
}

# Function to disable sleep, suspend, and hibernation
disable_sleep_modes() {
    echo "🔧 Disabling sleep, suspend, and hibernation modes..."
    
    # Disable systemd sleep targets
    sudo systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target
    
    # Configure systemd logind to ignore power events
    sudo mkdir -p /etc/systemd/logind.conf.d/
    sudo tee /etc/systemd/logind.conf.d/no-suspend.conf > /dev/null <<EOF
[Login]
HandlePowerKey=ignore
HandleSuspendKey=ignore
HandleHibernateKey=ignore
HandleLidSwitch=ignore
HandleLidSwitchExternalPower=ignore
HandleLidSwitchDocked=ignore
IdleAction=ignore
IdleActionSec=0
EOF
    
    echo "✅ Sleep modes disabled"
}

# Function to set CPU governor to performance
set_performance_governor() {
    echo "🔧 Setting CPU governor to performance mode..."
    
    # Install cpufrequtils if not present
    if ! command -v cpufreq-set &> /dev/null; then
        echo "📦 Installing cpufrequtils..."
        sudo apt update
        sudo apt install -y cpufrequtils
    fi
    
    # Set all CPUs to performance governor
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        if [[ -f "$cpu" ]]; then
            echo performance | sudo tee "$cpu" > /dev/null
        fi
    done
    
    # Make it persistent
    sudo tee /etc/default/cpufrequtils > /dev/null <<EOF
# CPU frequency scaling governor
GOVERNOR="performance"
MAX_SPEED="0"
MIN_SPEED="0"
EOF
    
    echo "✅ CPU governor set to performance"
}

# Function to optimize power management
optimize_power_settings() {
    echo "🔧 Optimizing power management settings..."
    
    # Disable USB autosuspend
    sudo tee /etc/udev/rules.d/50-usb-power.rules > /dev/null <<EOF
# Disable USB autosuspend
ACTION=="add", SUBSYSTEM=="usb", TEST=="power/control", ATTR{power/control}="on"
EOF
    
    # Configure kernel parameters for performance
    sudo tee /etc/sysctl.d/99-performance-server.conf > /dev/null <<EOF
# Performance server optimizations
vm.swappiness=10
vm.vfs_cache_pressure=50
vm.dirty_background_ratio=5
vm.dirty_ratio=10
net.core.rmem_max=134217728
net.core.wmem_max=134217728
net.ipv4.tcp_rmem=4096 65536 134217728
net.ipv4.tcp_wmem=4096 65536 134217728
net.ipv4.tcp_congestion_control=bbr
kernel.sched_migration_cost_ns=5000000
EOF
    
    # Apply sysctl settings
    sudo sysctl -p /etc/sysctl.d/99-performance-server.conf
    
    echo "✅ Power management optimized"
}

# Function to configure GRUB for performance
configure_grub() {
    echo "🔧 Configuring GRUB for performance..."
    
    # Backup original GRUB config
    sudo cp /etc/default/grub /etc/default/grub.backup.$(date +%Y%m%d_%H%M%S)
    
    # Add performance parameters to GRUB
    if ! grep -q "intel_idle.max_cstate=1" /etc/default/grub; then
        sudo sed -i 's/GRUB_CMDLINE_LINUX_DEFAULT="[^"]*/& intel_idle.max_cstate=1 processor.max_cstate=1 idle=poll/' /etc/default/grub
    fi
    
    if ! grep -q "nohz=off" /etc/default/grub; then
        sudo sed -i 's/GRUB_CMDLINE_LINUX_DEFAULT="[^"]*/& nohz=off highres=off/' /etc/default/grub
    fi
    
    # Update GRUB
    sudo update-grub
    
    echo "✅ GRUB configured for performance"
}

# Function to create systemd service for persistent settings
create_systemd_service() {
    echo "🔧 Creating systemd service for persistent performance settings..."
    
    sudo tee /etc/systemd/system/performance-server.service > /dev/null <<EOF
[Unit]
Description=Performance Server Configuration
After=multi-user.target

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/usr/local/bin/apply-performance-settings.sh
User=root

[Install]
WantedBy=multi-user.target
EOF
    
    # Create the performance settings script
    sudo tee /usr/local/bin/apply-performance-settings.sh > /dev/null <<'EOF'
#!/bin/bash

# Apply performance settings on boot
echo "Applying performance server settings..."

# Set CPU governor to performance
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    if [[ -f "$cpu" ]]; then
        echo performance > "$cpu"
    fi
done

# Disable CPU idle states for maximum performance
for cpu in /sys/devices/system/cpu/cpu*/cpuidle/state*/disable; do
    if [[ -f "$cpu" ]]; then
        echo 1 > "$cpu"
    fi
done

# Set CPU frequency to maximum
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq; do
    if [[ -f "$cpu" ]]; then
        max_freq=$(cat "$cpu")
        echo "$max_freq" > "${cpu/scaling_max_freq/scaling_min_freq}"
    fi
done

# Disable power saving for network interfaces
for iface in /sys/class/net/*/device/power/control; do
    if [[ -f "$iface" ]]; then
        echo on > "$iface"
    fi
done

echo "Performance settings applied successfully"
EOF
    
    sudo chmod +x /usr/local/bin/apply-performance-settings.sh
    sudo systemctl enable performance-server.service
    
    echo "✅ Systemd service created and enabled"
}

# Function to install monitoring tools
install_monitoring_tools() {
    echo "🔧 Installing performance monitoring tools..."
    
    sudo apt update
    sudo apt install -y \
        htop \
        iotop \
        nethogs \
        powertop \
        stress \
        cpufrequtils \
        linux-tools-common \
        linux-tools-generic
    
    echo "✅ Monitoring tools installed"
}

# Function to create monitoring script
create_monitoring_script() {
    echo "🔧 Creating system monitoring script..."
    
    tee /home/vovkes/JavaMicroDeFi/monitor-performance.sh > /dev/null <<'EOF'
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
EOF
    
    chmod +x /home/vovkes/JavaMicroDeFi/monitor-performance.sh
    
    echo "✅ Monitoring script created at /home/vovkes/JavaMicroDeFi/monitor-performance.sh"
}

# Function to show final instructions
show_final_instructions() {
    echo -e "\n🎉 Performance server configuration completed!"
    echo "=============================================="
    echo ""
    echo "📋 What was configured:"
    echo "  ✅ Sleep, suspend, and hibernation disabled"
    echo "  ✅ CPU governor set to performance mode"
    echo "  ✅ Power management optimized"
    echo "  ✅ GRUB configured for maximum performance"
    echo "  ✅ Systemd service created for persistent settings"
    echo "  ✅ Monitoring tools installed"
    echo ""
    echo "🔄 Next steps:"
    echo "  1. Restart your system to apply all changes: sudo reboot"
    echo "  2. After restart, run: ./monitor-performance.sh"
    echo "  3. Verify settings with: systemctl status performance-server.service"
    echo ""
    echo "📊 Monitoring commands:"
    echo "  • System performance: ./monitor-performance.sh"
    echo "  • CPU usage: htop"
    echo "  • I/O usage: sudo iotop"
    echo "  • Network usage: sudo nethogs"
    echo "  • Power usage: sudo powertop"
    echo ""
    echo "⚠️  Note: These settings prioritize performance over power efficiency."
    echo "   Your system will consume more power but provide maximum performance."
}

# Main execution
main() {
    check_root
    disable_sleep_modes
    set_performance_governor
    optimize_power_settings
    configure_grub
    create_systemd_service
    install_monitoring_tools
    create_monitoring_script
    show_final_instructions
}

# Run main function
main "$@"
