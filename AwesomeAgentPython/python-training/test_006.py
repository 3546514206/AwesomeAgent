user = dict({"name": "alice", "age": 28, "city": "长沙"})
# 任务 A：加一个键值对 "job": "工程师"
# 任务 B：把 age 改成 29
# 任务 C：安全读 "email"，没这个 key 就返回 "未知"
# 任务 D：删掉 "city" 这个键
# 任务 E：把所有 key 拿出来成 list
# 任务 F：把所有 (k, v) 拿出来成 list of tuple

user["job"] = "工程师"
print(user)

user["age"] = 29
print(user)
print(user.get("email", "未知"))

del user["city"]

lst = list(user.keys())
print(lst)

lst2 = list()

for key, value in user.items():
   tpl = tuple([key, value])
   lst2.append(tpl)

print(lst2)