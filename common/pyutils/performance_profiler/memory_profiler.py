#! --*-- coding: utf-8 --*--

import tracemalloc
import datetime


def print_memory(name, snapshot, memory):
    current, peak = tracemalloc.get_traced_memory()
    current_snapshot = tracemalloc.take_snapshot()
    top_stats = current_snapshot.compare_to(snapshot, "lineno")
    now = str(datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    print("/" * 200)
    usage = round((current - memory) / 10 ** 3)
    print(f"{name:<15} : memory increase {usage}KB")
    for stat in top_stats[:15]:
        print("[" + now + "]" + "top15memorydiff:" + name + ":  " + str(stat))
    print("=" * 200)
    stats = current_snapshot.statistics("lineno")
    for stat in stats[:15]:
        print("[" + now + "]" + "top15memory:" + name + ":  " + str(stat))
    return current_snapshot, current
