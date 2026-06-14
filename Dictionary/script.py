import json
import re
from typing import Any
from sqlalchemy.orm import Session
import model


# OLD
WORD_IGNORE = ["prpers"]
PART_IGNORE = ["Proper noun"]

PART_KEY = "part"
GENDER_KEY = "gender"
RAW_DICTIONARY_JSON = "raw_dictionary.json"
DICTIONARY_JSON = "dictionary.json"

NATIVE_KEY = "english_translation"
FOREIGN_KEY = "word"
POS_KEY = "pos"
ARTICLE_KEY = "article_with_word"


def write_json(name: str, value: Any):
    json_string = json.dumps(value, indent=4, ensure_ascii=False)
    with open(name, "w", encoding="utf-8") as file:
        file.write(json_string)


def read_json(name: str) -> Any:
    with open(name, "r", encoding="utf-8") as file:
        return json.load(file)


def create_dictionary_json():
    raw_dictionary = read_json("raw_dictionary.json")
    dictionary = {}
    for word_info in raw_dictionary:
        foreign = word_info[FOREIGN_KEY].lower().strip()
        raw_translation = word_info[NATIVE_KEY].lower()
        if any([foreign in t for t in re.findall("\\([^)]*\\)", raw_translation)]):
            # access (often in phrases like 'd'abord')   <- terrorist dataset
            continue
        native_set = {} if foreign not in dictionary else dictionary[foreign]
        for native_word in break_up_words(raw_translation):
            gender = 0
            article = word_info[ARTICLE_KEY]
            article_words = article.lower().split()
            if "un" in article_words or "le" in article_words:
                gender = 1
            elif "une" in article_words or "la" in article_words:
                gender = 2
            native_set[native_word] = {
                PART_KEY: word_info[POS_KEY],
                GENDER_KEY: gender,
            }
        dictionary[foreign] = native_set

    write_json(DICTIONARY_JSON, dictionary)


def break_up_words(chunk: str) -> list[str]:
    words = [""]
    word_index = 0
    in_parentheses = 0
    for c in chunk:
        if in_parentheses <= 0 and c == ";":
            words.append("")
            word_index += 1
            continue
        elif c == "(":
            in_parentheses += 1
        elif c == ")":
            in_parentheses -= 1

        words[word_index] += c

    return [w.strip() for w in words]


def create_db():
    db = model.init_db()
    model.init_tables(db)
    dictionary_json = read_json(DICTIONARY_JSON)

    foreigns = {}
    natives = {}
    meanings = {}
    default_attempts = "0" * 10
    for foreign_word, native_set in dictionary_json.items():
        foreign = model.Foreign(
            word=foreign_word,
            attempts=default_attempts,
        )
        foreigns[foreign_word] = foreign

        for native_word, details in native_set.items():
            native = model.Native(
                word=native_word,
                attempts=default_attempts,
            )
            natives[native_word] = native
            meaning = model.Meaning(
                foreign=foreign_word,
                native=native_word,
                part=details[PART_KEY],
                gender=details[GENDER_KEY],
                enabled=1,
            )
            meanings[(foreign_word, native_word)] = meaning

    with Session(db) as session:
        session.add_all(list(foreigns.values()))
        session.add_all(list(natives.values()))
        session.add_all(list(meanings.values()))
        session.commit()


if __name__ == '__main__':
    create_dictionary_json()
    create_db()
