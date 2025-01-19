import mysql.connector

# 替换以下变量为您的 MySQL 服务器信息
host = "127.0.0.1"
user = "root"
password = "my-secret-pw"
port = 3306
# 创建数据库连接
try:
    conn = mysql.connector.connect(host=host, user=user, password=password, port=port)

    if conn.is_connected():
        cursor = conn.cursor()

        # 执行 SHOW DATABASES 查询
        cursor.execute("select action_type, count(*) as cnt from events where event_time > '2025-01-17 00:00:00' and event_time < '2025-01-19 00:00:00' group by action_type")


        # 获取查询结果
        databases = cursor.fetchall()

        # 遍历每个数据库并打印其中的表
        for database in databases:
            db_name = database[0]
            if db_name in invalid_database:
                continue
            # print(f"Database: {db_name}")

            # 连接到当前数据库
            cursor.execute(f"USE {db_name}")

            # 执行 SHOW TABLES 查询
            cursor.execute("SHOW TABLES")

            # 获取表列表
            tables = cursor.fetchall()

            # 打印表列表
            for table in tables:
                ## 如果表名
                # print(f"Table: {table[0]}")
                if table[0] == "tb_cubedef":
                    print(f"Database: {db_name}")
                    print(f"Table: {table[0]}")

except mysql.connector.Error as err:
    print(f"错误：{err}")

finally:
    if conn.is_connected():
        cursor.close()
        conn.close()
