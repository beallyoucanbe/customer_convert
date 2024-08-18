import sys

n = int(sys.argv[2])

with open(sys.argv[1]) as f:
    data = f.readline()
    cnt = 0
    content_list = []
    while data:
        single_data = eval(data.strip())
        if 'content' in single_data:
            content_list.append(single_data['content'])
            cnt += 1
        if cnt == n:
            res = {"user": single_data['user'],
                   "user_action_contents": single_data['user_action_contents'],
                   "content": content_list}
            print(res)
            cnt = 0
            content_list = []
        data = f.readline()
