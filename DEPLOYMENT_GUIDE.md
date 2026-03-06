# Czech Declension Quiz - Deployment Guide

## Overview

The app has automated deployment to Google Play via GitHub Actions.

## Quick Start

### Deploy to Internal Testing
```bash
# Via GitHub UI
Actions → Deploy to Google Play → Run workflow → Select "internal"
```

### Deploy to Production
```bash
# Create and push version tag
git tag v1.0.0
git push origin v1.0.0

# Automatically:
# ✅ Builds signed APK and AAB
# ✅ Deploys to Google Play production
# ✅ Creates GitHub release
```

## Workflows

### 1. Build APK (`.github/workflows/build.yml`)
- **Triggers**: Push/PR to master (except version tags)
- **Purpose**: Continuous integration, verify builds work
- **Outputs**: Unsigned debug and release APKs

### 2. UI Tests (`.github/workflows/ui-tests.yml`)
- **Triggers**: Push/PR to master (except version tags)
- **Purpose**: Automated UI testing
- **Outputs**: Test reports, screenshots

### 3. Deploy to Google Play (`.github/workflows/deploy.yml`)
- **Triggers**: 
  - Manual: workflow_dispatch
  - Automatic: version tags (`v*.*.*`)
- **Purpose**: Production deployment
- **Outputs**: Signed APK, AAB, Google Play release

## Setup Required

### 1. Create Signing Keystore

```bash
keytool -genkey -v -keystore release-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release-key \
  -dname "CN=Your Name, OU=Your Org, O=Your Company, L=City, ST=State, C=CZ"
```

**Store securely!** This keystore is required for all future releases.

### 2. Configure GitHub Secrets

Go to: Repository → Settings → Secrets and variables → Actions → New repository secret

| Secret Name | Description | How to Get |
|-------------|-------------|------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore | `base64 -i release-keystore.jks \| pbcopy` |
| `KEYSTORE_PASSWORD` | Keystore password | Password you set when creating keystore |
| `KEY_ALIAS` | Key alias | `release-key` (or what you used) |
| `KEY_PASSWORD` | Key password | Password for the key |
| `GOOGLE_SERVICES_JSON` | Firebase config | `cat app/google-services.json \| pbcopy` |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Play Console API key | See below |

### 3. Create Google Play Service Account

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Settings → API access
4. Click "Create new service account"
5. Follow link to Google Cloud Console
6. Create service account with name like "github-actions"
7. Grant role: "Service Account User"
8. Create JSON key and download
9. Back in Play Console, grant permissions:
   - ✅ Release to production
   - ✅ Release to testing tracks
   - ✅ Manage app releases
10. Copy JSON content to GitHub secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`

## Deployment Methods

### Method 1: Manual Deployment (Recommended for Testing)

1. Go to GitHub → Actions
2. Select "Deploy to Google Play"
3. Click "Run workflow"
4. Choose:
   - **Branch**: master
   - **Track**: internal/alpha/beta/production
   - **Release notes**: Optional custom notes
5. Click "Run workflow"

**Use cases:**
- Internal testing
- Alpha/beta releases
- Hotfixes
- Manual control over deployment

### Method 2: Automatic Deployment (Recommended for Production)

```bash
# 1. Update version in app/build.gradle
versionCode 2
versionName "1.0.1"

# 2. Commit changes
git add app/build.gradle
git commit -m "Bump version to 1.0.1"

# 3. Create and push tag
git tag v1.0.1
git push origin master
git push origin v1.0.1

# 4. Workflow automatically:
# - Builds signed APK and AAB
# - Deploys to production track
# - Creates GitHub release with downloads
```

**Use cases:**
- Production releases
- Versioned releases
- Automated deployment
- GitHub release creation

## Release Tracks

| Track | Purpose | Rollout | Audience |
|-------|---------|---------|----------|
| **Internal** | Team testing | Immediate | Up to 100 testers |
| **Alpha** | Early testing | Immediate | Opt-in testers |
| **Beta** | Public testing | Gradual | Wider audience |
| **Production** | Public release | Gradual/Full | All users |

## Release Notes

Update release notes before deployment:

```bash
# English
echo "• New quiz features
• Bug fixes
• Performance improvements" > distribution/whatsnew/whatsnew-en-US

# Czech
echo "• Nové funkce kvízu
• Opravy chyb
• Vylepšení výkonu" > distribution/whatsnew/whatsnew-cs-CZ

git add distribution/whatsnew/
git commit -m "Update release notes"
git push
```

**Limits:**
- Maximum 500 characters per language
- Plain text only
- No file extension

## Version Naming

Follow [Semantic Versioning](https://semver.org/):

- `v1.0.0` - Major release (breaking changes)
- `v1.1.0` - Minor release (new features)
- `v1.1.1` - Patch release (bug fixes)

**Example:**
```bash
# Bug fix release
git tag v1.0.1

# New feature release
git tag v1.1.0

# Major update
git tag v2.0.0
```

## Troubleshooting

### "Keystore not found"
```bash
# Verify secret is set correctly
echo "$KEYSTORE_BASE64" | base64 --decode > test.jks
keytool -list -v -keystore test.jks
```

### "Authentication failed"
- Check `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` is valid JSON
- Verify service account has correct permissions in Play Console
- Ensure service account is linked to your app

### "Package name mismatch"
- Verify package name in `app/build.gradle` matches Play Console
- Should be: `com.usharik.app`

### "Version code must be greater"
- Increment `versionCode` in `app/build.gradle`
- Each release must have a higher version code than the previous

## Security Best Practices

✅ **DO:**
- Store keystore securely offline
- Use GitHub secrets for sensitive data
- Rotate service account keys periodically
- Use different keystores for debug/release

❌ **DON'T:**
- Commit keystore to repository
- Share keystore password
- Commit real `google-services.json`
- Reuse passwords

## Monitoring

After deployment:

1. **Google Play Console**
   - Check release status
   - Monitor crash reports
   - Review user feedback

2. **GitHub Actions**
   - View workflow logs
   - Download artifacts (APK/AAB)
   - Check deployment status

3. **GitHub Releases**
   - Verify release created
   - Test download links
   - Review release notes

## Support

For detailed information, see:
- `distribution/README.md` - Deployment configuration
- `.github/workflows/deploy.yml` - Workflow definition
- [Google Play Console](https://play.google.com/console) - App management

---

**Ready to deploy!** 🚀

