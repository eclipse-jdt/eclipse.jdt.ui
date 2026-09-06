# Copyright (c) 2026 Carsten Hammer.
# SPDX-License-Identifier: EPL-2.0
"""Temporary PR-3154 diagnostics. Observe owned test VMs; never terminate them."""
from __future__ import annotations

import argparse
from collections import deque
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def target(arguments: list[str], workspace: Path) -> str | None:
    if not arguments or Path(arguments[0]).name != "java":
        return None
    try:
        application = arguments[arguments.index("-application") + 1]
        data = Path(arguments[arguments.index("-data") + 1]).resolve()
        relative = data.relative_to(workspace.resolve())
    except (ValueError, IndexError, OSError):
        return None
    if (not application.startswith("org.eclipse.tycho.surefire.")
            or len(relative.parts) < 3 or relative.parts[1] != "target"):
        return None
    return relative.parts[0]


def processes(workspace: Path):
    for proc in Path("/proc").iterdir():
        if not proc.name.isdecimal():
            continue
        try:
            if proc.stat().st_uid != os.geteuid():
                continue
            arguments = [part.decode(errors="replace")
                         for part in (proc / "cmdline").read_bytes().split(b"\0") if part]
            module = target(arguments, workspace)
            if module is not None:
                fields = (proc / "stat").read_text().rsplit(") ", 1)[1].split()
                yield (int(proc.name), fields[19]), module
        except (OSError, IndexError):
            continue


def progress(log: Path) -> str:
    try:
        with log.open("rb") as stream:
            stream.seek(max(0, log.stat().st_size - 131072))
            lines = stream.read().decode(errors="replace").splitlines()
        selected = [line for line in lines if len(line) < 600 and (
            "Time elapsed:" in line or line.startswith("Running ")
            or line.startswith("[INFO] ---") or "Reactor Summary" in line
            or "SIGTERM" in line or "BUILD FAILURE" in line or "BUILD SUCCESS" in line)]
        return "\n".join(selected[-8:]) or "No test-progress lines available."
    except OSError as error:
        return f"Progress unavailable: {error.__class__.__name__}"


