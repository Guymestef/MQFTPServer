package com.mqftpserver.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ArrayAdapter
import java.io.File
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

// default activity
class PancakeActivity : Activity() {
  private var ftpServer: SimpleFtpServer? = null
  private var wakeLock: PowerManager.WakeLock? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setTheme(R.style.PanelAppThemeTransparent)
    setContentView(R.layout.ui_pancake_modern)

    // Empêcher la mise en veille pour maintenir le serveur FTP actif
    setupWakeLock()
    
    // Garder l'écran allumé
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
    val btnListFiles = findViewById<Button>(R.id.btn_list_files)
    val btnRefreshFiles = findViewById<Button>(R.id.btn_refresh_files)

    // Définir le dossier racine par défaut vers Videos
    val defaultFtpRoot = try {
      val videosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
      if (videosDir.exists() || videosDir.mkdirs()) {
        File(videosDir, "ftp").absolutePath
      } else {
        // Fallback vers le dossier interne si Videos n'est pas accessible
        File(applicationContext.filesDir, "ftp").absolutePath
      }
    } catch (e: Exception) {
      Log.e("FTP", "Erreur lors de l'accès au dossier Videos", e)
      File(applicationContext.filesDir, "ftp").absolutePath
    }
    
    rootEdit?.setText(defaultFtpRoot)

    // Pré-remplir les champs avec les valeurs par défaut
    portEdit?.setText("2121")
    userEdit?.setText("admin")
    passEdit?.setText("password")

    // Afficher l'adresse IP locale
    val localIP = getLocalIPAddress()
    ipText?.text = "IP locale: $localIP"

    // Démarrer automatiquement le serveur FTP après un court délai
    // pour s'assurer que l'interface est complètement initialisée
    Handler(Looper.getMainLooper()).postDelayed({
      startFtpServerAutomatically(portEdit, userEdit, passEdit, rootEdit, statusText, localIP)
    }, 1000) // Délai de 1 seconde

    btnBrowse?.setOnClickListener {
      showSimpleFolderPicker { selectedPath ->
        rootEdit?.setText(selectedPath)
      }
    }

    btnListFiles?.setOnClickListener {
      val rootPath = rootEdit?.text?.toString() ?: File(applicationContext.filesDir, "ftp").absolutePath
      showFileExplorer(File(rootPath))
    }

    btnRefreshFiles?.setOnClickListener {
      Toast.makeText(this, "Fichiers actualisés", Toast.LENGTH_SHORT).show()
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
          // Créer quelques fichiers de test
          createTestFiles(rootDir)
        }

        ftpServer = SimpleFtpServer(port, user, pass, rootDir)
        ftpServer?.start()
        
        // S'assurer que le WakeLock est actif pour maintenir le serveur
        if (wakeLock?.isHeld != true) {
          setupWakeLock()
        }
        
