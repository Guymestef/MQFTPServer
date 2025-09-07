package com.mqftpserver.app

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class SimpleFtpServer(
    private val port: Int,
    private val username: String,
    private val password: String,
    private val rootDirectory: File
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val clientJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "SimpleFtpServer"
        const val CRLF = "\r\n"
    }

    fun start() {
        if (isRunning) {
            Log.w(TAG, "Server is already running")
            return
        }

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Log.i(TAG, "FTP Server started on port $port")

                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        if (clientSocket != null) {
                            val clientId = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                            val job = launch { handleClient(clientSocket) }
                            clientJobs[clientId] = job
                        }
                    } catch (e: IOException) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client connection", e)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error starting server", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
            clientJobs.values.forEach { it.cancel() }
            clientJobs.clear()
            Log.i(TAG, "FTP Server stopped")
        } catch (e: IOException) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val output = PrintWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
            
            var isAuthenticated = false
            var currentUser = ""
            var currentDirectory = rootDirectory
            var dataServerSocket: ServerSocket? = null
            var passiveMode = false
            var fileToRename: File? = null // Stocke le fichier à renommer pour RNFR/RNTO

            // Send welcome message
            output.println("220 Simple FTP Server Ready")

            while (!socket.isClosed && isRunning) {
                val command = input.readLine() ?: break
                Log.d(TAG, "Received command: $command")

                val commandParts = command.trim().split(' ', limit = 2)
                val cmd = commandParts[0].uppercase()
                val args = if (commandParts.size > 1) commandParts[1] else ""

                when (cmd) {
                    "USER" -> {
                        currentUser = args
                        output.println("331 Password required for $args")
                    }
                    "PASS" -> {
                        if (currentUser == username && args == password) {
                            isAuthenticated = true
                            output.println("230 User logged in")
                        } else {
                            output.println("530 Login incorrect")
                        }
                    }
                    "PWD" -> {
                        if (isAuthenticated) {
                            val relativePath = getRelativePath(currentDirectory)
                            output.println("257 \"$relativePath\" is current directory")
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "CWD" -> {
                        if (isAuthenticated) {
                            val newDir = if (args.startsWith("/")) {
                                File(rootDirectory, args.substring(1))
                            } else {
                                File(currentDirectory, args)
                            }
                            if (newDir.exists() && newDir.isDirectory && newDir.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
                                currentDirectory = newDir
                                output.println("250 Directory changed")
                            } else {
                                output.println("550 Directory not found")
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "LIST" -> {
                        if (isAuthenticated) {
                            try {
                                output.println("150 Opening ASCII mode data connection for file list")
                                
                                if (passiveMode && dataServerSocket != null) {
                                    // Mode passif - utiliser la connexion de données
                                    val dataConnection = dataServerSocket!!.accept()
                                    val dataOutput = PrintWriter(OutputStreamWriter(dataConnection.getOutputStream(), StandardCharsets.UTF_8), true)
                                    
                                    val listing = buildFileList(currentDirectory)
                                    dataOutput.print(listing)
                                    dataOutput.flush()
                                    
                                    dataOutput.close()
                                    dataConnection.close()
                                    dataServerSocket?.close()
                                    dataServerSocket = null
                                    passiveMode = false
                                } else {
                                    // Mode actif simplifié - envoyer directement
                                    val listing = buildFileList(currentDirectory)
                                    Log.d(TAG, "Sending file list: $listing")
                                    
                                    // Envoyer ligne par ligne
                                    listing.split("\n").forEach { line ->
                                        if (line.isNotEmpty()) {
                                            output.println(line)
                                        }
                                    }
                                }
                                
                                output.println("226 Transfer complete")
                                Log.d(TAG, "LIST command completed")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in LIST command", e)
                                output.println("550 Error listing directory")
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "PASV" -> {
                        if (isAuthenticated) {
                            try {
                                dataServerSocket?.close()
                                dataServerSocket = ServerSocket(0)
                                val dataPort = dataServerSocket!!.localPort
                                passiveMode = true
                                
                                // Obtenir l'adresse IP du serveur
                                val serverAddress = socket.localAddress.hostAddress ?: "127.0.0.1"
                                val addressParts = serverAddress.split(".")
                                val p1 = dataPort / 256
                                val p2 = dataPort % 256
                                
                                val pasvResponse = "227 Entering Passive Mode (${addressParts.joinToString(",")},${p1},${p2})"
                                output.println(pasvResponse)
                                Log.d(TAG, "PASV mode enabled: $pasvResponse")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error setting up passive mode", e)
                                output.println("425 Can't open passive connection")
                                passiveMode = false
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "TYPE" -> {
                        if (isAuthenticated) {
                            output.println("200 Type set")
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "SYST" -> {
                        output.println("215 UNIX Type: L8")
                    }
                    "NOOP" -> {
                        output.println("200 OK")
                    }
                    "FEAT" -> {
                        output.println("211-Features:")
                        output.println(" UTF8")
                        output.println(" SIZE")
                        output.println(" MKD")
                        output.println(" RMD") 
                        output.println(" DELE")
                        output.println(" STOR")
                        output.println(" RNFR")
                        output.println(" RNTO")
                        output.println(" PASV")
                        output.println("211 End")
                    }
                    "OPTS" -> {
                        if (args.uppercase() == "UTF8 ON") {
                            output.println("200 UTF8 enabled")
                        } else {
                            output.println("501 Option not supported")
                        }
                    }
                    "SIZE" -> {
                        if (isAuthenticated) {
                            val file = File(currentDirectory, args)
                            if (file.exists() && file.isFile) {
                                output.println("213 ${file.length()}")
                            } else {
                                output.println("550 File not found")
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "MKD", "XMKD" -> {
                        if (isAuthenticated) {
                            try {
                                val newDir = if (args.startsWith("/")) {
                                    File(rootDirectory, args.substring(1))
                                } else {
                                    File(currentDirectory, args)
                                }
                                
                                // Vérifier que le nouveau dossier sera dans la racine autorisée
                                if (!newDir.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
                                    output.println("550 Access denied")
                                    Log.w(TAG, "MKD attempt outside root directory: ${newDir.canonicalPath}")
                                } else if (newDir.exists()) {
                                    output.println("550 Directory already exists")
                                    Log.d(TAG, "MKD failed - directory exists: ${newDir.name}")
                                } else if (newDir.mkdirs()) {
                                    val relativePath = getRelativePath(newDir)
                                    output.println("257 \"$relativePath\" directory created")
                                    Log.i(TAG, "Directory created: ${newDir.absolutePath}")
                                } else {
                                    output.println("550 Create directory operation failed")
                                    Log.e(TAG, "Failed to create directory: ${newDir.absolutePath}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in MKD command", e)
                                output.println("550 Create directory operation failed")
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "RMD", "XRMD" -> {
                        if (isAuthenticated) {
                            try {
                                val dirToRemove = if (args.startsWith("/")) {
                                    File(rootDirectory, args.substring(1))
                                } else {
                                    File(currentDirectory, args)
                                }
                                
                                // Vérifier que le dossier est dans la racine autorisée
                                if (!dirToRemove.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
                                    output.println("550 Access denied")
                                    Log.w(TAG, "RMD attempt outside root directory: ${dirToRemove.canonicalPath}")
                                } else if (!dirToRemove.exists()) {
                                    output.println("550 Directory not found")
                                } else if (!dirToRemove.isDirectory) {
                                    output.println("550 Not a directory")
                                } else if (dirToRemove.listFiles()?.isNotEmpty() == true) {
                                    output.println("550 Directory not empty")
                                } else if (dirToRemove.delete()) {
                                    output.println("250 Directory removed")
                                    Log.i(TAG, "Directory removed: ${dirToRemove.absolutePath}")
                                } else {
                                    output.println("550 Remove directory operation failed")
                                    Log.e(TAG, "Failed to remove directory: ${dirToRemove.absolutePath}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in RMD command", e)
                                output.println("550 Remove directory operation failed")
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "DELE" -> {
                        if (isAuthenticated) {
                            try {
                                val fileToDelete = if (args.startsWith("/")) {
                                    File(rootDirectory, args.substring(1))
                                } else {
                                    File(currentDirectory, args)
                                }
                                
                                // Vérifier que le fichier est dans la racine autorisée
                                if (!fileToDelete.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
                                    output.println("550 Access denied")
                                    Log.w(TAG, "DELE attempt outside root directory: ${fileToDelete.canonicalPath}")
                                } else if (!fileToDelete.exists()) {
                                    output.println("550 File not found")
                                } else if (fileToDelete.isDirectory) {
                                    output.println("550 Cannot delete directory with DELE, use RMD")
                                } else if (fileToDelete.delete()) {
                                    output.println("250 File deleted")
                                    Log.i(TAG, "File deleted: ${fileToDelete.absolutePath}")
                                } else {
                                    output.println("550 Delete operation failed")
                                    Log.e(TAG, "Failed to delete file: ${fileToDelete.absolutePath}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in DELE command", e)
                                output.println("550 Delete operation failed")
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "STOR" -> {
                        if (isAuthenticated) {
                            try {
                                val targetFile = if (args.startsWith("/")) {
                                    File(rootDirectory, args.substring(1))
                                } else {
                                    File(currentDirectory, args)
                                }
                                
                                // Vérifier que le fichier sera dans la racine autorisée
                                if (!targetFile.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
                                    output.println("550 Access denied")
                                    Log.w(TAG, "STOR attempt outside root directory: ${targetFile.canonicalPath}")
                                } else {
                                    // Créer le répertoire parent si nécessaire
                                    targetFile.parentFile?.mkdirs()
                                    
                                    output.println("150 Opening BINARY mode data connection for ${args}")
                                    
                                    if (passiveMode && dataServerSocket != null) {
                                        // Mode passif - recevoir le fichier via la connexion de données
                                        val dataConnection = dataServerSocket!!.accept()
                                        val dataInput = dataConnection.getInputStream()
                                        
                                        try {
                                            val fileOutput = FileOutputStream(targetFile)
                                            val buffer = ByteArray(8192)
                                            var bytesRead: Int
                                            var totalBytes = 0L
                                            
                                            while (dataInput.read(buffer).also { bytesRead = it } != -1) {
                                                fileOutput.write(buffer, 0, bytesRead)
                                                totalBytes += bytesRead
                                            }
                                            
                                            fileOutput.flush()
                                            fileOutput.close()
                                            
                                            Log.i(TAG, "File uploaded: ${targetFile.absolutePath} (${totalBytes} bytes)")
                                            output.println("226 Transfer complete")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error during file upload", e)
                                            output.println("550 Transfer failed")
                                            targetFile.delete() // Nettoyer le fichier partiellement uploadé
                                        } finally {
                                            dataInput.close()
                                            dataConnection.close()
                                            dataServerSocket?.close()
                                            dataServerSocket = null
                                            passiveMode = false
                                        }
                                    } else {
                                        output.println("425 Use PASV first")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in STOR command", e)
                                output.println("550 Store operation failed")
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "RNFR" -> {
                        if (isAuthenticated) {
                            try {
                                val sourceFile = if (args.startsWith("/")) {
                                    File(rootDirectory, args.substring(1))
                                } else {
                                    File(currentDirectory, args)
                                }
                                
                                // Vérifier que le fichier source est dans la racine autorisée
                                if (!sourceFile.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
                                    output.println("550 Access denied")
                                    Log.w(TAG, "RNFR attempt outside root directory: ${sourceFile.canonicalPath}")
                                } else if (!sourceFile.exists()) {
                                    output.println("550 File not found")
                                    fileToRename = null
                                } else {
                                    fileToRename = sourceFile
                                    output.println("350 Ready for RNTO")
                                    Log.i(TAG, "RNFR: File marked for rename: ${sourceFile.absolutePath}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in RNFR command", e)
                                output.println("550 RNFR operation failed")
                                fileToRename = null
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "RNTO" -> {
                        if (isAuthenticated) {
                            if (fileToRename == null) {
                                output.println("503 Bad sequence of commands (use RNFR first)")
                            } else {
                                try {
                                    val targetFile = if (args.startsWith("/")) {
                                        File(rootDirectory, args.substring(1))
                                    } else {
                                        File(currentDirectory, args)
                                    }
                                    
                                    // Vérifier que le fichier cible sera dans la racine autorisée
                                    if (!targetFile.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
                                        output.println("550 Access denied")
                                        Log.w(TAG, "RNTO attempt outside root directory: ${targetFile.canonicalPath}")
                                    } else if (targetFile.exists()) {
                                        output.println("550 Target file already exists")
                                    } else {
                                        // Créer le dossier parent si nécessaire
                                        targetFile.parentFile?.mkdirs()
                                        
                                        // Effectuer le renommage
                                        if (fileToRename!!.renameTo(targetFile)) {
                                            output.println("250 File renamed successfully")
                                            Log.i(TAG, "File renamed: ${fileToRename!!.absolutePath} -> ${targetFile.absolutePath}")
                                        } else {
                                            output.println("550 Rename operation failed")
                                            Log.e(TAG, "Failed to rename: ${fileToRename!!.absolutePath} -> ${targetFile.absolutePath}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in RNTO command", e)
                                    output.println("550 Rename operation failed")
                                } finally {
                                    fileToRename = null // Reset après l'opération
                                }
                            }
                        } else {
                            output.println("530 Not logged in")
                        }
                    }
                    "QUIT" -> {
                        output.println("221 Goodbye")
                        break
                    }
                    else -> {
                        output.println("502 Command not implemented")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        } finally {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing client socket", e)
            }
        }
    }

    private suspend fun handleListCommand(dataServerSocket: ServerSocket, directory: File) {
        try {
            withTimeout(5000) { // 5 seconds timeout
                val dataConnection = dataServerSocket.accept()
                val dataOutput = PrintWriter(dataConnection.getOutputStream(), true)
                
                val listing = buildFileList(directory)
                dataOutput.print(listing)
                dataOutput.flush()
                
                dataOutput.close()
                dataConnection.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in LIST command", e)
        }
    }

    private fun buildFileList(directory: File): String {
        return buildString {
            try {
                Log.d(TAG, "Building file list for: ${directory.absolutePath}")
                val files = directory.listFiles()
                
                if (files == null || files.isEmpty()) {
                    Log.d(TAG, "Directory is empty or null")
                    return@buildString
                }
                
                files.sortedWith(compareBy({ !it.isDirectory }, { it.name })).forEach { file ->
                    try {
                        val permissions = if (file.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                        val size = if (file.isDirectory) "4096" else file.length().toString().padStart(8)
                        val name = file.name
                        val date = "Jan 01 12:00"
                        val line = "$permissions 1 owner group $size $date $name"
                        Log.d(TAG, "File entry: $line")
                        appendLine(line)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing file: ${file.name}", e)
                    }
                }
                Log.d(TAG, "File list built successfully, ${files.size} items")
            } catch (e: Exception) {
                Log.e(TAG, "Error building file list", e)
            }
        }
    }

    private fun getRelativePath(directory: File): String {
        val rootPath = rootDirectory.canonicalPath
        val currentPath = directory.canonicalPath
        return if (currentPath.startsWith(rootPath)) {
            val relativePath = currentPath.substring(rootPath.length)
            if (relativePath.isEmpty()) "/" else relativePath.replace(File.separator, "/")
        } else {
            "/"
        }
    }
}
