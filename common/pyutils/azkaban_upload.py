# -*- coding: utf-8 -*-
import argparse
from subprocess import getstatusoutput


parser = argparse.ArgumentParser(description="Azkaban uploader.")
parser.add_argument(
    "-f", "--file_dir", default="./default__1__def.zip", help="输入azkaban配置文件地址，默认为当前目录default__1__def.zip文件"
)
parser.add_argument(
    "-u", "--user_name", default="azkaban", help="登录azkaban的用户名，默认为azkaban"
)
parser.add_argument(
    "-p", "--password", default="SensorsData2017", help="登录azkaban的密码"
)
parser.add_argument(
    "-j", "--project", default="default__1__def", help="azkaban中创建的项目名"
)

args = parser.parse_args()

log_in_command = f'curl -s -k -X POST --data "action=login&username={args.user_name}&password={args.password}" http://localhost:9901'
status, output = getstatusoutput(log_in_command)
session_id = None
if status:
    print("something error happens! error is: %s" % output)
else:
    output = eval(output)
    session_id = output["session.id"]

if session_id:
    upload_command = f"""
        curl -k -i -H "Content-Type: multipart/mixed" -X POST --form "session.id={session_id}" --form "ajax=upload" \
            --form "file=@{args.file_dir};type=application/zip" \
            --form "project={args.project}" http://localhost:9901/manager
    """
    upload_status, upload_output = getstatusoutput(upload_command)
    if upload_status:
        print(f"upload failed, error is {upload_output}")
    else:
        print("upload succeed!")
