# MQFTPServer 🥽

A **VR-optimized FTP Server** application designed specifically for **Meta Quest 3** devices, built with the Meta Spatial SDK. This app allows you to easily transfer files to and from your VR headset using standard FTP clients like FileZilla or WinSCP.

## 🌟 Features

### Core Functionality
- **Full FTP Server Implementation** - Standard FTP protocol with authentication
- **VR-Native Interface** - Optimized for Meta Quest 3 VR experience
- **Pancake Mode Support** - Run as a 2D panel in VR space
- **Auto-Start Server** - Automatically starts on app launch for convenience
- **Wireless File Transfer** - Transfer files over your local network

### FTP Protocol Support
- **Authentication** - Secure login with username/password
- **Directory Navigation** - Browse, create, and delete folders
- **File Operations** - Upload, download, rename, and delete files
- **Passive Mode** - Full passive FTP support for firewalls
- **UTF-8 Support** - International character support
- **Real-time Logging** - Detailed operation logs for debugging

### VR-Specific Features
- **Hand Tracking** - Native Meta Quest hand tracking support
- **Passthrough Mode** - See your real environment while using the app
- **Wake Lock** - Keeps server running even when headset is idle
- **Touch Interface** - Optimized for VR touch controllers
- **Background Operation** - Server continues running when app is minimized

## 📱 System Requirements

- **Device**: Meta Quest 2, Quest Pro, or Quest 3
- **OS**: HorizonOS SDK 69+
- **Network**: WiFi connection (same network as your computer)
- **Storage**: Access to device storage for file transfers

## 🚀 Quick Start

### 1. Installation
1. Download the latest APK from releases
2. Install on your Meta Quest device
3. Grant storage permissions when prompted

### 2. Basic Setup
1. Launch the app in your Quest
2. The server will auto-start with default settings:
   - **Username**: `admin`
   - **Password**: `password`
   - **Port**: `2121`
   - **Root Folder**: `/Movies/ftp`

### 3. Connect from Your Computer
1. Note the IP address displayed in the app
2. Open your FTP client (FileZilla, WinSCP, etc.)
3. Connect using:
   - **Host**: The IP shown in the app
   - **Port**: `2121`
   - **Username**: `admin`
   - **Password**: `password`

## ⚙️ Configuration

### Server Settings
- **Port**: Default 2121 (change if needed to avoid conflicts)
- **Username/Password**: Customize for security
- **Root Folder**: Choose where files are accessible

### Recommended Folders
The app suggests common Quest folders:
- **Movies** (`/Movies/ftp`) - For video files
- **Documents** (`/Documents/ftp`) - For documents
- **Pictures** (`/Pictures/ftp`) - For images
- **Downloads** (`/Downloads/ftp`) - For general downloads

### Custom Folder Selection
Use the "📂" browse button to select any accessible folder on your device.

## 🔧 Technical Details

### Architecture
- **Language**: Kotlin
- **Framework**: Meta Spatial SDK
- **Minimum SDK**: Android API 29
- **Target SDK**: Android API 34
- **VR SDK**: HorizonOS 69+

### FTP Implementation
- **Protocol**: RFC 959 compliant FTP server
- **Mode**: Passive mode for firewall compatibility
- **Encoding**: UTF-8 for international filenames
- **Security**: Username/password authentication
- **Concurrency**: Multiple client support with coroutines

### Permissions Required
- `INTERNET` - Network communication
- `ACCESS_NETWORK_STATE` - Network status
- `READ/WRITE_EXTERNAL_STORAGE` - File access
- `MANAGE_EXTERNAL_STORAGE` - Full storage access (Android 11+)
- `WAKE_LOCK` - Keep server running
- `FOREGROUND_SERVICE` - Background operation

## 🛠️ Development

### Building from Source

1. **Prerequisites**:
   - Android Studio with Meta Spatial SDK
   - Java JDK 17+
   - Android SDK 34

2. **Clone and Build**:
   ```bash
   git clone https://github.com/Guymestef/MQFTPServer.git
   cd MQFTPServer
   ./gradlew assembleDebug
   ```

3. **Release Build**:
   Use the provided PowerShell scripts for signed releases:
   ```powershell
   .\build-release-simple.ps1
   ```

### Project Structure
```
app/src/main/java/com/mqftpserver/app/
├── PancakeActivity.kt          # Main VR activity
├── SimpleFtpServer.kt          # FTP server implementation
└── res/
    ├── layout/                 # VR-optimized layouts
    ├── values/                 # Strings and themes
    └── drawable/               # VR icons and graphics
```

## 🔒 Security Considerations

### Network Security
- Server only accessible on local network
- Default credentials should be changed for production use
- No HTTPS/FTPS support (planned for future versions)

### Storage Security  
- Server root is sandboxed to selected folder
- Path traversal protection implemented
- Read/write permissions respected

## 🐛 Troubleshooting

### Common Issues

**Server won't start**:
- Check storage permissions are granted
- Ensure port 2121 isn't already in use
- Verify WiFi connection is active

**Can't connect from computer**:
- Confirm both devices on same WiFi network
- Check firewall settings on computer
- Verify IP address in app matches connection settings

**Files not visible**:
- Check folder permissions
- Ensure files are in the configured root directory
- Try refreshing the FTP client directory listing

**Performance issues**:
- Close other VR apps to free memory
- Use wired connection for large file transfers
- Enable airplane mode then re-enable WiFi to reset connection

### Debug Mode
Enable detailed logging by checking logcat output:
```bash
adb logcat | grep "FTP\|SimpleFtpServer"
```

## 🤝 Contributing

Contributions are welcome! Please read our contributing guidelines and submit pull requests for any improvements.

### Development Areas
- Additional FTP commands (FTPS, SFTP)
- Enhanced VR interface elements
- Performance optimizations
- Internationalization
- Enhanced security features

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Meta** for the Spatial SDK and Quest platform
- **Android FTP Server** projects for protocol reference
- **VR Development Community** for testing and feedback

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Guymestef/MQFTPServer/issues)
- **Wiki**: [Project Wiki](https://github.com/Guymestef/MQFTPServer/wiki)
- **Discussions**: [GitHub Discussions](https://github.com/Guymestef/MQFTPServer/discussions)

---

**Made with ❤️ for the VR community**

*Experience seamless file transfers in virtual reality!*