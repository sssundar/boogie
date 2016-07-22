#!/bin/bash
#
# A Script to automatically (mostly) install Asterisk VoIP Server, FreePBX and
# Awesome PBX. Written specifically for an Ubuntu 14.04Amazon EC2 image, but
# it should be fairly portable.
#
# This script was written with the assistance of the following web pages-
#   http://wiki.freepbx.org/display/FOP/Installing+FreePBX+13+on+Ubuntu+Server+14.04.2+LTS
#   http://aliensgrin.com/2014/01/20/pbx-in-a-flash-piaf-on-amazon-ec2/
#   http://nerdvittles.com/?p=5060
#   http://nerdvittles.com/?p=9526
#   http://nerdvittles.com/?p=1119
#   http://nerdvittles.com/?p=14183
#
# Don't forget to practice smart computing and set secure user/root passwords
# via `sudo passwd` and `sudo passwd root`. This script is meant to be run as
# the root user. So, either enable root login via ssh, or execute `sudo -i`
# before running this script.

wip_dir=/usr/src

# Other important tools
apt-get install -y htop fail2ban tmux

# Set blank password for MySQL. FreePBX will set a strong password later during
# the install process.
echo "mysql-server-5.5 mysql-server/root_password password" | debconf-set-selections
echo "mysql-server-5.5 mysql-server/root_password_again password" | debconf-set-selections
# Install dependencies.
apt-get install -y build-essential linux-headers-`uname -r` openssh-server \
  apache2 mysql-server mysql-client bison flex php5 php5-curl php5-cli \
  php5-mysql php-pear php5-gd curl sox libncurses5-dev libssl-dev \
  libmysqlclient-dev mpg123 libxml2-dev libnewt-dev sqlite3 libsqlite3-dev \
  pkg-config automake libtool autoconf git unixodbc-dev uuid uuid-dev \
  libasound2-dev libogg-dev libvorbis-dev libcurl4-openssl-dev libical-dev \
  libneon27-dev libsrtp0-dev libspandsp-dev libmyodbc

# Create a swap partition - very helpful due to the fact that the EC2 micro
# instances are only allocated 1GB of RAM.
dd if=/dev/zero of=/swapfile bs=1024 count=1024k
chown root:root /swapfile
chmod 0600 /swapfile
mkswap /swapfile
swapon /swapfile
echo "/swapfile          swap            swap    defaults        0 0" >> /etc/fstab
sysctl vm.swappiness=10
echo vm.swappiness=10 >> /etc/sysctl.conf

# Legacy Pear requirements.
pear install Console_Getopt

# Google Voice Plugin dependencies.
cd $wip_dir
wget https://iksemel.googlecode.com/files/iksemel-1.4.tar.gz
tar xzf iksemel-1.4.tar.gz
cd iksemel-*
./configure
make
make install

# THIS IS ONLY NEEDED IF DAHDI HARDWARE PRESENT
# cd $wip_dir
# wget http://downloads.asterisk.org/pub/telephony/dahdi-linux-complete/dahdi-linux-complete-current.tar.gz
# tar xvfz dahdi-linux-complete-current.tar.gz
# rm -f dahdi-linux-complete-current.tar.gz
# cd dahdi-linux-complete-*
# make all
# make install
# make config
# cd /usr/src
# wget http://downloads.asterisk.org/pub/telephony/libpri/libpri-1.4-current.tar.gz
# tar xvfz libpri-1.4-current.tar.gz
# rm -f libpri-1.4-current.tar.gz
# cd libpri-*
# make
# make install

# PJSIP pjproject.
cd $wip_dir
wget http://www.pjsip.org/release/2.4/pjproject-2.4.tar.bz2
tar xjf pjproject-2.4.tar.bz2
cd pjproject-2.4
CFLAGS='-DPJ_HAS_IPV6=1' ./configure --enable-shared --disable-sound --disable-resample --disable-video --disable-opencore-amr
make dep
make
make install

# Jansson.
cd $wip_dir
wget -O jansson.tar.gz https://github.com/akheron/jansson/archive/v2.7.tar.gz
tar xzf jansson.tar.gz
cd jansson-*
autoreconf -i
./configure
make
make install

# Asterisk 13.
cd $wip_dir
wget http://downloads.asterisk.org/pub/telephony/asterisk/asterisk-13-current.tar.gz
tar xzf asterisk-13-current.tar.gz
cd asterisk-*
contrib/scripts/install_prereq install
./configure
contrib/scripts/get_mp3_source.sh
make menuselect
make
make install
make config
ldconfig
update-rc.d -f asterisk remove

# High quality sound files.
cd /var/lib/asterisk/sounds
wget http://downloads.asterisk.org/pub/telephony/sounds/asterisk-core-sounds-en-wav-current.tar.gz
wget http://downloads.asterisk.org/pub/telephony/sounds/asterisk-extra-sounds-en-wav-current.tar.gz
tar xzf asterisk-core-sounds-en-wav-current.tar.gz
tar xzf asterisk-extra-sounds-en-wav-current.tar.gz
rm -f asterisk-core-sounds-en-wav-current.tar.gz
rm -f asterisk-extra-sounds-en-wav-current.tar.gz

# Create asterisk user.
useradd -m asterisk
chown asterisk. /var/run/asterisk
chown -R asterisk. /etc/asterisk
chown -R asterisk. /var/{lib,log,spool}/asterisk
chown -R asterisk. /usr/lib/asterisk
rm -rf /var/www/html

# Apache configuration modifications.
sed -i 's/\(^upload_max_filesize = \).*/\120M/' /etc/php5/apache2/php.ini
cp /etc/apache2/apache2.conf /etc/apache2/apache2.conf_orig
sed -i 's/^\(User\|Group\).*/\1 asterisk/' /etc/apache2/apache2.conf
sed -i 's/AllowOverride None/AllowOverride All/' /etc/apache2/apache2.conf
service apache2 restart

# Configure ODBC.
cat >> /etc/odbcinst.ini << EOF
[MySQL]
Description = ODBC for MySQL
Driver = /usr/lib/x86_64-linux-gnu/odbc/libmyodbc.so
Setup = /usr/lib/x86_64-linux-gnu/odbc/libodbcmyS.so
FileUsage = 1
 
EOF

cat >> /etc/odbc.ini << EOF
[MySQL-asteriskcdrdb]
Description=MySQL connection to 'asteriskcdrdb' database
driver=MySQL
server=localhost
database=asteriskcdrdb
Port=3306
Socket=/var/run/mysqld/mysqld.sock
option=3
 
EOF

# Finally FreePBX.
cd $wip_dir
wget http://mirror.freepbx.org/modules/packages/freepbx/freepbx-13.0-latest.tgz
tar xzf freepbx-13.0-latest.tgz
cd freepbx
./start_asterisk start
./install -n

# Cleanup
cd $wip_dir
rm -rf asterisk-* iksemel-* jansson* freepbx* pjproject-2.4*

echo "Initial FreePBX install and configuration complete. Please reboot your EC2"
echo "instance and continue with FreePBX configuration by visiting http://`ifconfig | awk -F "[: ]+" '/inet addr:/ { if ($4 != "127.0.0.1") print $4 }'`"
echo "and setting up the administrator account for FreePBX."