def snapshot(identity, module: str, workspace: Path, output: Path, jcmd: str) -> dict:
    pid, start = identity
    folder = output / "threads" / f"{module}-{pid}-{start}"
    folder.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    metadata = {"pid": pid, "start_tick": start, "module": module, "utc": stamp,
                "last_progress": progress(output / "maven.log")}
    # Do not save JVM arguments, system properties or environment variables.
    try:
        proc = Path("/proc") / str(pid)
        stat = (proc / "stat").read_text().rsplit(") ", 1)[1].split()
        if stat[19] != start or proc.stat().st_uid != os.geteuid():
            return {"error": "Process identity changed before capture"}
        current = [part.decode(errors="replace") for part in
                   (proc / "cmdline").read_bytes().split(b"\0") if part]
        if target(current, workspace) != module:
            return {"error": "Process is no longer an owned test VM"}
        metadata["cpu_ticks"] = {"user": int(stat[11]), "system": int(stat[12])}
        metadata["status"] = "\n".join(line for line in (proc / "status").read_text().splitlines()
                                      if line.startswith(("State:", "VmRSS:", "VmSwap:", "Threads:")))
    except (OSError, IndexError):
        return {"error": "Test VM exited before capture"}
    metadata["cgroup"] = {}
    for name in ("cpu.max", "cpu.stat", "memory.current", "memory.max", "memory.events"):
        try:
            metadata["cgroup"][name] = (Path("/sys/fs/cgroup") / name).read_text()[:2000]
        except OSError:
            pass
    dump = folder / f"{stamp}.txt"
    try:
        with dump.open("w", encoding="utf-8") as stream:
            result = subprocess.run([jcmd, str(pid), "Thread.print", "-l"], stdout=stream,
                                    stderr=subprocess.STDOUT, timeout=10, check=False)
        metadata["jcmd_exit"] = result.returncode
    except (OSError, subprocess.TimeoutExpired) as error:
        metadata["jcmd_error"] = error.__class__.__name__
    (folder / f"{stamp}.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    print(f"PR3154 diagnostic snapshot: {module}, pid={pid}, utc={stamp}", flush=True)
    return metadata


def watch(args) -> None:
    due, counts = {}, {}
    while not args.stop_file.exists():
        now = time.monotonic()
        for identity, module in processes(args.workspace):
            if args.stop_file.exists():
                break
            deadline = due.setdefault(identity, now if args.once else now + 90)
            if now >= deadline and counts.get(identity, 0) < 12:
                snapshot(identity, module, args.workspace, args.output, args.jcmd)
                counts[identity] = counts.get(identity, 0) + 1
                due[identity] = time.monotonic() + 120
        if args.once:
            return
        time.sleep(2)


def report_inventory(workspace: Path) -> str:
    lines = []
    for module in ("org.eclipse.jdt.ui.tests", "org.eclipse.jdt.text.tests", "org.eclipse.jdt.ui.tests.refactoring"):
        reports = sorted((workspace / module / "target/surefire-reports").glob("TEST-*.xml"))
        count, failed, skipped, invalid = 0, 0, 0, []
        for report in reports:
            recent = deque(maxlen=4)
            try:
                for _, case in ET.iterparse(report, events=("end",)):
                    if case.tag == "testcase":
                        count += 1
                        failed += case.find("failure") is not None or case.find("error") is not None
                        skipped += case.find("skipped") is not None
                        recent.append(f"{case.get('classname')}.{case.get('name')}")
                        case.clear()
            except (ET.ParseError, OSError) as error:
                invalid.append(f"{report.name}: {error}\nLast readable report entries (NOT necessarily execution order):\n" + "\n".join(recent))
        lines.append(f"{module}: reports={len(reports)}, readable cases={count}, failures/errors={failed}, skipped={skipped}, malformed={len(invalid)}")
        lines.extend(invalid)
    return "\n".join(lines)


def thread_excerpt(text: str) -> str:
    blocks = re.split(r"\n\s*\n", text)
    important = [block for block in blocks if block.startswith('"') and (
        block.startswith(('"main"', '"UI Thread"', '"JUnit'))
        or any(token in block for token in ("org.eclipse.jdt.junit.", "TestRunnerViewPart", "TestRunListenerTest", "ShutdownHook", "org.apache.maven.surefire")))]
    if not important:
        important = [block for block in blocks if block.startswith('"')][:4]
    if not important:
        return text[:1000]
    return "\n\n".join("\n".join(block.splitlines()[:55]) for block in important[:10])[:14000]


def summarize(workspace: Path, output: Path) -> str:
    output.mkdir(parents=True, exist_ok=True)
    exit_file = output / "maven-exit.txt"
    result = exit_file.read_text().strip() if exit_file.exists() else "not recorded (build may have been interrupted)"
    text = ("# PR-3154 runtime evidence\n\nTemporary diagnostics only; this check does not certify test success.\n\n"
            f"Maven exit: `{result}`\n\n## Last build/test progress\n```text\n{progress(output / 'maven.log')}\n```\n\n"
            f"## Report inventory (original reports are not changed)\n```text\n{report_inventory(workspace)}\n```\n")
    folders = sorted((output / "threads").glob("*"), key=lambda path: (
        not path.name.startswith("org.eclipse.jdt.ui.tests-"), path.name))
    if not folders:
        text += "\nNo VM snapshots recorded. This is missing evidence, not proof that no hang occurred.\n"
    for folder in folders[:3]:
        for path in sorted(folder.glob("*.json"))[-2:]:
            try:
                metadata = json.loads(path.read_text())
                dump = path.with_suffix(".txt").read_text(errors="replace")
            except (OSError, ValueError):
                continue
            text += (f"\n## {metadata['module']} / {metadata['utc']}\n```text\n"
                     + json.dumps(metadata, indent=2) + "\n\n" + thread_excerpt(dump) + "\n```\n")
    # GitHub checks accept at most 65535 bytes per text field. Preserve complete diagnostics as artifacts.
    encoded = text.encode("utf-8")
    if len(encoded) > 58000:
        text = encoded[:57000].decode("utf-8", errors="ignore") + "\n```\n\nExcerpt truncated; complete snapshots are archived.\n"
    (output / "summary.md").write_text(text, encoding="utf-8")
    return text


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("watch", "summarize"))
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--stop-file", type=Path)
    parser.add_argument("--jcmd")
    parser.add_argument("--once", action="store_true")
    args = parser.parse_args()
    if args.mode == "watch":
        if args.stop_file is None or args.jcmd is None:
            parser.error("watch requires --stop-file and --jcmd")
        watch(args)
    else:
        print(summarize(args.workspace, args.output))


if __name__ == "__main__":
    main()
