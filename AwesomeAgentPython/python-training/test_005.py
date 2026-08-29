fruits = ["apple", "banana", "cherry"]

# 任务 A：末尾加 "date"
# 任务 B：在索引 0 位置插入 "apricot"
# 任务 C：删除 "banana"
# 任务 D：把列表倒序
# 任务 E：判断 "apple" 在不在列表里
# 任务 F：列表里每个单词的长度 → [5, 6, 6]

fruits.append("date")
print(fruits)
fruits.insert(0, "apricot")
print(fruits)
fruits.remove("banana")
print(fruits)
fruits.reverse()
print(fruits)

print("apple" in fruits)

length = []
for fruit in fruits:
    length.append(len(fruit))

print(length)

