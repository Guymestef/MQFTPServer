package com.mqftpserver.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.Manifest

// default activity
class PancakeActivity : Activity() {
  private var ftpServer: SimpleFtpServer? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private val PERMISSION_REQUEST_CODE = 1001

  companion object {
    private const val TAG = "PancakeActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setTheme(R.style.PanelAppThemeTransparent)
    setContentView(R.layout.ui_pancake_modern)

    // Prevent sleep to keep FTP server active
    setupWakeLock()
    
    // Keep screen on
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    val portEdit = findViewById<EditText>(R.id.ftp_port)
    val userEdit = findViewById<EditText>(R.id.ftp_user)
    val passEdit = findViewById<EditText>(R.id.ftp_password)
    val rootEdit = findViewById<EditText>(R.id.ftp_root)
    val statusText = findViewById<TextView>(R.id.server_status)
    val ipText = findViewById<TextView>(R.id.ip_address)
    val btnStart = findViewById<Button>(R.id.btn_start_ftp)
    val btnStop = findViewById<Button>(R.id.btn_stop_ftp)
    val btnBrowse = findViewById<Button>(R.id.btn_browse_folder)

    // Check permissions before starting
    if (!checkStoragePermissions()) {
      requestStoragePermissions()
      return@onCreate
    }

    // Set default root folder to Videos
    val defaultFtpRoot = try {
      val videosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
      if (videosDir.exists() || videosDir.mkdirs()) {
        File(videosDir, "ftp").absolutePath
      } else {
        // Fallback to internal folder if Videos is not accessible
        File(applicationContext.filesDir, "ftp").absolutePath
      }
    } catch (e: Exception) {
      Log.e("FTP", "Error accessing Videos folder", e)
      File(applicationContext.filesDir, "ftp").absolutePath
    }
    
    rootEdit?.setText(defaultFtpRoot)

    // Pre-fill fields with default values
    portEdit?.setText(getString(R.string.default_port))
    userEdit?.setText(getString(R.string.default_username))
    passEdit?.setText(getString(R.string.default_password))

    // Display local IP address
    val localIP = getLocalIPAddress()
    ipText?.text = getString(R.string.local_ip_format, localIP)

    // Start FTP server automatically after a short delay
    // to ensure the interface is completely initialized
    Handler(Looper.getMainLooper()).postDelayed({
      startFtpServerAutomatically(portEdit, userEdit, passEdit, rootEdit, statusText, localIP)
    }, 1000) // 1 second delay

    btnBrowse?.setOnClickListener {
      showSimpleFolderPicker { selectedPath ->
        rootEdit?.setText(selectedPath)
      }
    }

    btnStart?.setOnClickListener {
      try {
        val port = portEdit?.text?.toString()?.toIntOrNull() ?: 2121
        val user = userEdit?.text?.toString()?.ifBlank { "admin" } ?: "admin"
        val pass = passEdit?.text?.toString()?.ifBlank { "password" } ?: "password"
        val rootPath = rootEdit?.text?.toString()?.ifBlank { 
          File(applicationContext.filesDir, "ftp").absolutePath 
        } ?: File(applicationContext.filesDir, "ftp").absolutePath

        val rootDir = File(rootPath)
        if (!rootDir.exists()) {
          rootDir.mkdirs()
          // Create some test files
          createTestFiles(rootDir)
        }

        ftpServer = SimpleFtpServer(port, user, pass, rootDir)
        ftpServer?.start()
        
        // Ensure WakeLock is active to maintain the server
        if (wakeLock?.isHeld != true) {
          setupWakeLock()
        }
        
        statusText?.text = getString(R.string.server_running_on, localIP, port)
        statusText?.setTextColor(0xFF4CAF50.toInt()) // Green for success
        Toast.makeText(this, getString(R.string.server_running_on, localIP, port), Toast.LENGTH_LONG).show()
      } catch (e: Exception) {
        Toast.makeText(this, getString(R.string.server_start_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
      }
    }

    btnStop?.setOnClickListener {
      ftpServer?.stop()
      ftpServer = null
      statusText?.text = getString(R.string.server_stopped_status)
      statusText?.setTextColor(0xFFF44336.toInt()) // Red for stopped
      Toast.makeText(this, getString(R.string.server_stopped_status), Toast.LENGTH_SHORT).show()
    }
  }

  private fun checkStoragePermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Android 11+ - Check MANAGE_EXTERNAL_STORAGE
      Environment.isExternalStorageManager()
    } else {
      // Android 10 and below - Check READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE
      ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
  }

  private fun requestStoragePermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Android 11+ - Request MANAGE_EXTERNAL_STORAGE
      try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, PERMISSION_REQUEST_CODE)
        Toast.makeText(this, getString(R.string.permission_request_all_files), Toast.LENGTH_LONG).show()
      } catch (e: Exception) {
        Log.e(TAG, "Error requesting MANAGE_EXTERNAL_STORAGE permission", e)
        // Fallback to general settings screen
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivityForResult(intent, PERMISSION_REQUEST_CODE)
      }
    } else {
      // Android 10 and below - Request classic permissions
      ActivityCompat.requestPermissions(this, 
        arrayOf(
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), 
        PERMISSION_REQUEST_CODE
      )
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PERMISSION_REQUEST_CODE) {
      if (checkStoragePermissions()) {
        Toast.makeText(this, getString(R.string.permissions_granted_restart), Toast.LENGTH_LONG).show()
        // Optional: Automatically restart the activity
        recreate()
      } else {
        Toast.makeText(this, getString(R.string.permissions_denied_limitations), Toast.LENGTH_LONG).show()
      }
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PERMISSION_REQUEST_CODE) {
      if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
        Toast.makeText(this, getString(R.string.permissions_granted), Toast.LENGTH_SHORT).show()
        recreate()
      } else {
        Toast.makeText(this, getString(R.string.permissions_denied_limitations), Toast.LENGTH_LONG).show()
      }
    }
  }





  private fun showSimpleFolderPicker(onFolderSelected: (String) -> Unit) {
    try {
      var currentDirectory = try {
        Environment.getExternalStorageDirectory()
      } catch (e: Exception) {
        applicationContext.filesDir
      }

      fun showFolderDialog() {
        try {
          val folders = mutableListOf<Pair<String, File>>()
          
          // Add option to go up if possible
          currentDirectory.parent?.let { parent ->
            folders.add(Pair("⬆️ Parent folder", File(parent)))
          }
          
          // Add folders from current directory
          currentDirectory.listFiles()?.let { files ->
            files.filter { it.isDirectory && it.canRead() }
              .sortedBy { it.name.lowercase() }
              .forEach { folder ->
                val displayName = "📁 ${folder.name}"
                folders.add(Pair(displayName, folder))
              }
          }
          
          // Add option to select current folder
          folders.add(0, Pair("✅ Select this folder: ${currentDirectory.name}", currentDirectory))
          
          val folderNames = folders.map { it.first }.toTypedArray()
          
          AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
            .setTitle(getString(R.string.folder_browser_title, currentDirectory.absolutePath))
            .setItems(folderNames) { _, which ->
              val selectedFolder = folders[which].second
              if (which == 0) {
                // Select current folder
                onFolderSelected(currentDirectory.absolutePath)
                Toast.makeText(this, getString(R.string.folder_selected, currentDirectory.absolutePath), Toast.LENGTH_SHORT).show()
              } else {
                // Navigate to selected folder
                currentDirectory = selectedFolder
                showFolderDialog() // Recursion to continue navigation
              }
            }
            .setNeutralButton("📁 New folder") { _, _ ->
              showCreateFolderDialog(currentDirectory) {
                showFolderDialog() // Refresh after creation
              }
            }
            .setNegativeButton("❌ Cancel", null)
            .show()
        } catch (e: Exception) {
          Log.e("FTP", "Error in showFolderDialog", e)
          Toast.makeText(this, getString(R.string.folder_access_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
        }
      }
      
      showFolderDialog()
    } catch (e: Exception) {
      Log.e("FTP", "Error in showSimpleFolderPicker", e)
      Toast.makeText(this, getString(R.string.folder_selector_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
    }
  }

  private fun showCreateFolderDialog(parentDirectory: File, onSuccess: () -> Unit) {
    try {
      val input = EditText(this)
      input.hint = "New folder name"
      input.setPadding(50, 30, 50, 30)
      
      AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
        .setTitle(getString(R.string.create_new_folder_title))
        .setMessage(getString(R.string.create_folder_location, parentDirectory.absolutePath))
        .setView(input)
        .setPositiveButton("✅ Create") { _, _ ->
          val folderName = input.text.toString().trim()
          if (folderName.isNotEmpty()) {
            try {
              val newFolder = File(parentDirectory, folderName)
              if (newFolder.mkdir()) {
                onSuccess()
                Toast.makeText(this, getString(R.string.folder_created, folderName), Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(this, getString(R.string.folder_creation_failed), Toast.LENGTH_SHORT).show()
              }
            } catch (e: Exception) {
              Log.e("FTP", "Folder creation error", e)
              Toast.makeText(this, getString(R.string.folder_creation_error, e.message ?: "Unknown"), Toast.LENGTH_SHORT).show()
            }
          } else {
            Toast.makeText(this, getString(R.string.enter_folder_name), Toast.LENGTH_SHORT).show()
          }
        }
        .setNegativeButton("❌ Cancel", null)
        .show()
    } catch (e: Exception) {
      Log.e("FTP", "Error in showCreateFolderDialog", e)
      Toast.makeText(this, "Dialog error: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }



  private fun showFolderSelector(onFolderSelected: (String) -> Unit) {
    val commonFolders = mutableListOf<Pair<String, String>>()
    
    // Add Videos first (default folder)
    try {
      val videosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
      commonFolders.add(Pair("📹 Videos (default)", videosDir.absolutePath))
    } catch (e: Exception) {
      Log.e("FTP", "Error accessing Videos folder", e)
    }
    
    // Add other available folders
    commonFolders.add(Pair("Internal App", applicationContext.filesDir.absolutePath))
    
    val externalFilesDir = applicationContext.getExternalFilesDir(null)
    if (externalFilesDir != null) {
      commonFolders.add(Pair("External App", externalFilesDir.absolutePath))
    }
    
    try {
      commonFolders.add(Pair("Documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath))
      commonFolders.add(Pair("Downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath))
      commonFolders.add(Pair("Pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath))
      commonFolders.add(Pair("Music", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath))
      commonFolders.add(Pair("DCIM", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath))
      commonFolders.add(Pair("External Storage", Environment.getExternalStorageDirectory().absolutePath))
    } catch (e: Exception) {
      // Ignore folder access errors
    }

    val folderNames = commonFolders.map { "${it.first}\n${it.second}" }.toTypedArray()
    
    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
      .setTitle("Select a folder")
      .setItems(folderNames) { _, which ->
        val selectedFolder = commonFolders[which].second
        // Create an 'ftp' subfolder in the selected folder
        val ftpFolder = File(selectedFolder, "ftp")
        try {
          if (!ftpFolder.exists()) {
            ftpFolder.mkdirs()
          }
          onFolderSelected(ftpFolder.absolutePath)
          Toast.makeText(this, "Folder selected: ${ftpFolder.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
          Toast.makeText(this, "Folder access error: ${e.message}", Toast.LENGTH_LONG).show()
        }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun startFtpServerAutomatically(
    portEdit: EditText?,
    userEdit: EditText?,
    passEdit: EditText?,
    rootEdit: EditText?,
    statusText: TextView?,
    localIP: String
  ) {
    Log.i("FTP", "Attempting automatic FTP server startup...")
    
    try {
      // Use default values or those in the fields
      val port = portEdit?.text?.toString()?.toIntOrNull() ?: 2121
      val user = userEdit?.text?.toString()?.ifBlank { "admin" } ?: "admin"
      val pass = passEdit?.text?.toString()?.ifBlank { "password" } ?: "password"
      val rootPath = rootEdit?.text?.toString()?.ifBlank { 
        File(applicationContext.filesDir, "ftp").absolutePath 
      } ?: File(applicationContext.filesDir, "ftp").absolutePath

      Log.i("FTP", "Configuration: port=$port, user=$user, rootPath=$rootPath")

      val rootDir = File(rootPath)
      if (!rootDir.exists()) {
        Log.i("FTP", "Creating root folder: ${rootDir.absolutePath}")
        val created = rootDir.mkdirs()
        Log.i("FTP", "Folder created: $created")
        if (created) {
          // Create some test files
          createTestFiles(rootDir)
        }
      } else {
        Log.i("FTP", "Root folder already exists: ${rootDir.absolutePath}")
      }

      // Stop previous server if it exists
      if (ftpServer != null) {
        Log.i("FTP", "Stopping previous server...")
        ftpServer?.stop()
        ftpServer = null
      }
      
      // Start the new server
      Log.i("FTP", "Creating and starting new server...")
      ftpServer = SimpleFtpServer(port, user, pass, rootDir)
      ftpServer?.start()
      
      // Ensure WakeLock is active to maintain the server
      if (wakeLock?.isHeld != true) {
        setupWakeLock()
      }
      
      statusText?.text = getString(R.string.server_auto_started, localIP, port)
      statusText?.setTextColor(0xFF4CAF50.toInt()) // Green for success
      
      Log.i("FTP", "FTP server auto-started successfully on $localIP:$port")
      Toast.makeText(this, getString(R.string.server_auto_started, localIP, port), Toast.LENGTH_LONG).show()
      
    } catch (e: Exception) {
      Log.e("FTP", "Error during FTP server auto-start", e)
      statusText?.text = getString(R.string.server_start_error, e.message ?: "Unknown")
      statusText?.setTextColor(0xFFF44336.toInt()) // Red for error
      Toast.makeText(this, getString(R.string.server_start_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
    }
  }

  private fun getLocalIPAddress(): String {
    try {
      val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
      for (intf in interfaces) {
        val addrs = Collections.list(intf.inetAddresses)
        for (addr in addrs) {
          if (!addr.isLoopbackAddress) {
            val sAddr = addr.hostAddress
            if (sAddr != null && sAddr.indexOf(':') < 0) { // IPv4
              return sAddr
            }
          }
        }
      }
    } catch (e: Exception) {
      return "Unknown"
    }
    return "Unknown"
  }

  private fun setupWakeLock() {
    try {
      val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
      wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "MQFTPServer::FTPServerWakeLock"
      )
      wakeLock?.acquire(10*60*1000L /*10 minutes*/)
      Log.i("FTP", "WakeLock acquired to keep server active")
    } catch (e: Exception) {
      Log.e("FTP", "Error acquiring WakeLock", e)
    }
  }

  override fun onPause() {
    super.onPause()
    Log.i("FTP", "Activity paused - FTP server remains active")
  }

  override fun onResume() {
    super.onResume()
    Log.i("FTP", "Activity resumed")
    
    // Re-acquire WakeLock if necessary
    if (wakeLock?.isHeld != true) {
      setupWakeLock()
    }
  }

  override fun onStop() {
    super.onStop()
    Log.i("FTP", "Activity stopped - FTP server remains active in background")
  }

  override fun onRestart() {
    super.onRestart()
    Log.i("FTP", "Activity restarted")
  }

  override fun onDestroy() {
    super.onDestroy()
    
    // Stop FTP server
    ftpServer?.stop()
    
    // Release WakeLock
    try {
      wakeLock?.release()
      Log.i("FTP", "WakeLock released")
    } catch (e: Exception) {
      Log.e("FTP", "Error releasing WakeLock", e)
    }
  }

  private fun createTestFiles(rootDir: File) {
    try {
      // Create a test file
      val testFile = File(rootDir, "readme.txt")
      if (!testFile.exists()) {
        testFile.writeText("Welcome to the FTP server!\nThis file was created automatically.")
      }
      
      // Create a test folder
      val testFolder = File(rootDir, "documents")
      if (!testFolder.exists()) {
        testFolder.mkdirs()
        File(testFolder, "example.txt").writeText("Example file in the documents folder")
      }
      
      Log.i("FTP", "Test files created in ${rootDir.absolutePath}")
    } catch (e: Exception) {
      Log.e("FTP", "Error creating test files", e)
    }
  }
}
