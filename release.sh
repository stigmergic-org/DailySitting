#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only

set -euo pipefail

REMOTE="github"
GRADLE_FILE="app/build.gradle.kts"

die() {
  printf 'release.sh: %s\n' "$1" >&2
  exit 1
}

current_branch=$(git branch --show-current)
[ -n "$current_branch" ] || die "must be run from a branch, not detached HEAD"

[ -f "$GRADLE_FILE" ] || die "could not find $GRADLE_FILE"

if [ -n "$(git status --porcelain)" ]; then
  die "working tree is not clean; commit or stash changes first"
fi

git fetch --tags "$REMOTE"

latest_tag=$(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | sed -n '1p')
if [ -n "$latest_tag" ]; then
  base_version=${latest_tag#v}
  commit_range="$latest_tag..HEAD"
else
  base_version=$(sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$GRADLE_FILE" | sed -n '1p')
  [ -n "$base_version" ] || die "could not read versionName from $GRADLE_FILE"
  commit_range="HEAD"
fi

[[ "$base_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "base version is not semantic: $base_version"

subjects=$(git log --format='%s' "$commit_range")
bodies=$(git log --format='%B' "$commit_range")

if [ -z "$subjects" ]; then
  die "no commits found since ${latest_tag:-repository start}"
fi

if printf '%s\n' "$subjects" | grep -Eq '^[a-z]+(\([^)]+\))?!:' ||
  printf '%s\n' "$bodies" | grep -Eq '^BREAKING CHANGE:'; then
  bump="major"
elif printf '%s\n' "$subjects" | grep -Eq '^feat(\([^)]+\))?:'; then
  bump="minor"
elif printf '%s\n' "$subjects" | grep -Eq '^[a-z]+(\([^)]+\))?:'; then
  bump="patch"
else
  die "no semantic commit messages found since ${latest_tag:-repository start}"
fi

IFS=. read -r major minor patch <<<"$base_version"
case "$bump" in
major)
  major=$((major + 1))
  minor=0
  patch=0
  ;;
minor)
  minor=$((minor + 1))
  patch=0
  ;;
patch)
  patch=$((patch + 1))
  ;;
esac

next_version="$major.$minor.$patch"
next_tag="v$next_version"

if git rev-parse -q --verify "refs/tags/$next_tag" >/dev/null; then
  die "tag already exists: $next_tag"
fi

current_version_code=$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$GRADLE_FILE" | sed -n '1p')
[ -n "$current_version_code" ] || die "could not read versionCode from $GRADLE_FILE"
next_version_code=$((current_version_code + 1))

perl -0pi -e "s/versionCode\s*=\s*\d+/versionCode = $next_version_code/" "$GRADLE_FILE"
perl -0pi -e "s/versionName\s*=\s*\"[^\"]+\"/versionName = \"$next_version\"/" "$GRADLE_FILE"

git add "$GRADLE_FILE"
git commit -m "chore(release): $next_tag"
git tag -a "$next_tag" -m "$next_tag"

git push "$REMOTE" "$current_branch"
git push "$REMOTE" "$next_tag"

printf 'Released %s (%s bump, versionCode %s).\n' "$next_tag" "$bump" "$next_version_code"
