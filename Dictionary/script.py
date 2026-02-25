import json
import xml.etree.ElementTree as ET
from typing import Any
from sqlalchemy.orm import Session
import model

WORD_IGNORE = ["prpers"]
PART_IGNORE = ["Proper noun"]


def write_json(name: str, value: Any):
    seen_string = json.dumps(value, indent=4, ensure_ascii=False)
    with open(name, "w", encoding="utf-8") as file:
        file.write(seen_string)


def read_json(name: str) -> Any:
    with open(name, "r", encoding="utf-8") as file:
        return json.load(file)


def create_dictionary_json():
    with open("dictionary.xml", "r", encoding="utf-8") as dict_file:
        xml_data = dict_file.read()
    root = ET.fromstring(xml_data)
    sdefs = root[1]
    section = root[2]

    parts_of_speech = {}
    for child in sdefs:
        if "c" in child.attrib and child.attrib["n"] not in parts_of_speech:
            parts_of_speech[child.attrib["n"]] = child.attrib["c"]

    dictionary = {}
    for child in section:
        word = child[0][0].text
        translation = child[0][1].text
        assert word is not None
        assert translation is not None
        part_of_speech = None
        skip = False

        for c in child[0][0]:
            if c.tag == "b":
                word += f" {c.tail}"
            elif c.tag == "s":
                part = parts_of_speech[c.attrib["n"]]
                if part == "":
                    continue
                if part in PART_IGNORE:
                    skip = True
                if not part_of_speech:
                    part_of_speech = part
                else:
                    part_of_speech += f" | {part}"
            elif c.tag == "g":
                for c2 in c:
                    assert c2.tail is not None
                    word += f" {c2.tail}"
            else:
                raise AssertionError("what?")

        for c in child[0][1]:
            if c.tag == "b":
                translation += f" {c.tail}"
            elif c.tag == "g":
                for c2 in c:
                    assert c2.tail is not None
                    word += f" {c2.tail}"
            elif c.tag != "s":
                raise AssertionError("huh?")

        assert part_of_speech is not None
        if translation in WORD_IGNORE or skip:
            continue

        if word not in dictionary:
            dictionary[word] = {}
        dictionary[word][translation] = part_of_speech

    write_json("dictionary.json", dictionary)


def create_db():
    db = model.init_db()
    model.init_tables(db)
    dictionary_json = read_json("dictionary.json")

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

        for native_word, part in native_set.items():
            native = model.Native(
                word=native_word,
                attempts=default_attempts,
            )
            natives[native_word] = native
            meaning = model.Meaning(
                foreign=foreign_word,
                native=native_word,
                part=part,
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
