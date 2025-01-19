#!/bin/bash
# 设置变量
CONTAINER_NAME="mysql8.0"
BACKUP_DIR="/opt/backup/mysql"
DATE=$(date +\%Y-\%m-\%d)
BACKUP_FILE="$BACKUP_DIR/backup-$DATE.sql"
DATABASE_NAME="customer"

# 创建备份目录（如果不存在）
mkdir -p $BACKUP_DIR

# 执行备份
docker exec $CONTAINER_NAME /usr/bin/mysqldump -u root --password=my-secret-pw $DATABASE_NAME > $BACKUP_FILE

# 删除超过7天的备份文件
find $BACKUP_DIR -type f -name "*.sql" -mtime +7 -exec rm {} \;

INSIGHT_BACKUP_DIR="/home/opsuser/hsw/chat_insight-main"
INSIGHT_BACKUP_FILE="/home/opsuser/hsw/backup/chat_insight-main-$DATE.tar"
sudo tar -cPf $INSIGHT_BACKUP_FILE $INSIGHT_BACKUP_DIR

sudo find /home/opsuser/hsw/backup/ -type f -name "*.tar" -mtime +7 -exec rm {} \;