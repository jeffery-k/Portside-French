import json
from typing import Any


def write_json(name: str, value: Any):
    json_string = json.dumps(value, indent=4, ensure_ascii=False)
    with open(name, "w", encoding="utf-8") as file:
        file.write(json_string)


def read_json(name: str) -> Any:
    with open(name, "r", encoding="utf-8") as file:
        return json.load(file)


class Group:
    def __init__(self):
        self.foreigns = set()
        self.natives = set()

    def get_size(self) -> int:
        return len(self.foreigns) + len(self.natives)

    def to_dict(self) -> dict[str, Any]:
        return {
            "foreigns": list(self.foreigns),
            "natives": list(self.natives),
            "size": self.get_size(),
        }


class GroupSet:
    def __init__(self, foreigns_to_natives: dict[str, set[str]], natives_to_foreigns: dict[str, set[str]]):
        self.foreigns_to_natives: dict[str, set[str]] = foreigns_to_natives
        self.natives_to_foreigns: dict[str, set[str]] = natives_to_foreigns
        self.processed_foreigns: dict[str, bool] = { f: False for f in foreigns_to_natives.keys() }
        self.processed_natives: dict[str, bool] = { n: False for n in natives_to_foreigns.keys() }
        self.groups: list[Group] = []

        for foreign in foreigns_to_natives.keys():
            self.insert_foreign(foreign)

    def insert_foreign(self, foreign: str):
        self.processed_foreigns[foreign] = True
        natives = self.foreigns_to_natives[foreign]
        group = None
        for g in self.groups:
            if foreign in g.foreigns:
                group = g
                break

        if group is None:
            group = Group()
            group.foreigns.add(foreign)
            self.groups.append(group)
        group.natives.update(natives)
        for native in natives:
            if not self.processed_natives[native]:
                self.insert_native(native)


    def insert_native(self, native: str):
        self.processed_natives[native] = True
        foreigns = self.natives_to_foreigns[native]
        group = None
        for g in self.groups:
            if native in g.natives:
                group = g
                break

        if group is None:
            group = Group()
            group.natives.add(native)
            self.groups.append(group)
        group.foreigns.update(foreigns)
        for foreign in foreigns:
            if not self.processed_foreigns[foreign]:
                self.insert_foreign(foreign)


    def to_list(self) -> list[dict[str, Any]]:
        return [
            group.to_dict() for group
            in sorted(self.groups, key=lambda x: x.get_size(), reverse=True)
        ]


def find_loops(dictionary: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    foreign_to_natives = {}
    native_to_foreigns = {}
    for foreign, natives in dictionary.items():
        if foreign not in foreign_to_natives:
            foreign_to_natives[foreign] = set()
        for native in natives.keys():
            if native not in native_to_foreigns:
                native_to_foreigns[native] = set()
            foreign_to_natives[foreign].add(native)
            native_to_foreigns[native].add(foreign)

    group_set = GroupSet(foreign_to_natives, native_to_foreigns)
    return group_set.to_list()


def main():
    dictionary = read_json("dictionary.json")
    findings = find_loops(dictionary)
    write_json("findings.json", findings)


if __name__ == '__main__':
    main()