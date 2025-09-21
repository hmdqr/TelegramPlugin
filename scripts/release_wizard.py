#!/usr/bin/env python3
"""
Release Wizard (Windows-friendly, English-only)
This interactive wizard guides you safely through:
- Optional version bump in pom.xml (Maven)
- git add/commit/push
- Create and push annotated tag v<version>
- Optional build and upload shaded JAR to GitHub Releases via gh

Requirements:
- Python 3.8+
- Git in PATH
- Maven in PATH (only if you want automatic version bump/build)
- GitHub CLI (gh) if you want to upload the JAR locally

Run from cmd:
    cd /d "c:\\Users\\hamad\\projects\\TelegramPlugin"
    python scripts\\release_wizard.py
"""

import subprocess
import sys
import os
import shlex
from pathlib import Path
import re

REPO_ROOT = Path(__file__).resolve().parent.parent


def run(cmd, cwd=REPO_ROOT, check=True, capture=False):
    """Run a shell command and return (code, out).
    Uses shell=False on Windows with list args.
    """
    if isinstance(cmd, str):
        args = shlex.split(cmd)
    else:
        args = cmd
    try:
        if capture:
            p = subprocess.run(args, cwd=str(cwd), check=check, text=True,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            return p.returncode, p.stdout
        else:
            p = subprocess.run(args, cwd=str(cwd), check=check)
            return p.returncode, None
    except subprocess.CalledProcessError as e:
        if capture and e.stdout:
            return e.returncode, e.stdout
        raise


def which(prog: str) -> bool:
    """Return True if program is found in PATH."""
    from shutil import which as _which
    return _which(prog) is not None


def _read_version_from_pom(pom_path: Path) -> str:
    try:
        text = pom_path.read_text(encoding="utf-8")
    except Exception:
        return ""
    # Try to read <version> directly under <project>
    m = re.search(r"<project[\s\S]*?<version>\s*([^<\s]+)\s*</version>", text, re.IGNORECASE)
    if m:
        return m.group(1).strip()
    # Fallback: any version tag
    m2 = re.search(r"<version>\s*([^<\s]+)\s*</version>", text, re.IGNORECASE)
    return m2.group(1).strip() if m2 else ""


def get_current_version() -> str:
    """Best-effort current version from Maven or pom.xml."""
    version = ""
    if which("mvn"):
        try:
            code, out = run(["mvn", "-q", "-DforceStdout", "help:evaluate", "-Dexpression=project.version"], capture=True)
            lines = (out or "").strip().splitlines()
            if lines:
                version = lines[-1].strip()
        except Exception:
            version = ""
    if not version:
        version = _read_version_from_pom(REPO_ROOT / "pom.xml")
    return version


def newest_shaded_jar():
    """Return the most recently modified *-shaded.jar in target/, or None."""
    target_dir = REPO_ROOT / "target"
    if not target_dir.exists():
        return None
    jars = []
    try:
        for name in os.listdir(target_dir):
            if name.endswith("-shaded.jar"):
                p = target_dir / name
                if p.is_file():
                    jars.append(p)
    except Exception:
        return None
    if not jars:
        return None
    jars.sort(key=lambda p: p.stat().st_mtime, reverse=True)
    return jars[0]


def prompt_file_path(prompt: str, allow_empty: bool = False):
    """Prompt user for a file path and return Path or None if empty allowed."""
    while True:
        raw = input(prompt).strip().strip('"')
        if not raw and allow_empty:
            return None
        if not raw:
            print("Please enter a path or press Enter to cancel.")
            continue
        p = Path(raw)
        if p.is_file():
            return p
        print("Path not found or not a file. Try again.")


def bump_version(new_version: str) -> None:
    print(f"[INFO] Setting version to {new_version} via Maven ...")
    run(["mvn", "-q", "versions:set", f"-DnewVersion={new_version}", "-DgenerateBackupPoms=false"])  # sets pom.xml
    # Optionally verify
    after = get_current_version()
    print(f"[OK] Version is now: {after}")


def confirm(prompt: str, default: bool = True) -> bool:
    default_str = "Y/n" if default else "y/N"
    while True:
        ans = input(f"{prompt} [{default_str}]: ").strip().lower()
        if not ans:
            return default
        if ans in ("y", "yes", "yep"):
            return True
        if ans in ("n", "no"):
            return False
        print("Please answer with y or n.")


def main():
    os.chdir(REPO_ROOT)
    print("== Release Wizard ==")

    if not which("git"):
        print("[ERROR] Git not found in PATH.")
        sys.exit(1)

    if not which("mvn"):
        print("[WARN] Maven not found in PATH. Version bump/build via Maven will be unavailable.")

    # 1) Read current version
    current_version = ""
    try:
        current_version = get_current_version()
    except Exception:
        current_version = ""
    if not current_version:
        print("[WARN] Unable to read project version automatically; you may be asked to enter it.")

    print(f"Current project version: {current_version or '(unknown)'}")

    # 2) Optional version bump
    if which("mvn") and confirm("Do you want to bump the version in pom.xml?", default=False):
        while True:
            new_version = input("Enter new version (e.g., 1.0.1): ").strip()
            if new_version:
                try:
                    bump_version(new_version)
                    current_version = new_version
                    break
                except subprocess.CalledProcessError as e:
                    print("[ERROR] Failed to set version:")
                    print(e)
            else:
                print("Please enter a valid value.")

        # git add/commit
        if confirm("Perform git add/commit for the change?", default=True):
            run(["git", "add", "pom.xml"])
            msg = f"Bump version to {current_version}"
            run(["git", "commit", "-m", msg], check=False)  # allow no-op
        else:
            print("[INFO] Skipping add/commit as requested.")

    # 3) push
    if confirm("Do you want to git push?", default=True):
        run(["git", "push"])
    else:
        print("[INFO] Skipping push.")

    # 4) Create and push tag
    if not current_version:
        # Ask for version to tag
        current_version = input("Enter version for the tag (e.g., 1.0.1): ").strip()
    tag = f"v{current_version}"
    print(f"Tag to create: {tag}")

    if confirm(f"Create tag {tag}?", default=True):
        # If tag exists, skip creation
        code, _ = run(["git", "rev-parse", "-q", "--verify", f"refs/tags/{tag}"], check=False)
        if code != 0:
            run(["git", "tag", "-a", tag, "-m", f"Release {tag}"])
            print("[OK] Tag created.")
        else:
            print("[INFO] Tag already exists; continuing.")
        if confirm("Push the tag to origin?", default=True):
            run(["git", "push", "origin", tag])
            print("[OK] Tag pushed.")
        else:
            print("[INFO] Skipping tag push.")
    else:
        print("[INFO] Tag creation cancelled.")

    # 5) Optionally upload shaded JAR to GitHub Releases via gh
    do_upload = confirm("Build and upload shaded JAR to GitHub Releases now?", default=False)
    if do_upload:
        # Locate GitHub CLI
        gh_cmd = "gh" if which("gh") else None
        if gh_cmd is None:
            p = prompt_file_path("GitHub CLI (gh) not found. Paste full path to gh executable, or press Enter to skip: ", allow_empty=True)
            if p is None:
                print("[INFO] Skipping local upload. CI can handle the tag.")
                return
            gh_cmd = str(p)

        # Build (offer if Maven present, else allow manual mvn path or skip)
        built = False
        if which("mvn"):
            print("[INFO] Building with Maven (skip tests)...")
            try:
                run(["mvn", "-q", "-DskipTests", "package"])
                built = True
            except FileNotFoundError:
                pass
            except subprocess.CalledProcessError:
                print("[WARN] Maven build failed.")
        if not built:
            ans = input("Paste full path to Maven executable to build (or press Enter to skip build): ").strip().strip('"')
            if ans:
                cand = Path(ans)
                if cand.is_file():
                    try:
                        print("[INFO] Building with Maven (skip tests)...")
                        run([str(cand), "-q", "-DskipTests", "package"])
                        built = True
                    except (FileNotFoundError, subprocess.CalledProcessError):
                        print("[WARN] Build failed or mvn not executable. Continuing without build.")
                else:
                    print("[WARN] Not a valid file. Continuing without build.")
            else:
                print("[INFO] Skipping build. Will try to use an existing shaded JAR.")

        # Find or prompt for shaded jar
        jar_path = newest_shaded_jar()
        if jar_path is None:
            jar_path = prompt_file_path("No *-shaded.jar found in target. Paste full path to a shaded JAR (or press Enter to cancel): ", allow_empty=True)
            if jar_path is None:
                print("[INFO] No JAR provided. Skipping upload. You can rely on CI.")
                return

        tag = f"v{current_version}"
        print(f"[INFO] Preparing upload to release {tag} ...")

        # Create or update release using gh_cmd
        try:
            code, _ = run([gh_cmd, "release", "view", tag], check=False)
        except FileNotFoundError:
            print("[ERROR] Could not execute GitHub CLI. Try running: gh auth login")
            print("[INFO] Skipping local upload. CI can handle the tag.")
            return

        if code != 0:
            print("[INFO] Creating new Release...")
            rc, out = run([gh_cmd, "release", "create", tag, str(jar_path), "--title", tag, "--notes", "Automated release"], check=False, capture=True)
            if rc != 0:
                print("[ERROR] Failed to create Release via gh:")
                print(out or "")
                if confirm("Skip local upload and rely on CI?", default=True):
                    print("[INFO] Skipping local upload. CI will handle the tag.")
                    return
                else:
                    print("Try manually later:")
                    print(f"  \"{gh_cmd}\" release create {tag} \"{jar_path}\" --title {tag} --notes \"Automated release\"")
                    return
        else:
            print("[INFO] Release exists. Uploading/replacing asset ...")
            rc, out = run([gh_cmd, "release", "upload", tag, str(jar_path), "--clobber"], check=False, capture=True)
            if rc != 0:
                print("[ERROR] Failed to upload via gh:")
                print(out or "")
                if confirm("Skip local upload and rely on CI?", default=True):
                    print("[INFO] Skipping local upload. CI will handle the tag.")
                    return
                else:
                    print("Try manually later:")
                    print(f"  \"{gh_cmd}\" release upload {tag} \"{jar_path}\" --clobber")
                    return

        print(f"[OK] Uploaded {jar_path.name} to release {tag}.")
    else:
        print("\nWizard finished. You can also rely on CI or gh scripts later to upload the JAR.")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n[INFO] Cancelled.")
        sys.exit(1)
