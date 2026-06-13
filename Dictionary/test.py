import json
from typing import Any


def write_json(name: str, value: Any):
    json_string = json.dumps(value, indent=4, ensure_ascii=False)
    with open(name, "w", encoding="utf-8") as file:
        file.write(json_string)


def read_json(name: str) -> Any:
    with open(name, "r", encoding="utf-8") as file:
        return json.load(file)


def main():
    raw_dictionary = read_json("raw_dictionary.json")
    counts = {}
    for word_info in raw_dictionary:
        split = word_info["article_with_word"].split()
        if not split:
            continue
        word = split[0]
        if word not in counts:
            counts[word] = 0
        counts[word] += 1
    counts = {k: counts[k] for k in sorted(counts.keys(), key=lambda x: counts[x], reverse=True)}
    write_json("test.json", counts)


if __name__ == '__main__':
    main()