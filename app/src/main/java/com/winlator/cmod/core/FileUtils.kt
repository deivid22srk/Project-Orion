package com.winlator.cmod.core

import android.content.Context
import android.system.Os
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files

object FileUtils {
    
    fun read(context: Context, assetFile: String): ByteArray? {
        return try {
            context.assets.open(assetFile).use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
    
    fun read(file: File): ByteArray? {
        return try {
            BufferedInputStream(FileInputStream(file)).use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
    
    fun readString(context: Context, assetFile: String): String {
        return read(context, assetFile)?.toString(StandardCharsets.UTF_8) ?: ""
    }
    
    fun readString(file: File): String {
        return read(file)?.toString(StandardCharsets.UTF_8) ?: ""
    }
    
    fun writeString(file: File, data: String): Boolean {
        return try {
            BufferedWriter(FileWriter(file)).use {
                it.write(data)
                it.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun readLines(file: File): ArrayList<String> {
        val lines = ArrayList<String>()
        if (!file.exists()) return lines
        
        return try {
            file.readLines() as ArrayList<String>
        } catch (e: Exception) {
            lines
        }
    }
    
    fun symlink(linkTarget: String, linkFile: String) {
        try {
            File(linkFile).delete()
            Os.symlink(linkTarget, linkFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun isSymlink(file: File): Boolean {
        return Files.isSymbolicLink(file.toPath())
    }
    
    fun delete(targetFile: File?): Boolean {
        if (targetFile == null) return false
        if (targetFile.isDirectory) {
            if (!isSymlink(targetFile)) {
                if (!clear(targetFile)) return false
            }
        }
        return targetFile.delete()
    }
    
    fun clear(targetFile: File?): Boolean {
        if (targetFile == null) return false
        if (targetFile.isDirectory) {
            targetFile.listFiles()?.forEach { file ->
                if (!delete(file)) return false
            }
        }
        return true
    }
    
    fun copy(src: File, dst: File): Boolean {
        return try {
            if (src.isDirectory) {
                dst.mkdirs()
                src.listFiles()?.forEach { file ->
                    copy(file, File(dst, file.name))
                }
            } else {
                src.copyTo(dst, overwrite = true)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun copy(src: File, dst: File, onCopy: ((File) -> Unit)?): Boolean {
        return try {
            if (src.isDirectory) {
                dst.mkdirs()
                src.listFiles()?.forEach { file ->
                    val dstFile = File(dst, file.name)
                    copy(file, dstFile, onCopy)
                    onCopy?.invoke(dstFile)
                }
            } else {
                src.copyTo(dst, overwrite = true)
                onCopy?.invoke(dst)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun chmod(file: File, mode: Int): Boolean {
        return try {
            Os.chmod(file.absolutePath, mode)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getBasename(path: String): String {
        val lastDot = path.lastIndexOf(".")
        val lastSlash = path.lastIndexOf("/")
        return if (lastDot > lastSlash) {
            path.substring(lastSlash + 1, lastDot)
        } else {
            path.substring(lastSlash + 1)
        }
    }
    
    fun getSize(context: Context, assetFile: String): Long {
        return try {
            context.assets.openFd(assetFile).use { it.length }
        } catch (e: Exception) {
            0
        }
    }
    
    fun readSymlink(file: File): String {
        return try {
            Files.readSymbolicLink(file.toPath()).toString()
        } catch (e: Exception) {
            ""
        }
    }
    
    fun saveBitmapToFile(bitmap: android.graphics.Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
