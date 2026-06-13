#
# def create_dictionary_json():
#     # with open("mock.xml", "r", encoding="utf-8") as dict_file:
#     with open("old/dictionary.xml", "r", encoding="utf-8") as dict_file:
#         xml_data = dict_file.read()
#     root = ET.fromstring(xml_data)
#     sdefs = root[1]
#     section = root[2]
#
#     parts_of_speech = {}
#     for child in sdefs:
#         if "c" in child.attrib and child.attrib["n"] not in parts_of_speech:
#             parts_of_speech[child.attrib["n"]] = child.attrib["c"]
#
#     dictionary = {}
#     for child in section:
#         word = child[0][0].text
#         translation = child[0][1].text
#         assert word is not None
#         assert translation is not None
#         part_of_speech = None
#         skip = False
#
#         for c in child[0][0]:
#             if c.tag == "b":
#                 word += f" {c.tail}"
#             elif c.tag == "s":
#                 part = parts_of_speech[c.attrib["n"]]
#                 if part == "":
#                     continue
#                 if part in PART_IGNORE:
#                     skip = True
#                 if not part_of_speech:
#                     part_of_speech = part
#                 else:
#                     part_of_speech += f" | {part}"
#             elif c.tag == "g":
#                 for c2 in c:
#                     assert c2.tail is not None
#                     word += f" {c2.tail}"
#             else:
#                 raise AssertionError("what?")
#
#         for c in child[0][1]:
#             if c.tag == "b":
#                 translation += f" {c.tail}"
#             elif c.tag == "g":
#                 for c2 in c:
#                     assert c2.tail is not None
#                     translation += f" {c2.tail}"
#             elif c.tag != "s":
#                 raise AssertionError("huh?")
#
#         assert part_of_speech is not None
#         if translation in WORD_IGNORE or skip:
#             continue
#
#         if word not in dictionary:
#             dictionary[word] = {}
#         dictionary[word][translation] = part_of_speech
#
#     write_json("dictionary.json", dictionary)