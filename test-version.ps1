# Test script for version generation
# Run this to preview the version that would be generated

param(
    [Parameter(Mandatory=$false)]
    [switch]$Help = $false
)

if ($Help) {
    Write-Host @"
MQFTPServer Version Preview Script
==================================

This script shows what version numbers would be generated for a build
without actually building the APK.

Usage: .\test-version.ps1

Output:
- Shows current date-based version
- Shows git commit count for build number
- Shows example APK filename
"@
    exit 0
}

function Get-VersionInfo {
    # Get current date
    $currentDate = Get-Date
    $major = $currentDate.Year
    $minor = $currentDate.Month
    $patch = $currentDate.Day
    
    # Get commit count for build number
    try {
        $commitCount = (git rev-list --count HEAD 2>$null) -replace '\s', ''
        if (-not $commitCount -or $commitCount -eq "") {
            Write-Warning "Could not get git commit count, using default build number 1"
            $commitCount = "1"
        }
        $build = [int]$commitCount
    }
    catch {
        Write-Warning "Git not available or not a git repository, using default build number 1"
        $build = 1
    }
    
    $versionName = "$major.$minor.$patch"
    $versionCode = $build
    
    return @{
        Major = $major
        Minor = $minor
        Patch = $patch
        Build = $build
        VersionName = $versionName
        VersionCode = $versionCode
    }
}

Write-Host "MQFTPServer Version Preview" -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan
Write-Host ""

$versionInfo = Get-VersionInfo

Write-Host "Date-based Version Information:" -ForegroundColor Yellow
Write-Host "  Major (Year):  $($versionInfo.Major)" -ForegroundColor White
Write-Host "  Minor (Month): $($versionInfo.Minor)" -ForegroundColor White
Write-Host "  Patch (Day):   $($versionInfo.Patch)" -ForegroundColor White
Write-Host ""

Write-Host "Build Information:" -ForegroundColor Yellow
Write-Host "  Build Number:  $($versionInfo.Build) (from git commit count)" -ForegroundColor White
Write-Host ""

Write-Host "Final Version:" -ForegroundColor Green
Write-Host "  Version Name:  $($versionInfo.VersionName)" -ForegroundColor White
Write-Host "  Version Code:  $($versionInfo.VersionCode)" -ForegroundColor White
Write-Host ""

$timestamp = Get-Date -Format 'yyyyMMdd-HHmm'
$exampleApkName = "MQFTPServer-v$($versionInfo.VersionName)-build$($versionInfo.Build)-$timestamp.apk"

Write-Host "Example APK filename:" -ForegroundColor Green
Write-Host "  $exampleApkName" -ForegroundColor White
Write-Host ""

Write-Host "This version will be automatically applied when building with:" -ForegroundColor Cyan
Write-Host "  .\build-release-simple.ps1" -ForegroundColor White
