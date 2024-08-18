# coding: utf-8
import os
import re
from pathlib import Path

from .shell_wrapper import run_cmd


# 实现 else / elif


class CodeBuilder(object):
    """
    Code Builder
    方便管理缩进
    """
    INDENT_STEP = 4

    def __init__(self, indent=0):
        self.code = []
        self.indent_level = indent

    def __str__(self):
        return "".join(str(c) for c in self.code)

    def indent(self):
        self.indent_level += self.INDENT_STEP

    def dedent(self):
        self.indent_level -= self.INDENT_STEP

    def add_line(self, line):
        """
        自动添加缩进等级与换行
        :param line:
        :return:
        """
        self.code.extend([" " * self.indent_level, line, "\n"])

    def add_section(self):
        """
        添加代码块, 享有共同的基础缩进等级
        :return:
        """
        section = CodeBuilder(self.indent_level)
        self.code.append(section)
        return section

    def get_global_dict(self):
        """
        执行获取变量字典
        :return:
        """
        # assert self.indent_level == 0
        python_source = str(self)
        global_namespace = {}
        exec(python_source, global_namespace)
        return global_namespace


class SBTemplate(object):
    """
    Sensors Brain Template
    support grammars:
      var
        {{a.b.c|func_a|func_b}}
      loop
        {% for var in list %}...{% endfor %}
        {% for var1 var2 in list %}...{% endfor %}
      if
        {% if var %}...{% endif %}
      comment
        {# xxxx #}
    usage:
        t = SBTemplate('xxx')
        text = t.render({'a': 'xxx',
                         'b': 'xxx'})
    """

    def __init__(self, text, *contexts):
        self.context = {}
        for context in contexts:
            self.context.update(context)

        # 全局变量 + 函数变量, 加上 'c_' 前缀避免与局部变量重复
        self.all_vars = set()
        # 局部变量
        self.loop_vars = set()

        self._render_function = self.parse_text_to_code(text)

    @staticmethod
    def _variable(name, vars_set):
        """
        对 var 命名的检查与注册
        :param name:
        :param vars_set:
        :return:
        """
        if not re.match(r"[_a-zA-Z][_a-zA-Z0-9]*$", name):
            raise ValueError("Not a valid name: %r" % name)
        vars_set.add(name)

    @staticmethod
    def _do_dots(value, *dots):
        """
        dots 获取attr, 如为函数则执行(适应类成员函数调用)
        :param value:
        :param dots:
        :return:
        """
        for dot in dots:
            try:
                value = getattr(value, dot)
            except AttributeError:
                value = value[dot]
            if callable(value):
                value = value()
        return value

    def _expr_code(self, expr):
        """
        执行 expr, 满足 var 下对 dots 与 func 的需求
        同时注册会用到的 var 与 func 到 self.all_vars
        :param expr:
        :return:
        """
        if "|" in expr:
            pipes = expr.split("|")
            code = self._expr_code(pipes[0])
            for func in pipes[1:]:
                self._variable(func, self.all_vars)
                code = "c_%s(%s)" % (func, code)
        elif "." in expr:
            dots = expr.split(".")
            code = self._expr_code(dots[0])
            # 生成do_dots的list参数
            args = ", ".join(repr(d) for d in dots[1:])
            code = "do_dots(%s, %s)" % (code, args)
        else:
            # 单纯的var, 直接输出即可
            self._variable(expr, self.all_vars)
            code = "c_%s" % expr
        return code

    def parse_text_to_code(self, text):
        code = CodeBuilder()
        ops_stack = []
        buffered = []

        def flush_output():
            """
            缓冲区域, 显式调用输出
            :return:
            """
            if len(buffered) == 1:
                code.add_line("append_result(%s)" % buffered[0])
            elif len(buffered) > 1:
                code.add_line("extend_result([%s])" % ", ".join(buffered))
            del buffered[:]

        # 最终目的是借由text生成该函数 render_function, 执行 render 得到最终 模版替换结果
        code.add_line("def render_function(context, do_dots):")
        code.indent()
        vars_code = code.add_section()
        code.add_line("result = []")
        code.add_line("append_result = result.append")
        code.add_line("extend_result = result.extend")
        code.add_line("to_str = str")

        # 切分代码段
        tokens = re.split(r"(?s)({{.*?}}|{%.*?%}|{#.*?#})", text)
        for token in tokens:
            # comment
            if token.startswith('{#'):
                continue
            # var (dot & func)
            elif token.startswith('{{'):
                expr = self._expr_code(token[2:-2].strip())
                buffered.append("to_str(%s)" % expr)

            elif token.startswith('{%'):
                flush_output()
                words = token[2:-2].strip().split()
                # if statement
                if words[0] == 'if':
                    if len(words) != 2:
                        raise ValueError("Invalid IF condition: %r" % token)
                    ops_stack.append('if')
                    code.add_line("if %s:" % self._expr_code(words[1]))
                    code.indent()
                # todo: 实现else
                # elif words[0] == 'else':
                #     pass
                # for statement
                elif words[0] == 'for':
                    if len(words) < 4 or words[-2] != 'in':
                        raise ValueError("Invalid FOR condition: %r" % token)
                    ops_stack.append('for')
                    for tag in words[1:-2]:
                        self._variable(tag, self.loop_vars)
                    code.add_line(
                        "for %s in %s:" % (
                            ','.join(['c_%s' % tag for tag in words[1:-2]]),
                            self._expr_code(words[-1])
                        )
                    )

                    code.indent()
                # end statement
                elif words[0].startswith('end'):
                    # Endsomething.  Pop the ops stack.
                    if len(words) != 1:
                        raise ValueError("Invalid END condition: %r" % token)
                    end_what = words[0][3:]
                    if not ops_stack:
                        raise ValueError("Too many ends: %r" % token)
                    start_what = ops_stack.pop()
                    if start_what != end_what:
                        raise ValueError("Mismatched end tag: %r" % token)
                    code.dedent()
                else:
                    raise ValueError("Invalid tag: %r" % words[0])
            else:
                # 无需处理的正常代码片段
                if token:
                    buffered.append(repr(token))

        if ops_stack:
            raise ValueError("Unmatched action tag: %r" % ops_stack[-1])

        flush_output()

        # 添加变量与对应的值
        for var_name in self.all_vars - self.loop_vars:
            vars_code.add_line("c_%s = context[%r]" % (var_name, var_name))

        code.add_line("return ''.join(result)")
        code.dedent()

        return code.get_global_dict()['render_function']

    def render(self, context=None):
        """
        模版替换
        :param context: extra变量字典
        :return:
        """
        # Make the complete context we'll use.
        render_context = dict(self.context)
        if context:
            render_context.update(context)
        return self._render_function(render_context, self._do_dots)


def generate_template_file_from_text(text, output_file_path, *contexts):
    """
    由字符串生成文件
    :param text:
    :param output_file_path:
    :param contexts:
    :return:
    """
    content = SBTemplate(text, *contexts)
    with open(output_file_path, 'w') as f_out:
        f_out.write(content)


def generate_template_file_from_file(input_file_path, output_file_path, context):
    """
    由文件模版生成文件
    :param input_file_path:
    :param output_file_path:
    :param contexts:
    :return:
    """
    with open(input_file_path) as f_in:
        text = ''.join(f_in.readlines())
    content = SBTemplate(text)
    dir_path = Path(os.path.split(output_file_path)[0])
    if not dir_path.exists():
        run_cmd('mkdir -p {}'.format(os.path.split(output_file_path)[0]))

    with open(output_file_path, 'w') as f_out:
        f_out.write(content.render(context))

# todo: 创建地址
