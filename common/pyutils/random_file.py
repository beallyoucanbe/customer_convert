import bisect
import os
import random
import sys
import tempfile
import time

BUFFER_SIZE = 32


def modify_file(input_file_name, cnt, output_file_name):
    with open(input_file_name) as fin:
        s = time.clock()
        N = cnt
        sys.stderr.write("Shuffling {0} lines...\n".format(N))
        with open(output_file_name, "w") as output_file:

            def random_permutation(N):
                length = list(range(N))
                for i, n in enumerate(length):
                    r = random.randint(0, i)
                    length[i] = length[r]
                    length[r] = n
                return length

            p = random_permutation(N)
            ridx = [0] * N
            files = []
            mx = []
            sys.stderr.write("Computing list of temporary files\n")
            for i, n in enumerate(p):
                pos = bisect.bisect_left(mx, n) - 1
                if pos == -1:
                    files.insert(0, [n])
                    mx.insert(0, n)
                else:
                    files[pos].append(n)
                    mx[pos] = n
            P = len(files)
            sys.stderr.write("Caching to {0} temporary files\n".format(P))
            fps_names = [
                tempfile.mktemp(prefix="hmf_data_", suffix=".csv") for i in range(P)
            ]
            # 建立缓冲区，将数据先写入缓冲区，待缓冲区满后再写入文件
            buffers_list = [[] for _ in range(P)]
            for file_index, line_list in enumerate(files):
                for line in line_list:
                    ridx[line] = file_index
            for i, line in enumerate(fin):
                buffers_list[ridx[i]].append(line)
                if len(buffers_list[ridx[i]]) >= BUFFER_SIZE:
                    with open(fps_names[ridx[i]], mode="a") as fp:
                        fp.writelines(buffers_list[ridx[i]])
                    buffers_list[ridx[i]].clear()

            sys.stderr.write("Writing to the shuffled file\n")
            for i in range(N):
                if os.path.exists(fps_names[ridx[p[i]]]):
                    with open(fps_names[ridx[p[i]]], mode="r") as fp:
                        for line in fp:
                            output_file.write(line)
                    os.remove(fps_names[ridx[p[i]]])
                # 将 buffer 里剩下的行写入
                if buffers_list[ridx[p[i]]]:
                    output_file.writelines(buffers_list[ridx[p[i]]])
                    buffers_list[ridx[p[i]]].clear()
        e = time.clock()
        sys.stderr.write("Shuffling took an overall of {0} secs\n".format(e - s))


if __name__ == "__main__":
    input_file = sys.argv[1]
    cnt = int(sys.argv[2])
    output_file = sys.argv[3]
    modify_file(input_file, cnt, output_file)
