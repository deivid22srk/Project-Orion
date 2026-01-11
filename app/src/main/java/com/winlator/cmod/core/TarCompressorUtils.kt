package com.winlator.cmod.core

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object TarCompressorUtils {
    
    enum class Type { XZ, ZSTD }
    
    interface OnExtractFileListener {
        fun onExtractFile(file: File, size: Long): File?
    }
    
    fun extract(
        type: Type,
        context: Context,
        assetFile: String,
        destination: File,
        listener: OnExtractFileListener? = null
    ): Boolean {
        return try {
            context.assets.open(assetFile).use { inputStream ->
                val compressorStream = when (type) {
                    Type.XZ -> XZCompressorInputStream(BufferedInputStream(inputStream))
                    Type.ZSTD -> ZstdCompressorInputStream(BufferedInputStream(inputStream))
                }
                
                extractTar(compressorStream, destination, listener)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun extract(
        type: Type,
        sourceFile: File,
        destination: File,
        listener: OnExtractFileListener? = null
    ): Boolean {
        return try {
            FileInputStream(sourceFile).use { inputStream ->
                val compressorStream = when (type) {
                    Type.XZ -> XZCompressorInputStream(BufferedInputStream(inputStream))
                    Type.ZSTD -> ZstdCompressorInputStream(BufferedInputStream(inputStream))
                }
                
                extractTar(compressorStream, destination, listener)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun extractTar(
        inputStream: java.io.InputStream,
        destination: File,
        listener: OnExtractFileListener?
    ) {
        TarArchiveInputStream(inputStream).use { tar ->
            var entry: TarArchiveEntry? = tar.nextTarEntry
            
            while (entry != null) {
                var outputFile = File(destination, entry.name)
                
                if (listener != null) {
                    val result = listener.onExtractFile(outputFile, entry.size)
                    if (result == null) {
                        entry = tar.nextTarEntry
                        continue
                    }
                    outputFile = result
                }
                
                when {
                    entry.isSymbolicLink -> {
                        FileUtils.symlink(entry.linkName, outputFile.absolutePath)
                    }
                    entry.isDirectory -> {
                        outputFile.mkdirs()
                    }
                    else -> {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { output ->
                            tar.copyTo(output)
                        }
                        
                        if (entry.mode and 0x40 != 0) {
                            FileUtils.chmod(outputFile, 0x1ed)
                        }
                    }
                }
                
                entry = tar.nextTarEntry
            }
        }
    }
}
