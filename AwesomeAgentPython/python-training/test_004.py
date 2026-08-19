words = ["Python", "is", "awesome", "for", "data", "science"]

word_lens = dict()

for word in words:
    word_lens[word] = len(word)

print(word_lens)