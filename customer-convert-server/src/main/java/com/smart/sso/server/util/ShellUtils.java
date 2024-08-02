package com.smart.sso.server.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author padme
 * @version 1.0.0
 * @since 15/8/25
 */
@Slf4j
@UtilityClass
public class ShellUtils {

    /**
     * 超时返回-9 这是遵循python的规范
     */
    public static final int TIMEOUT_RETURN_CODE = -9;


    public static class TimeoutShellException extends Exception {
        private static final long serialVersionUID = 9108521611277852499L;

        public TimeoutShellException(String msg) {
            super(msg);
        }
    }

    /**
     * 启动一个进程执行shell 可以指定补充的环境变量
     * 函数返回一个process对象 调用方可以选择调用waitFor()阻塞等待 也可以选择不断sleep+调用isAlive()来异步执行
     * <p>
     * 同步调用的方式推荐后面的call, checkCall, output
     */
    public static Process bashRun(String cmd, Map<String, String> envs) throws IOException {
        // 1. 启动bash 其他命令作为参数
        // -l 表示模拟Login shell 就会读取.bash_profile
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-l", "-c", cmd);
        // 2. 补充自己的环境变量
        if (null != envs) {
            Map<String, String> processEnvs = processBuilder.environment();
            for (Map.Entry<String, String> entry : envs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                processEnvs.put(key, value);
            }
        }
        // 3. 启动进程
        return processBuilder.start();
    }

    /**
     * inputstream -> string
     */
    public static String dumpInputStream(InputStream inputStream) {
        return dumpInputStream(inputStream, new StringBuilder());
    }

    /**
     * inputstream -> string
     * non-blocking read 尽可能读一点然后返回
     */
    public static String dumpInputStream(InputStream inputStream, StringBuilder stringBuilder) {
        byte[] buffer = new byte[4096];
        int readBytes = 0;
        try {
            while (true) {
                // non blocking read
                readBytes = inputStream.available();
                if (readBytes == 0) {
                    break;
                }
                readBytes = readBytes > buffer.length ? buffer.length : readBytes;
                inputStream.read(buffer, 0, readBytes);
                stringBuilder.append(new String(buffer, 0, readBytes));
            }
        } catch (IOException e) {
            log.warn("exception when trying to read from inputstream, ", e);
        }
        return stringBuilder.toString();
    }

    /**
     * 同步执行命令的封装函数 执行命令并返回返回码，stdout, stderr输出
     */
    public static ExecuteReturn syncBashRun(String cmd, Map<String, String> envs) throws Exception {
        Process process = ShellUtils.bashRun(cmd, envs);
        process.waitFor();
        ExecuteReturn ret = new ExecuteReturn();
        ret.retcode = process.exitValue();
        ret.stdout = ShellUtils.dumpInputStream(process.getInputStream());
        ret.stderr = ShellUtils.dumpInputStream(process.getErrorStream());
        log.debug("executed cmd[{}] ret[{}] stdout[{}] stderr[{}]", cmd, ret.retcode, ret.stdout, ret.stderr);

        return ret;
    }


    /**
     * 带超时的执行某个命令
     */
    public static ExecuteReturn syncBashRunWithin(String cmd, Map<String, String> envs, int timeoutSeconds)
            throws Exception {
        ExecuteReturn ret = new ExecuteReturn();
        Process process = ShellUtils.bashRun(cmd, envs);
        StringBuilder stdoutStringBuilder = new StringBuilder();
        StringBuilder stderrStringBuilder = new StringBuilder();

        // 1. 执行条件：进程没有退出&&没有超时
        // 每0.1 s检查一次
        long start = System.currentTimeMillis();
        long executeTimeSeconds = 0;
        while (true) {
            executeTimeSeconds = (System.currentTimeMillis() - start) / 1000;
            ShellUtils.dumpInputStream(process.getInputStream(), stdoutStringBuilder);
            ShellUtils.dumpInputStream(process.getErrorStream(), stderrStringBuilder);
            // 2. 超时直接报异常
            if (executeTimeSeconds > timeoutSeconds) {
                String msg = String.format("failed to execute cmd %s because timeout %d", cmd, timeoutSeconds);
                log.warn(msg);
                log.warn("stdout: {}\nstderr: {}", stdoutStringBuilder, stderrStringBuilder);
                process.destroy();
                throw new TimeoutShellException(msg);
            }
            // 3. 进程退出 记录结果
            if (!process.isAlive()) {
                // 子进程结束，pipe 可能没有读完，再读一次，尽可能保证读完
                ShellUtils.dumpInputStream(process.getInputStream(), stdoutStringBuilder);
                ShellUtils.dumpInputStream(process.getErrorStream(), stderrStringBuilder);
                ret.retcode = process.exitValue();
                ret.stdout = stdoutStringBuilder.toString();
                ret.stderr = stderrStringBuilder.toString();
                log.debug("executed cmd[{}] ret[{}] execute_time[{}] timeout[{}] stdout[{}] stderr[{}]", cmd, ret.retcode,
                        executeTimeSeconds, timeoutSeconds, ret.stdout, ret.stderr);
                return ret;
            }
            Thread.sleep(100);
        }
    }