        statusText?.text = "✅ Serveur actif sur $localIP:$port"
        statusText?.setTextColor(0xFF4CAF50.toInt()) // Vert pour succès
        Toast.makeText(this, "Serveur FTP démarré sur $localIP:$port", Toast.LENGTH_LONG).show()
      } catch (e: Exception) {
        Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }

    btnStop?.setOnClickListener {
      ftpServer?.stop()
      ftpServer = null
      statusText?.text = "⏹️ Serveur arrêté"
      statusText?.setTextColor(0xFFF44336.toInt()) // Rouge pour arrêt
      Toast.makeText(this, "Serveur FTP arrêté", Toast.LENGTH_SHORT).show()
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
          
          // Ajouter l'option de remonter si possible
          currentDirectory.parent?.let { parent ->
            folders.add(Pair("⬆️ Dossier parent", File(parent)))
          }
          
          // Ajouter les dossiers du répertoire actuel
          currentDirectory.listFiles()?.let { files ->
            files.filter { it.isDirectory && it.canRead() }
              .sortedBy { it.name.lowercase() }
              .forEach { folder ->
                val displayName = "📁 ${folder.name}"
                folders.add(Pair(displayName, folder))
              }
          }
          
          // Ajouter l'option de sélectionner le dossier actuel
          folders.add(0, Pair("✅ Sélectionner ce dossier: ${currentDirectory.name}", currentDirectory))
          
          val folderNames = folders.map { it.first }.toTypedArray()
          
          AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
            .setTitle("📁 Navigateur de dossiers\n📍 ${currentDirectory.absolutePath}")
            .setItems(folderNames) { _, which ->
              val selectedFolder = folders[which].second
              if (which == 0) {
                // Sélectionner le dossier actuel
                onFolderSelected(currentDirectory.absolutePath)
                Toast.makeText(this, "Dossier sélectionné: ${currentDirectory.absolutePath}", Toast.LENGTH_SHORT).show()
              } else {
                // Naviguer vers le dossier sélectionné
                currentDirectory = selectedFolder
                showFolderDialog() // Récursion pour continuer la navigation
              }
            }
            .setNeutralButton("📁 Nouveau dossier") { _, _ ->
              showCreateFolderDialog(currentDirectory) {
                showFolderDialog() // Rafraîchir après création
              }
            }
            .setNegativeButton("❌ Annuler", null)
            .show()
        } catch (e: Exception) {
          Log.e("FTP", "Erreur dans showFolderDialog", e)
          Toast.makeText(this, "Erreur d'accès au dossier: ${e.message}", Toast.LENGTH_LONG).show()
        }
      }
      
      showFolderDialog()
    } catch (e: Exception) {
      Log.e("FTP", "Erreur dans showSimpleFolderPicker", e)
      Toast.makeText(this, "Erreur du sélecteur de dossier: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }

  private fun showCreateFolderDialog(parentDirectory: File, onSuccess: () -> Unit) {
    try {
      val input = EditText(this)
      input.hint = "Nom du nouveau dossier"
      input.setPadding(50, 30, 50, 30)
      
      AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
        .setTitle("📁 Créer un nouveau dossier")
        .setMessage("Dans: ${parentDirectory.absolutePath}")
        .setView(input)
        .setPositiveButton("✅ Créer") { _, _ ->
          val folderName = input.text.toString().trim()
          if (folderName.isNotEmpty()) {
            try {
              val newFolder = File(parentDirectory, folderName)
              if (newFolder.mkdir()) {
                onSuccess()
                Toast.makeText(this, "✅ Dossier créé: $folderName", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(this, "❌ Impossible de créer le dossier", Toast.LENGTH_SHORT).show()
              }
            } catch (e: Exception) {
              Log.e("FTP", "Erreur création dossier", e)
              Toast.makeText(this, "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
          } else {
            Toast.makeText(this, "⚠️ Veuillez entrer un nom", Toast.LENGTH_SHORT).show()
          }
        }
        .setNegativeButton("❌ Annuler", null)
        .show()
    } catch (e: Exception) {
      Log.e("FTP", "Erreur dans showCreateFolderDialog", e)
      Toast.makeText(this, "Erreur du dialog: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }



  private fun showFolderSelector(onFolderSelected: (String) -> Unit) {
    val commonFolders = mutableListOf<Pair<String, String>>()
    
    // Ajouter Videos en premier (dossier par défaut)
    try {
      val videosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
      commonFolders.add(Pair("📹 Videos (par défaut)", videosDir.absolutePath))
    } catch (e: Exception) {
      Log.e("FTP", "Erreur d'accès au dossier Videos", e)
    }
    
    // Ajouter les autres dossiers disponibles
    commonFolders.add(Pair("App interne", applicationContext.filesDir.absolutePath))
    
    val externalFilesDir = applicationContext.getExternalFilesDir(null)
    if (externalFilesDir != null) {
      commonFolders.add(Pair("App externe", externalFilesDir.absolutePath))
    }
    
    try {
      commonFolders.add(Pair("Documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath))
      commonFolders.add(Pair("Downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath))
      commonFolders.add(Pair("Pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath))
      commonFolders.add(Pair("Music", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath))
      commonFolders.add(Pair("DCIM", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath))
      commonFolders.add(Pair("Stockage externe", Environment.getExternalStorageDirectory().absolutePath))
    } catch (e: Exception) {
      // Ignorer les erreurs d'accès aux dossiers
    }

    val folderNames = commonFolders.map { "${it.first}\n${it.second}" }.toTypedArray()
    
    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
      .setTitle("Sélectionner un dossier")
      .setItems(folderNames) { _, which ->
        val selectedFolder = commonFolders[which].second
        // Créer un sous-dossier 'ftp' dans le dossier sélectionné
        val ftpFolder = File(selectedFolder, "ftp")
        try {
          if (!ftpFolder.exists()) {
            ftpFolder.mkdirs()
          }
          onFolderSelected(ftpFolder.absolutePath)
          Toast.makeText(this, "Dossier sélectionné: ${ftpFolder.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
          Toast.makeText(this, "Erreur d'accès au dossier: ${e.message}", Toast.LENGTH_LONG).show()
        }
      }
      .setNegativeButton("Annuler", null)
      .show()
  }

  private fun showFileExplorer(directory: File) {
    try {
      if (!directory.exists()) {
        Toast.makeText(this, "Le dossier n'existe pas", Toast.LENGTH_SHORT).show()
        return
      }

      val files = directory.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
      
      if (files.isEmpty()) {
        Toast.makeText(this, "Dossier vide", Toast.LENGTH_SHORT).show()
        return
      }

      val fileItems = mutableListOf<String>()
      
      // Ajouter le parent directory si ce n'est pas la racine
      val rootPath = File(findViewById<EditText>(R.id.ftp_root)?.text?.toString() ?: "").absolutePath
      if (directory.absolutePath != rootPath) {
        fileItems.add("📁 .. (Dossier parent)")
      }
      
      files.forEach { file ->
        val icon = if (file.isDirectory) "📁" else "📄"
        val size = if (file.isDirectory) "" else " (${formatFileSize(file.length())})"
        fileItems.add("$icon ${file.name}$size")
      }

      AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
        .setTitle("Fichiers dans: ${getRelativePathForDisplay(directory)}")
        .setItems(fileItems.toTypedArray()) { _, which ->
          var actualIndex = which
          
          // Si on a ajouté le parent directory, ajuster l'index
          if (directory.absolutePath != rootPath) {
            if (which == 0) {
              // Retour au dossier parent
              showFileExplorer(directory.parentFile ?: directory)
              return@setItems
            }
            actualIndex = which - 1
          }
          
          val selectedFile = files[actualIndex]
          if (selectedFile.isDirectory) {
            showFileExplorer(selectedFile)
          } else {
            showFileInfo(selectedFile)
          }
        }
        .setNegativeButton("Fermer", null)
        .setNeutralButton("Créer dossier") { _, _ ->
          showCreateFolderDialog(directory)
        }
        .show()
    } catch (e: Exception) {
      Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }

  private fun showFileInfo(file: File) {
    val info = buildString {
      appendLine("Nom: ${file.name}")
      appendLine("Taille: ${formatFileSize(file.length())}")
      appendLine("Modifié: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(file.lastModified())}")
      appendLine("Chemin: ${file.absolutePath}")
      appendLine("Lecture: ${if (file.canRead()) "Oui" else "Non"}")
      appendLine("Écriture: ${if (file.canWrite()) "Oui" else "Non"}")
    }

    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
      .setTitle("Informations du fichier")
      .setMessage(info)
      .setPositiveButton("OK", null)
      .setNegativeButton("Supprimer") { _, _ ->
        if (file.delete()) {
          Toast.makeText(this, "Fichier supprimé", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(this, "Impossible de supprimer le fichier", Toast.LENGTH_SHORT).show()
        }
      }
      .show()
  }

  private fun showCreateFolderDialog(parentDir: File) {
    val input = EditText(this)
    input.hint = "Nom du nouveau dossier"
    input.setTextColor(0xFF000000.toInt())

    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
      .setTitle("Créer un nouveau dossier")
      .setView(input)
      .setPositiveButton("Créer") { _, _ ->
        val folderName = input.text.toString().trim()
        if (folderName.isNotEmpty()) {
          val newFolder = File(parentDir, folderName)
          if (newFolder.mkdirs()) {
            Toast.makeText(this, "Dossier créé: $folderName", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(this, "Impossible de créer le dossier", Toast.LENGTH_SHORT).show()
          }
        }
      }
      .setNegativeButton("Annuler", null)
      .show()
  }

  private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
  }

  private fun getRelativePathForDisplay(directory: File): String {
    val rootPath = File(findViewById<EditText>(R.id.ftp_root)?.text?.toString() ?: "").absolutePath
    val currentPath = directory.absolutePath
    return if (currentPath.startsWith(rootPath)) {
      val relativePath = currentPath.substring(rootPath.length)
      if (relativePath.isEmpty()) "/" else relativePath.replace(File.separator, "/")
    } else {
      currentPath
    }
  }

  private fun startFtpServerAutomatically(
    portEdit: EditText?,
    userEdit: EditText?,
    passEdit: EditText?,
    rootEdit: EditText?,
    statusText: TextView?,
    localIP: String
  ) {
    Log.i("FTP", "Tentative de démarrage automatique du serveur FTP...")
    
    try {
      // Utiliser les valeurs par défaut ou celles dans les champs
      val port = portEdit?.text?.toString()?.toIntOrNull() ?: 2121
      val user = userEdit?.text?.toString()?.ifBlank { "admin" } ?: "admin"
      val pass = passEdit?.text?.toString()?.ifBlank { "password" } ?: "password"
      val rootPath = rootEdit?.text?.toString()?.ifBlank { 
        File(applicationContext.filesDir, "ftp").absolutePath 
      } ?: File(applicationContext.filesDir, "ftp").absolutePath

      Log.i("FTP", "Configuration: port=$port, user=$user, rootPath=$rootPath")

      val rootDir = File(rootPath)
      if (!rootDir.exists()) {
        Log.i("FTP", "Création du dossier racine: ${rootDir.absolutePath}")
        val created = rootDir.mkdirs()
        Log.i("FTP", "Dossier créé: $created")
        if (created) {
          // Créer quelques fichiers de test
          createTestFiles(rootDir)
        }
      } else {
        Log.i("FTP", "Dossier racine existe déjà: ${rootDir.absolutePath}")
      }

      // Arrêter le serveur précédent s'il existe
      if (ftpServer != null) {
        Log.i("FTP", "Arrêt du serveur précédent...")
        ftpServer?.stop()
        ftpServer = null
      }
      
      // Démarrer le nouveau serveur
      Log.i("FTP", "Création et démarrage du nouveau serveur...")
      ftpServer = SimpleFtpServer(port, user, pass, rootDir)
      ftpServer?.start()
      
      // S'assurer que le WakeLock est actif pour maintenir le serveur
      if (wakeLock?.isHeld != true) {
        setupWakeLock()
      }
      
      statusText?.text = "🚀 Serveur démarré automatiquement sur $localIP:$port"
      statusText?.setTextColor(0xFF4CAF50.toInt()) // Vert pour succès
      
      Log.i("FTP", "Serveur FTP démarré automatiquement avec succès sur $localIP:$port")
      Toast.makeText(this, "Serveur FTP démarré automatiquement sur $localIP:$port", Toast.LENGTH_LONG).show()
      
    } catch (e: Exception) {
      Log.e("FTP", "Erreur lors du démarrage automatique du serveur FTP", e)
      statusText?.text = "❌ Erreur de démarrage: ${e.message}"
      statusText?.setTextColor(0xFFF44336.toInt()) // Rouge pour erreur
      Toast.makeText(this, "Erreur de démarrage automatique: ${e.message}", Toast.LENGTH_LONG).show()
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
      return "Inconnue"
    }
    return "Inconnue"
  }

  private fun setupWakeLock() {
    try {
      val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
      wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "MQFTPServer::FTPServerWakeLock"
      )
      wakeLock?.acquire(10*60*1000L /*10 minutes*/)
      Log.i("FTP", "WakeLock acquis pour maintenir le serveur actif")
    } catch (e: Exception) {
      Log.e("FTP", "Erreur lors de l'acquisition du WakeLock", e)
    }
  }

  override fun onPause() {
    super.onPause()
    Log.i("FTP", "Activité en pause - serveur FTP reste actif")
  }

  override fun onResume() {
    super.onResume()
    Log.i("FTP", "Activité reprise")
    
    // Réacquérir le WakeLock si nécessaire
    if (wakeLock?.isHeld != true) {
      setupWakeLock()
    }
  }

  override fun onStop() {
    super.onStop()
    Log.i("FTP", "Activité arrêtée - serveur FTP reste actif en arrière-plan")
  }

  override fun onRestart() {
    super.onRestart()
    Log.i("FTP", "Activité redémarrée")
  }

  override fun onDestroy() {
    super.onDestroy()
    
    // Arrêter le serveur FTP
    ftpServer?.stop()
    
    // Libérer le WakeLock
    try {
      wakeLock?.release()
      Log.i("FTP", "WakeLock libéré")
    } catch (e: Exception) {
      Log.e("FTP", "Erreur lors de la libération du WakeLock", e)
    }
  }

  private fun createTestFiles(rootDir: File) {
    try {
      // Créer un fichier de test
      val testFile = File(rootDir, "readme.txt")
      if (!testFile.exists()) {
        testFile.writeText("Bienvenue sur le serveur FTP!\nCe fichier a été créé automatiquement.")
      }
      
      // Créer un dossier de test
      val testFolder = File(rootDir, "documents")
      if (!testFolder.exists()) {
        testFolder.mkdirs()
        File(testFolder, "exemple.txt").writeText("Fichier d'exemple dans le dossier documents")
      }
      
      Log.i("FTP", "Fichiers de test créés dans ${rootDir.absolutePath}")
    } catch (e: Exception) {
      Log.e("FTP", "Erreur lors de la création des fichiers de test", e)
    }
  }
}
