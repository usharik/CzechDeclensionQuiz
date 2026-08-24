#!/usr/bin/env bash
# setup-github-secrets.sh
#
# Creates GitHub Actions secrets required by .github/workflows/deploy.yml.
#
# Shared across apps:
#   - KEYSTORE_BASE64
#   - KEYSTORE_PASSWORD
#
# App-specific for this repository (CZECH_DECLENSION_QUIZ):
#   - KEY_ALIAS_<APP_SUFFIX>
#   - KEY_PASSWORD_<APP_SUFFIX>
#   - GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_<APP_SUFFIX>
#   - GOOGLE_SERVICES_JSON_<APP_SUFFIX>
#
# Sources:
#   - local.properties        → signing.storeFile / signing.storePassword / signing.keyAlias / signing.keyPassword
#   - play-key.json           → Google Play service account for this app (override with PLAY_SERVICE_ACCOUNT_FILE)
#   - app/google-services.json → Firebase config for this app (override with GOOGLE_SERVICES_FILE)

set -euo pipefail

REPO="${REPO:-usharik/CzechDeclensionQuiz}"
APP_SECRET_SUFFIX="CZECH_DECLENSION_QUIZ"
LOCAL_PROPS="${LOCAL_PROPS:-local.properties}"
PLAY_SERVICE_ACCOUNT_FILE="${PLAY_SERVICE_ACCOUNT_FILE:-play-key.json}"
GOOGLE_SERVICES_FILE="${GOOGLE_SERVICES_FILE:-app/google-services.json}"

die()  { echo "❌  $*" >&2; exit 1; }
info() { echo "▶  $*"; }
ok()   { echo "✅  $*"; }

prop_get() {
    local key="$1"
    grep -E "^${key}[[:space:]]*=" "$LOCAL_PROPS" \
        | head -1 \
        | cut -d= -f2- \
        | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

set_secret() {
    local name="$1"
    local value="$2"
    printf '%s' "$value" | gh secret set "$name" --repo "$REPO" --body -
    ok "Secret set: $name"
}

set_secret_file() {
    local name="$1"
    local file="$2"
    gh secret set "$name" --repo "$REPO" < "$file"
    ok "Secret set: $name"
}

command -v gh >/dev/null 2>&1  || die "gh CLI not found. Install from https://cli.github.com"
gh auth status >/dev/null 2>&1 || die "Not authenticated. Run: gh auth login"
[[ -f "$LOCAL_PROPS" ]] || die "File not found: $LOCAL_PROPS"

STORE_FILE=$(prop_get "signing.storeFile")
STORE_PASS=$(prop_get "signing.storePassword")
KEY_ALIAS=$(prop_get "signing.keyAlias")
KEY_PASS=$(prop_get "signing.keyPassword")

[[ -n "$STORE_FILE" ]] || die "signing.storeFile not found in $LOCAL_PROPS"
[[ -n "$STORE_PASS" ]] || die "signing.storePassword not found in $LOCAL_PROPS"
[[ -n "$KEY_ALIAS" ]] || die "signing.keyAlias not found in $LOCAL_PROPS"
[[ -n "$KEY_PASS" ]] || die "signing.keyPassword not found in $LOCAL_PROPS"
[[ -f "$STORE_FILE" ]] || die "Keystore file not found: $STORE_FILE"
[[ -f "$PLAY_SERVICE_ACCOUNT_FILE" ]] || die "File not found: $PLAY_SERVICE_ACCOUNT_FILE (set PLAY_SERVICE_ACCOUNT_FILE=... if needed)"
[[ -f "$GOOGLE_SERVICES_FILE" ]] || die "File not found: $GOOGLE_SERVICES_FILE (set GOOGLE_SERVICES_FILE=... if needed)"

KEY_ALIAS_SECRET="KEY_ALIAS_${APP_SECRET_SUFFIX}"
KEY_PASSWORD_SECRET="KEY_PASSWORD_${APP_SECRET_SUFFIX}"
PLAY_SERVICE_ACCOUNT_SECRET="GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_${APP_SECRET_SUFFIX}"
GOOGLE_SERVICES_SECRET="GOOGLE_SERVICES_JSON_${APP_SECRET_SUFFIX}"

echo ""
echo "🔐  Setting GitHub Actions secrets for: $REPO"
echo "    Shared keystore          : $STORE_FILE"
echo "    Shared keystore password : KEYSTORE_PASSWORD"
echo "    App secret suffix        : $APP_SECRET_SUFFIX"
echo "    App key alias secret     : $KEY_ALIAS_SECRET"
echo "    App key password secret  : $KEY_PASSWORD_SECRET"
echo "    App Play JSON secret     : $PLAY_SERVICE_ACCOUNT_SECRET"
echo "    App Firebase JSON secret : $GOOGLE_SERVICES_SECRET"
echo ""

info "Encoding shared keystore as base64..."
KEYSTORE_B64=$(base64 < "$STORE_FILE" | tr -d '\n')
set_secret "KEYSTORE_BASE64" "$KEYSTORE_B64"
set_secret "KEYSTORE_PASSWORD" "$STORE_PASS"

set_secret "$KEY_ALIAS_SECRET" "$KEY_ALIAS"
set_secret "$KEY_PASSWORD_SECRET" "$KEY_PASS"

info "Uploading app-specific Play service account JSON..."
set_secret_file "$PLAY_SERVICE_ACCOUNT_SECRET" "$PLAY_SERVICE_ACCOUNT_FILE"

info "Uploading app-specific google-services.json..."
set_secret_file "$GOOGLE_SERVICES_SECRET" "$GOOGLE_SERVICES_FILE"

echo ""
echo "🎉  Done! Secrets are configured."
echo ""
echo "    Verify with: gh secret list --repo $REPO"
