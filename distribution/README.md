# Google Play Deployment

This directory contains configuration for deploying the Czech Declension Quiz app to Google Play.

## Workflow

The deployment is automated via GitHub Actions (`.github/workflows/deploy.yml`).

### Triggers

1. **Manual Deployment** (workflow_dispatch)
   - Go to Actions → Deploy to Google Play → Run workflow
   - Choose release track: internal, alpha, beta, or production
   - Optionally add release notes

2. **Automatic Deployment** (tag push)
   - Push a version tag: `git tag v1.0.0 && git push origin v1.0.0`
   - Automatically deploys to production track
   - Creates GitHub release with APK and AAB

## Release Tracks

| Track | Purpose | Audience |
|-------|---------|----------|
| **internal** | Internal testing | Team members only |
| **alpha** | Early testing | Opt-in testers |
| **beta** | Public testing | Wider audience |
| **production** | Public release | All users |

## Required Secrets

Configure these in GitHub Settings → Secrets and variables → Actions:

### 1. KEYSTORE_BASE64
Your Android signing keystore encoded in base64.

**To create:**
```bash
# Generate keystore (if you don't have one)
keytool -genkey -v -keystore release-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release-key

# Encode to base64
base64 -i release-keystore.jks | pbcopy
```

Then paste into GitHub secret `KEYSTORE_BASE64`.

### 2. KEYSTORE_PASSWORD
Password for the keystore file.

### 3. KEY_ALIAS
Alias of the key in the keystore (e.g., `release-key`).

### 4. KEY_PASSWORD
Password for the specific key.

### 5. GOOGLE_SERVICES_JSON
Your real Firebase `google-services.json` file content.

**To create:**
```bash
cat app/google-services.json | pbcopy
```

Then paste into GitHub secret `GOOGLE_SERVICES_JSON`.

### 6. GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
Google Play Console service account JSON.

**To create:**
1. Go to [Google Play Console](https://play.google.com/console)
2. Settings → API access
3. Create new service account or use existing
4. Grant permissions: "Release to production, exclude devices, and use Play App Signing"
5. Download JSON key
6. Copy content to GitHub secret

## Release Notes

Release notes are stored in `distribution/whatsnew/` directory:

- `whatsnew-en-US` - English release notes
- `whatsnew-cs-CZ` - Czech release notes

**Format:**
- Plain text file
- Maximum 500 characters
- No file extension

**To update:**
```bash
echo "New features and bug fixes" > distribution/whatsnew/whatsnew-en-US
echo "Nové funkce a opravy chyb" > distribution/whatsnew/whatsnew-cs-CZ
git add distribution/whatsnew/
git commit -m "Update release notes"
```

## Manual Deployment

### Option 1: Via GitHub Actions UI

1. Go to repository → Actions
2. Select "Deploy to Google Play" workflow
3. Click "Run workflow"
4. Choose branch (usually `master`)
5. Select release track
6. Add release notes (optional)
7. Click "Run workflow"

### Option 2: Via Git Tag

```bash
# Create and push tag
git tag v1.2.3
git push origin v1.2.3

# This will:
# - Build signed APK and AAB
# - Deploy to production track
# - Create GitHub release
```

## What Gets Deployed

The workflow builds and uploads:

1. **AAB (Android App Bundle)** - Uploaded to Google Play
   - Optimized for Play Store distribution
   - Smaller download size for users
   - Required for new apps

2. **APK** - Uploaded as GitHub release artifact
   - Direct installation file
   - Useful for testing and distribution outside Play Store

## Deployment Process

1. **Build**
   - Checkout code
   - Set up Java 21
   - Decode keystore and google-services.json
   - Build signed APK and AAB

2. **Upload**
   - Upload AAB to Google Play
   - Set release track (internal/alpha/beta/production)
   - Add release notes from `whatsnew/` directory

3. **Artifacts**
   - Upload APK and AAB to GitHub Actions artifacts
   - Create GitHub release (for tag pushes)

4. **Cleanup**
   - Remove keystore file (security)

## Troubleshooting

### Build fails with "keystore not found"
- Check that `KEYSTORE_BASE64` secret is set correctly
- Verify base64 encoding: `echo "$KEYSTORE_BASE64" | base64 --decode > test.jks`

### Upload fails with "authentication error"
- Verify `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` is valid
- Check service account has correct permissions in Play Console

### "Package name not found"
- Ensure app is already created in Google Play Console
- Verify package name matches: `com.usharik.app`

### Release notes not showing
- Check files exist in `distribution/whatsnew/`
- Verify file names match locale codes (e.g., `whatsnew-en-US`)
- Ensure files are plain text with no extension

## Security Notes

⚠️ **NEVER commit these files:**
- `release-keystore.jks` - Signing keystore
- Real `google-services.json` - Firebase credentials
- Service account JSON - Google Play credentials

✅ **Safe to commit:**
- `utils/google-services.json.dummy` - Dummy Firebase config
- `distribution/whatsnew/*` - Release notes
- Workflow files

## Version Naming

Follow semantic versioning for tags:
- `v1.0.0` - Major release
- `v1.1.0` - Minor release (new features)
- `v1.1.1` - Patch release (bug fixes)

The version code and name should be updated in `app/build.gradle` before tagging.

