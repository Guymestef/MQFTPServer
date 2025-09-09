# MQFTPServer Release Build Scripts

This directory contains scripts to build a release signed APK for the MQFTPServer Android application.

## Scripts Available

### 1. `build-release-simple.ps1` (Recommended)
A PowerShell script that provides secure password handling and automatic build configuration.

**Features:**
- Secure password input (passwords are not stored in plain text)
- Automatic signing configuration injection
- Build validation and error handling
- Automatic APK copying to project root with timestamp
- Backup and restore of build configuration

**Usage:**

```powershell
# Basic usage (will auto-detect release keystore in common locations)
.\build-release-simple.ps1

# With parameters
.\build-release-simple.ps1 -KeystorePath "C:\path\to\your\keystore.jks" -KeyAlias "your-key-alias"

# Clean build
.\build-release-simple.ps1 -Clean

# Show help
.\build-release-simple.ps1 -Help
```

### 2. `build-release.bat`
A Windows batch script for users who prefer batch files over PowerShell.

**Note:** This script shows passwords in plain text during input. Use the PowerShell script for better security.

**Usage:**
```cmd
# Run the batch script
build-release.bat
```

### 3. `build-release.ps1`
A more comprehensive PowerShell script with advanced features (may have some compatibility issues).

## Prerequisites

1. **Java JDK 17 or later** - Required for Android development
2. **Android Keystore** - A `.jks` or `.keystore` file for signing your APK
3. **Gradle Wrapper** - Should be present in your project (gradlew.bat)

## Before You Start

### Release Keystore Auto-Detection

The script will automatically search for release keystores in these common locations:

```cmd
%USERPROFILE%\.android\release.keystore
%USERPROFILE%\Documents\release.jks
%USERPROFILE%\Documents\release.keystore
.\keystore\release.jks
.\release.keystore
.\release.jks
```

### Debug Keystore Fallback (Development Only)

If no release keystore is found, the script will offer the Android Studio debug keystore as a fallback:

```cmd
%USERPROFILE%\.android\debug.keystore
```

**⚠️ WARNING:** The debug keystore uses:

- **Alias:** `androiddebugkey`
- **Password:** `android` (both keystore and key password)

**Note:** The debug keystore is only suitable for development and testing. APKs signed with the debug certificate cannot be published to app stores.

### Creating a Keystore (if you don't have one)

If you don't have a keystore yet, create one using the Java keytool:

```cmd
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

**Important:** Keep your keystore and passwords safe! You'll need them for every app update.

### Environment Variables (Optional)

You can set these environment variables to avoid typing them repeatedly:

```powershell
$env:MQFTP_KEYSTORE_PATH = "C:\path\to\your\keystore.jks"
$env:MQFTP_KEY_ALIAS = "your-key-alias"
```

## Build Process

The scripts will:

1. Verify prerequisites (Java, Gradle, project structure)
2. Prompt for signing information (keystore path, passwords, key alias)
3. Temporarily modify the build configuration to include signing
4. Build the release APK with signing
5. Copy the APK to the project root with a timestamp
6. Restore the original build configuration

## Output

Successful builds will generate:
- `app/build/outputs/apk/release/app-release.apk` - The main APK file
- `MQFTPServer-release-YYYYMMDD-HHMM.apk` - A timestamped copy in the project root

## Security Notes

- The PowerShell script handles passwords securely using SecureString
- Signing information is temporarily added to gradle.properties and removed after build
- Original build files are backed up and restored
- Sensitive information is cleared from memory after use

## Troubleshooting

### Common Issues

1. **"Java not found"**
   - Install Java JDK 17 or later
   - Add Java to your PATH environment variable

2. **"Keystore not found"**
   - Verify the keystore file path is correct
   - Ensure the file has .jks or .keystore extension

3. **"Build failed"**
   - Check if you have the latest Android SDK components
   - Verify your keystore passwords are correct
   - Try a clean build with the `-Clean` parameter

4. **"Permission denied"**
   - Run PowerShell as Administrator if needed
   - Check if antivirus software is blocking the script

### Getting Help

Run the PowerShell script with the `-Help` parameter for detailed usage information:

```powershell
.\build-release-simple.ps1 -Help
```

## Example Workflow

1. Open PowerShell in the project directory
2. Run: `.\build-release-simple.ps1`
3. Enter your keystore path when prompted
4. Enter your key alias when prompted
5. Enter passwords securely when prompted
6. Wait for the build to complete
7. Find your signed APK in the project root

## Notes

- The first build may take longer as Gradle downloads dependencies
- Keep your keystore and passwords in a secure location
- Use the same keystore for all app updates to maintain compatibility
- Test the generated APK on a device before publishing

## Meta Spatial SDK

This project uses the Meta Spatial SDK for VR development. The build scripts are configured to handle the specific requirements of this SDK, including:
- NDK configuration for 16KB page size compatibility
- Special packaging requirements for VR applications
- Meta-specific dependencies and libraries

The generated APK will be compatible with Meta Quest devices and other VR platforms supported by the Meta Spatial SDK.