    /**
     * 执行带超时 但是超时其实返回的是-9 依然尽可能的读取stderr/stdout 而不是抛异常
     */
    public static ExecuteReturn syncBashRunWithinNoThrow(String cmd, Map<String, String> envs,
                                                         int timeoutSeconds) throws IOException, InterruptedException {
        ExecuteReturn ret = new ExecuteReturn();
        Process process = ShellUtils.bashRun(cmd, envs);
        StringBuilder stdoutStringBuilder = new StringBuilder();
        StringBuilder stderrStringBuilder = new StringBuilder();

        // 1. 执行条件：进程没有退出&&没有超时
        // 每0.1 s检查一次
        long start = System.currentTimeMillis();
        long executeTimeSeconds = 0;
        while (true) {
            executeTimeSeconds = (System.currentTimeMillis() - start) / 1000;
            ShellUtils.dumpInputStream(process.getInputStream(), stdoutStringBuilder);
            ShellUtils.dumpInputStream(process.getErrorStream(), stderrStringBuilder);
            // 2. 超时直接报异常
            if (executeTimeSeconds > timeoutSeconds) {
                ret.retcode = -9;
                ret.stdout = stdoutStringBuilder.toString();
                ret.stderr = stderrStringBuilder.toString();
                log.warn("executed timeout! cmd[{}] ret[{}] execute_time[{}] timeout[{}] stdout[{}] stderr[{}]", cmd,
                        ret.retcode,
                        executeTimeSeconds, timeoutSeconds, ret.stdout, ret.stderr);
                process.destroy();
                return ret;
            }
            // 3. 进程退出 记录结果
            if (!process.isAlive()) {
                // 子进程结束，pipe 可能没有读完，再读一次，尽可能保证读完
                ShellUtils.dumpInputStream(process.getInputStream(), stdoutStringBuilder);
                ShellUtils.dumpInputStream(process.getErrorStream(), stderrStringBuilder);
                ret.retcode = process.exitValue();
                ret.stdout = stdoutStringBuilder.toString();
                ret.stderr = stderrStringBuilder.toString();
                log.debug("executed cmd[{}] ret[{}] execute_time[{}] timeout[{}] stdout[{}] stderr[{}]", cmd, ret.retcode,
                        executeTimeSeconds, timeoutSeconds, ret.stdout, ret.stderr);
                return ret;
            }
            Thread.sleep(100);
        }
    }

    /**
     * 调用一个shell 命令 返回返回码
     */
    public static int call(String cmd, Map<String, String> envs) throws Exception {
        ExecuteReturn ret = syncBashRun(cmd, envs);
        return ret.retcode;
    }

    /**
     * 同上 不传递环境变量的
     */
    public static int call(String cmd) throws Exception {
        return call(cmd, new HashMap<>());
    }

    /**
     * 调用一个shell命令 返回返回码 带超时
     */
    public static int callWithin(String cmd, Map<String, String> envs, int timeout) throws Exception {
        ExecuteReturn ret = syncBashRunWithin(cmd, envs, timeout);
        return ret.retcode;
    }

    /**
     * 同上 不传递环境变量的
     */
    public static int callWithin(String cmd, int timeout) throws Exception {
        return callWithin(cmd, new HashMap<>(), timeout);
    }

    /**
     * 调用命令并检查返回值
     */
    public static void checkCalledWithin(String cmd, int timeout) throws Exception {
        if (callWithin(cmd, timeout) != 0) {
            throw new IOException(String.format("failed to call %s", cmd));
        }
    }

    /**
     * 启动一个进程 执行bash命令 返回stdout输出
     * 如果执行失败了跑出异常
     */
    public static String output(String cmd, Map<String, String> envs) throws Exception {
        ExecuteReturn ret = syncBashRun(cmd, envs);
        if (ret.retcode != 0) {
            throw new IOException(String.format("failed to execute cmd[%s]", cmd));
        }
        return ret.stdout;
    }

    /**
     * 同上 不传递环境变量的
     */
    public static String output(String cmd) throws Exception {
        return ShellUtils.output(cmd, new HashMap<>());
    }

    /**
     * 带超时的ouput
     */
    public static String outputWithin(String cmd, Map<String, String> envs, int timeout) throws Exception {
        ExecuteReturn ret = syncBashRunWithin(cmd, envs, timeout);
        if (ret.retcode != 0) {
            throw new Exception(String.format("failed to execute cmd[%s]", cmd));
        }
        return ret.stdout;
    }

    /**
     * 同上 不传递环境变量的
     */
    public static String outputWithin(String cmd, int timeout) throws Exception {
        return outputWithin(cmd, new HashMap<String, String>(), timeout);
    }

    /**
     * 在SP环境中,启动一个Python进程, 默认使用python3执行
     */
    public static Process saPythonRun(String pythonFilePath, int maxArgc, String... params) throws IOException {
        String pythonPath =
                String.format("python", "");
        String[] command = null;
        if (maxArgc > 0) {
            int realArgc = Math.min(params.length, maxArgc);
            command = new String[2 + realArgc];
            System.arraycopy(params, 0, command, 2, realArgc);
        } else {
            command = new String[2];
        }
        command[0] = pythonPath;
        command[1] = pythonFilePath;
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // 3. 启动进程
        return processBuilder.start();
    }

    /**
     * 对返回值的封装 要是Python的化直接写个dict 分分钟。。
     */
    public static class ExecuteReturn {
        private int retcode;
        private String stdout;
        private String stderr;

        public int getRetcode() {
            return retcode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }
    }

}
