import json
import re
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
    findings = []
    for word_info in raw_dictionary:
        word = word_info["word"]
        translation = word_info["english_translation"]
        if any([word.lower() in t.lower() for t in re.findall("\\([^)]*\\)", translation)]):
            findings.append(word_info)
    write_json("findings.json", findings)


if __name__ == '__main__':
    main()