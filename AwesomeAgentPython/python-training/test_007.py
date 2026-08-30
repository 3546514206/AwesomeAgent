a = {1, 2, 3, 4, 5}
b = {3, 4, 5, 6, 7}

# 任务 A：a 和 b 的并集（所有不重复的元素）
# 任务 B：a 和 b 的交集（都有的元素）
# 任务 C：a 有 b 没有（差集）
# 任务 D：把 [1, 2, 2, 3, 3, 3] 去重变成 {1, 2, 3}
# 任务 E：判断3 在不在 a 里
# 任务 F：往 a 里加 100，再删2（如果 2 不存在也不能报错）

c = set(a | b)
print(c)

d = set(a & b)
print(d)

e = set(a - b)
print(e)

lst = list([1, 2, 2, 3, 3, 3])
f = set(lst)
print(f)

print(3 in a)

a.add(100)
if 2 in a:
    a.remove(2)

a.discard(2)
print(a)
