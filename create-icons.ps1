# PowerShell script to create placeholder PNG icons
# This creates simple colored PNG files as placeholders

$iconSizes = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

$baseDir = "d:\Projects\MQFTPServer\app\src\main\res"

# Create a simple XML that can serve as PNG alternative
$simpleIconContent = @'
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="oval">
            <solid android:color="#16213E"/>
            <size android:width="{0}dp" android:height="{0}dp"/>
        </shape>
    </item>
    <item android:gravity="center">
        <shape android:shape="rectangle">
            <solid android:color="#4ECDC4"/>
            <size android:width="{1}dp" android:height="{2}dp"/>
        </shape>
    </item>
</layer-list>
'@

foreach ($density in $iconSizes.Keys) {
    $size = $iconSizes[$density]
    $serverWidth = [math]::Round($size * 0.25)
    $serverHeight = [math]::Round($size * 0.33)
    
    $content = $simpleIconContent -f $size, $serverWidth, $serverHeight
    
    $iconPath = "$baseDir\mipmap-$density\ic_launcher.xml"
    Set-Content -Path $iconPath -Value $content -Encoding UTF8
    
    # Also create round version
    $roundIconPath = "$baseDir\mipmap-$density\ic_launcher_round.xml"
    Set-Content -Path $roundIconPath -Value $content -Encoding UTF8
    
    Write-Host "Created icons for $density density ($size dp)"
}

Write-Host "Icon creation completed!"
