#!/bin/bash
DATE=$(date +\%Y-\%m-\%d)
BACKUP_FILE="/opt/backup/mysql/backup-$DATE" + ".sql"
# 执行备份
mysqldump -h rm-uf6ww10yk39fbw7pq.mysql.rds.aliyuncs.com -u ai --password=XO-P_c-g4posTF# --databases db_ai > $BACKUP_FILE