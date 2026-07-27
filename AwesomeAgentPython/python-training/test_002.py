users = [
    {"name": "Alice", "age": 25, "gender": "F"},
    {"name": "Bob", "age": 17, "gender": "M"},
    {"name": "Carol", "age": 30, "gender": "F"},
    {"name": "David", "age": 16, "gender": "M"},
    {"name": "Eve", "age": 22, "gender": "F"},
]

# 要求（用 list/dict 推导式，禁用普通 for）：
# 1) 提取所有 >=18 的成年用户列表
# 2) 把成年用户名字拼成 ["Ms.Alice", "Ms.Carol", "Ms.Eve"] / ["Mr.Bob"...](成年男性)
# 3) 按年龄分组（成年/未成年）成 dict：
#    {"adult": [...], "minor": [...]}

adults = []

for user in users:
    if user["age"] >= 18:
        adults.append(user)

print(adults)