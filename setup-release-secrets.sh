#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only

set -euo pipefail

KEYSTORE_FILE="app/signing/release.jks"
KEY_ALIAS="daily-sitting"
KEYSTORE_TYPE="JKS"
VALIDITY_DAYS="10000"
DNAME="CN=Daily Sitting, OU=Release, O=Daily Sitting, L=Unknown, ST=Unknown, C=US"
FORCE=0

usage() {
  cat <<'EOF'
Usage: ./setup-release-secrets.sh [options]

Generates the Android release signing secrets used by GitHub Actions, uploads
them as GitHub repository secrets with gh, and copies a KeePass-friendly backup
block to the clipboard.

Options:
  --force                 Overwrite an existing local keystore file.
  --alias NAME            Keystore key alias. Default: daily-sitting
  --keystore PATH         Local keystore path. Default: app/signing/release.jks
  -h, --help              Show this help.

Requires: keytool, gh, openssl, base64, and a clipboard command (pbcopy on macOS,
wl-copy, xclip, or xsel).
EOF
}

die() {
  printf 'setup-release-secrets.sh: %s\n' "$1" >&2
  exit 1
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --force)
      FORCE=1
      shift
      ;;
    --alias)
      [ "$#" -ge 2 ] || die "--alias requires a value"
      KEY_ALIAS="$2"
      shift 2
      ;;
    --keystore)
      [ "$#" -ge 2 ] || die "--keystore requires a value"
      KEYSTORE_FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "unknown option: $1"
      ;;
  esac
done

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "missing required command: $1"
}

need_cmd keytool
need_cmd gh
need_cmd openssl
need_cmd base64

copy_to_clipboard() {
  if command -v pbcopy >/dev/null 2>&1; then
    pbcopy
  elif command -v wl-copy >/dev/null 2>&1; then
    wl-copy
  elif command -v xclip >/dev/null 2>&1; then
    xclip -selection clipboard
  elif command -v xsel >/dev/null 2>&1; then
    xsel --clipboard --input
  else
    die "no clipboard command found; install pbcopy, wl-copy, xclip, or xsel"
  fi
}

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    die "missing checksum command: shasum or sha256sum"
  fi
}

gh auth status >/dev/null 2>&1 || die "gh is not authenticated; run: gh auth login"
gh repo view >/dev/null 2>&1 || die "gh cannot determine the repository; run this from the GitHub repo checkout"

if [ -e "$KEYSTORE_FILE" ] && [ "$FORCE" -ne 1 ]; then
  die "$KEYSTORE_FILE already exists; pass --force to overwrite it"
fi

mkdir -p "$(dirname "$KEYSTORE_FILE")"

keystore_password=$(openssl rand -base64 48 | tr -d '\n')
key_password=$(openssl rand -base64 48 | tr -d '\n')

rm -f "$KEYSTORE_FILE"
keytool -genkeypair -v \
  -keystore "$KEYSTORE_FILE" \
  -storetype "$KEYSTORE_TYPE" \
  -storepass "$keystore_password" \
  -keypass "$key_password" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity "$VALIDITY_DAYS" \
  -dname "$DNAME" >/dev/null

keystore_base64=$(base64 < "$KEYSTORE_FILE" | tr -d '\n')
keystore_sha256=$(sha256_file "$KEYSTORE_FILE")

gh secret set RELEASE_KEYSTORE_BASE64 --body "$keystore_base64" >/dev/null
gh secret set RELEASE_KEYSTORE_PASSWORD --body "$keystore_password" >/dev/null
gh secret set RELEASE_KEY_ALIAS --body "$KEY_ALIAS" >/dev/null
gh secret set RELEASE_KEY_PASSWORD --body "$key_password" >/dev/null

keepass_note=$(cat <<EOF
Daily Sitting Android release signing

GitHub repository secrets:
RELEASE_KEYSTORE_BASE64=$keystore_base64
RELEASE_KEYSTORE_PASSWORD=$keystore_password
RELEASE_KEY_ALIAS=$KEY_ALIAS
RELEASE_KEY_PASSWORD=$key_password

Local keystore path:
$KEYSTORE_FILE

Keystore SHA-256:
$keystore_sha256

Restore keystore from RELEASE_KEYSTORE_BASE64:
printf '%s' '<RELEASE_KEYSTORE_BASE64>' | base64 --decode > release.jks
EOF
)

printf '%s' "$keepass_note" | copy_to_clipboard

printf 'Generated %s and uploaded GitHub release signing secrets.\n' "$KEYSTORE_FILE"
printf 'Copied KeePass backup note to clipboard. Save it now.\n'
