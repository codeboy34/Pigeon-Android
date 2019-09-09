package com.pigeonmessenger.utils.backup

import android.Manifest
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.StatFs
import androidx.annotation.RequiresPermission
import com.pigeonmessenger.database.room.dbs.MessageRoomDatabase
import com.pigeonmessenger.extension.getBackupPath
import com.pigeonmessenger.utils.Constant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

private const val BACKUP_POSTFIX = ".backup"

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun backup(
    context: Context,
    callback: (Result) -> Unit
) {
    val dbFile = context.getDatabasePath(Constant.DataBase.DB_NAME)
        ?: return callback(Result.NOT_FOUND)
    GlobalScope.launch {
        val backupDir = context.getBackupPath()

        val availableSize = StatFs(backupDir.path).availableBytes
        if (availableSize < dbFile.length()) {
            withContext(Dispatchers.Main) {
                callback(Result.NO_AVAILABLE_MEMORY)
            }
            return@launch
        }

        val exists = backupDir.listFiles().any { it.name.contains(dbFile.name) }
        val name = "${dbFile.name}.${Constant.DataBase.CURRENT_VERSION}"
        val tmpName = if (exists) {
            "$name$BACKUP_POSTFIX"
        } else {
            name
        }
        val copyPath = "$backupDir${File.separator}$tmpName"
        MessageRoomDatabase.checkPoint()
        return@launch try {
            val result = dbFile.copyTo(File(copyPath))

            backupDir.listFiles().forEach { f ->
                if (f.name != tmpName) {
                    f.delete()
                }
            }
            if (tmpName.contains(BACKUP_POSTFIX)) {
                result.renameTo(File("$backupDir${File.separator}$name"))
            }

            val db = SQLiteDatabase.openDatabase("$backupDir${File.separator}$name", null, SQLiteDatabase.OPEN_READWRITE)
            db.execSQL("DELETE FROM sent_sender_keys")
            db.close()

            withContext(Dispatchers.Main) {
                callback(Result.SUCCESS)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback(Result.FAILURE)
            }
        }
    }
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun restore(
    context: Context,
    callback: (Result) -> Unit
) {
    GlobalScope.launch {
        val target = findBackup(context, coroutineContext)
            ?: return@launch callback(Result.NOT_FOUND)
        val file = context.getDatabasePath(Constant.DataBase.DB_NAME)
        try {
            if (file.exists()) {
                file.delete()
            }
            File("${file.absolutePath}-wal").delete()
            File("${file.absolutePath}-shm").delete()
            target.copyTo(file)

            withContext(Dispatchers.Main) {
                callback(Result.SUCCESS)
            }
        } catch (e: Exception) {
            callback(Result.FAILURE)
        }
    }
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun delete(
    context: Context
): Boolean {
    return GlobalScope.async {
        val backupDir = context.getBackupPath()
        return@async backupDir.deleteRecursively()
    }.await()
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun findBackup(
    context: Context,
    coroutineContext: CoroutineContext
): File? {
    return GlobalScope.async(coroutineContext) {
        val backupDir = context.getBackupPath()
        if (!backupDir.exists() || !backupDir.isDirectory) return@async null
        val files = backupDir.listFiles()
        if (files.isNullOrEmpty()) return@async null
        files.forEach { f ->
            val name = f.name
            val exists = try {
                val version = name.split('.')[2].toInt()
                version in Constant.DataBase.MINI_VERSION..Constant.DataBase.CURRENT_VERSION
            } catch (e: Exception) {
                false
            }
            if (exists) return@async f
        }
        null
    }.await()
}