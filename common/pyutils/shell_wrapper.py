# coding: utf-8
"""
模仿师姐sa shell_wrapper用法改写, 用于sr,
只实现了部分功能.
"""
import subprocess
import sys
import time
from concurrent import futures
from itertools import zip_longest

import paramiko

default_print_fun = lambda x: print(x, file=sys.stderr)
none_print_fun = lambda x: None


def run_cmd(cmd, print_fun=default_print_fun, timeout=600):
    """
    运行sh命令, 待执行完毕输出状态与结果.
    :param cmd: command
    :param print_fun:
    :param timeout:
    :return:
    """
    p = subprocess.Popen(
        cmd,
        shell=True,
        universal_newlines=True,
        stderr=subprocess.PIPE,
        stdout=subprocess.PIPE,
    )
    try:
        stdout, stderr = p.communicate(timeout=timeout)
    except subprocess.TimeoutExpired:
        print_fun("timeout")
        p.kill()
        stdout, stderr = p.communicate()
    ret = p.returncode

    print_fun("======>")
    print_fun(
        "cmd:\n%s\n\nstdout:\n%s\n\nstderr:\n%s\n\nret:%d\n"
        % (cmd, stdout, stderr, ret)
    )

    return {"ret": ret, "stdout": stdout, "stderr": stderr}


def run_cmd_with_continuous_output(cmd, print_fun=default_print_fun, timeout=600):
    """
    运行sh命令, 执行过程中输出stdout, 待执行完毕输出状态与结果.
    :param cmd: command
    :param print_fun:
    :param timeout:
    :return:
    """

    def timer(proc, timeout):
        """
        超时计时器
        :param proc: process
        :param timeout:
        :return:
        """
        try:
            proc.wait(timeout)
        except subprocess.TimeoutExpired as err:
            print_fun("\n*Timeout*")
            print_fun(err)
            proc.kill()

    p = subprocess.Popen(
        cmd, bufsize=0, shell=True, universal_newlines=True, stderr=None, stdout=None
    )

    exe = futures.ThreadPoolExecutor(max_workers=1)
    exe.submit(timer, p, timeout)

    print_fun("======>")
    print_fun("cmd:\n%s\n\nstdout & stderr:\n" % cmd)

    p.wait()
    print_fun("\n\nret:\n%d\n" % p.returncode)

    return {"ret": p.returncode}


def __assert_ret(ret, cmd, action):
    if ret != 0:
        if action:
            raise Exception("failed to %s! ret=%d" % (action, ret))
        else:
            raise Exception("failed to run[%s]! ret=%d" % (cmd, ret))


def call(cmd, print_fun=default_print_fun, timeout=600, stream_output=True):
    """
    统一 run_cmd/run_cmd_with_continous_output 的入口, 返回执行结果.
    :param cmd:
    :param print_fun:
    :param timeout:
    :param stream_output:
    :return:
    """
    if stream_output:
        return run_cmd_with_continuous_output(cmd, print_fun, timeout)["ret"]
    else:
        return run_cmd(cmd, print_fun, timeout)["ret"]


def check_call(
    cmd, print_fun=default_print_fun, timeout=600, stream_output=True, action=None
):
    """
    统一 run_cmd/run_cmd_with_continous_output 的入口, 无返回, 如执行失败抛异常
    :param cmd:
    :param print_fun:
    :param timeout:
    :param stream_output: 是否需要实时输出stdout
    :param action: command 任务描述
    :return:
    """
    if stream_output:
        result = run_cmd_with_continuous_output(cmd, print_fun, timeout)
    else:
        result = run_cmd(cmd, print_fun, timeout)
    __assert_ret(result["ret"], cmd, action)


def check_output(
    cmd, print_fun=default_print_fun, timeout=600, stream_output=True, action=None
):
    """
    统一 run_cmd/run_cmd_with_continous_output 的入口, 正常返回stdout, 如执行失败抛异常
    :param cmd:
    :param print_fun:
    :param timeout:
    :param stream_output:
    :param action:
    :return:
    """
    # if stream_output:
    #     result = run_cmd_with_continuous_output(cmd, print_fun, timeout)
    # else:
    #     result = run_cmd(cmd, print_fun, timeout)
    # todo: stream模式未重定向PIPE, 无法捕获stdout/stderr输出
    result = run_cmd(cmd, print_fun, timeout)
    __assert_ret(result["ret"], cmd, action)
    return result.get("stdout", None)


class ShellClient(object):
    """
    同SSHClient暴露的接口一致, 用于本机操作
    """

    def __init__(self, print_fun=None):
        self.print_fun = print_fun if print_fun else lambda x: print(x, file=sys.stderr)
        self.run_cmd = self.call

    def call(self, cmd, print_fun=None, timeout=600, stream_output=True):
        print_fun = print_fun if print_fun else self.print_fun
        return call(
            cmd, print_fun=print_fun, timeout=timeout, stream_output=stream_output
        )

    def check_call(
        self, cmd, print_fun=None, timeout=600, stream_output=True, action=None
    ):
        print_fun = print_fun if print_fun else self.print_fun
        return check_call(
            cmd,
            print_fun=print_fun,
            timeout=timeout,
            stream_output=stream_output,
            action=action,
        )

    def check_output(
        self, cmd, print_fun=None, timeout=600, stream_output=True, action=None
    ):
        print_fun = print_fun if print_fun else self.print_fun
        return check_output(
            cmd,
            print_fun=print_fun,
            timeout=timeout,
            stream_output=stream_output,
            action=action,
        )


