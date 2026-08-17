#!/usr/bin/env python3
"""Select a SiliconFlow JAR by its embedded exact Minecraft target."""
import json
import sys
import zipfile
from pathlib import Path

def main() -> int:
    if len(sys.argv) != 3:
        print(f"usage: {sys.argv[0]} <minecraft-version> <artifact-directory>", file=sys.stderr)
        return 2
    wanted, directory = sys.argv[1], Path(sys.argv[2])
    matches = []
    for jar in sorted(directory.glob("*.jar")):
        try:
            with zipfile.ZipFile(jar) as archive:
                target = json.loads(archive.read("m3frametime.build-target.json"))
            if target.get("minecraft") == wanted:
                matches.append(jar)
        except (KeyError, OSError, ValueError, json.JSONDecodeError):
            continue
    if len(matches) != 1:
        print(f"expected exactly one artifact for Minecraft {wanted}, found {len(matches)}", file=sys.stderr)
        return 1
    print(matches[0])
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
