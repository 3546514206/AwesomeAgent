nums = list(range(1, 20 + 1))

result = []

for n in nums:
    if n % 3 == 0:
        result.append(n * n)

result_v2 = [n * n for n in nums if n % 3 == 0]

print(result)
print(result_v2)