class SSHClient(object):
    """
    借助paramiko实现跨机操作
    """

    def __init__(
        self, host, name=None, password=None, encoding="utf8", port=22, print_fun=None
    ):
        self.print_fun = print_fun if print_fun else lambda x: print(x, file=sys.stderr)
        self.host = host
        self.params = {"hostname": host, "port": port}
        if name:
            self.params["username"] = name
        if password:
            self.params["password"] = password
        self.encoding = encoding
        self.client = paramiko.SSHClient()
        self.is_connected = False

    def __repr__(self):
        return "SSHClient(host=%s)" % self.host

    def check_connect(self):
        # todo: 保证构造函数不挂掉的意义？
        if self.is_connected:
            return
        self.client.load_system_host_keys()
        self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        self.client.connect(**self.params, banner_timeout=60)
        self.is_connected = True

    def _run_cmd(self, cmd, print_fun=None, timeout=600):
        print_fun = print_fun if print_fun else self.print_fun
        self.check_connect()
        stdin_fd, stdout_fd, stderr_fd = self.client.exec_command(cmd, timeout=timeout)
        ret = stdout_fd.channel.recv_exit_status()
        stdout, stderr = (
            stdout_fd.read().decode(self.encoding),
            stderr_fd.read().decode(self.encoding),
        )

        print_fun("======>")
        print_fun(
            "cmd:\n%s\n\nstdout:\n%s\n\nstderr:\n%s\n\nret:%d\n\n"
            % (cmd, stdout, stderr, ret)
        )
        return {"ret": ret, "stdout": stdout, "stderr": stderr}

    def _run_cmd_with_continuous_output(self, cmd, print_fun=None, timeout=600):
        print_fun = print_fun if print_fun else self.print_fun
        self.check_connect()
        stdin_fd, stdout_fd, stderr_fd = self.client.exec_command(cmd, timeout=timeout)

        print_fun("======>")
        print_fun("cmd:\n%s\n\nstdout & stderr:\n" % cmd)

        _stdout, _stderr = [], []
        for o_line, e_line in zip_longest(stdout_fd, stderr_fd):

            if o_line:
                o_line = o_line.strip("\n")
                print_fun("{}".format(o_line))
                _stdout.append(o_line)
            if e_line:
                e_line = e_line.strip("\n")
                print_fun("{}".format(e_line))
                _stderr.append(e_line)

        ret = stdout_fd.channel.recv_exit_status()
        stdout = "\n".join(_stdout)
        stderr = "\n".join(_stderr)

        print_fun("\n\nret:\n%s\n" % ret)

        return {"ret": ret, "stdout": stdout, "stderr": stderr}

    def run_cmd(self, cmd, print_fun=None, timeout=600, stream_output=True):
        print_fun = print_fun if print_fun else self.print_fun
        if stream_output:
            return self._run_cmd_with_continuous_output(cmd, print_fun, timeout)
        else:
            return self._run_cmd(cmd, print_fun, timeout)

    def __assert_ret(self, ret, cmd, action):
        if ret != 0:
            if action:
                raise Exception("failed to %s on %s! ret=%d" % (action, self.host, ret))
            else:
                raise Exception(
                    "failed to run[%s] on %s! ret=%d" % (cmd, self.host, ret)
                )

    def call(self, cmd, print_fun=None, timeout=600, stream_output=True):
        print_fun = print_fun if print_fun else self.print_fun
        return self.run_cmd(cmd, print_fun, timeout, stream_output)["ret"]

    def check_call(
        self, cmd, print_fun=None, timeout=600, stream_output=True, action=None
    ):
        print_fun = print_fun if print_fun else self.print_fun
        result = self.run_cmd(cmd, print_fun, timeout, stream_output)
        self.__assert_ret(result["ret"], cmd, action)

    def check_output(
        self, cmd, print_fun=None, timeout=600, stream_output=True, action=None
    ):
        print_fun = print_fun if print_fun else self.print_fun
        result = self.run_cmd(cmd, print_fun, timeout, stream_output)
        self.__assert_ret(result["ret"], cmd, action)
        return result["stdout"]

    def close(self):
        self.client.close()


def ssh_check_output(
    host,
    cmd,
    print_fun=default_print_fun,
    timeout=600,
    action=None,
    name=None,
    password=None,
    encode="utf8",
    port=22,
):
    client = SSHClient(host, name, password, encode, port)
    try:
        return client.check_output(
            cmd, print_fun, timeout, action=action, stream_output=False
        )
    finally:
        client.close()
