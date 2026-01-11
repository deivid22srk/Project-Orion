package com.winlator.cmod.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PEIconExtractor {
    private const val IMAGE_DOS_SIGNATURE = 0x5A4D // "MZ"
    private const val IMAGE_NT_SIGNATURE = 0x00004550 // "PE\0\0"
    private const val RT_ICON = 3
    private const val RT_GROUP_ICON = 14
    
    fun extractIcon(exeFile: File): Bitmap? {
        if (!exeFile.exists() || !exeFile.canRead()) return null
        
        try {
            RandomAccessFile(exeFile, "r").use { raf ->
                // Read DOS header
                val dosSignature = readShort(raf)
                if (dosSignature != IMAGE_DOS_SIGNATURE.toShort()) return null
                
                // Skip to PE header offset (at 0x3C)
                raf.seek(0x3C)
                val peHeaderOffset = readInt(raf)
                
                // Read PE signature
                raf.seek(peHeaderOffset.toLong())
                val peSignature = readInt(raf)
                if (peSignature != IMAGE_NT_SIGNATURE) return null
                
                // Skip COFF header (20 bytes) to get to optional header
                raf.skipBytes(20)
                
                // Read optional header magic to determine if 32 or 64 bit
                val magic = readShort(raf)
                val is64Bit = magic == 0x20b.toShort()
                
                // Calculate offset to data directories
                val dataDirectoryOffset = if (is64Bit) {
                    peHeaderOffset + 24 + 112 // PE signature + COFF header + OptionalHeader64 up to DataDirectory
                } else {
                    peHeaderOffset + 24 + 96 // PE signature + COFF header + OptionalHeader32 up to DataDirectory
                }
                
                // Read resource directory RVA (index 2 in data directories)
                raf.seek(dataDirectoryOffset + (2L * 8))
                val resourceRVA = readInt(raf)
                val resourceSize = readInt(raf)
                
                if (resourceRVA == 0 || resourceSize == 0) return null
                
                // Find section containing resources
                val sectionOffset = peHeaderOffset + 24 + (if (is64Bit) 240 else 224)
                raf.seek(sectionOffset.toLong())
                
                // Read number of sections
                raf.seek(peHeaderOffset + 6L)
                val numberOfSections = readShort(raf).toInt() and 0xFFFF
                
                var resourceFileOffset = 0
                var resourceVirtualAddress = 0
                
                // Find resource section
                raf.seek(sectionOffset.toLong())
                for (i in 0 until numberOfSections) {
                    val sectionStart = sectionOffset + (i * 40)
                    raf.seek(sectionStart.toLong())
                    
                    // Skip name (8 bytes)
                    raf.skipBytes(8)
                    val virtualSize = readInt(raf)
                    val virtualAddress = readInt(raf)
                    val sizeOfRawData = readInt(raf)
                    val pointerToRawData = readInt(raf)
                    
                    if (resourceRVA >= virtualAddress && resourceRVA < virtualAddress + virtualSize) {
                        resourceFileOffset = pointerToRawData + (resourceRVA - virtualAddress)
                        resourceVirtualAddress = virtualAddress
                        break
                    }
                }
                
                if (resourceFileOffset == 0) return null
                
                // Try to extract icon data
                return extractIconFromResourceSection(raf, resourceFileOffset, resourceVirtualAddress)
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun extractIconFromResourceSection(
        raf: RandomAccessFile,
        resourceFileOffset: Int,
        resourceVirtualAddress: Int
    ): Bitmap? {
        try {
            // Navigate resource directory to find RT_GROUP_ICON
            raf.seek(resourceFileOffset.toLong())
            
            // Skip characteristics, timestamp, version (12 bytes)
            raf.skipBytes(12)
            val namedEntryCount = readShort(raf).toInt() and 0xFFFF
            val idEntryCount = readShort(raf).toInt() and 0xFFFF
            
            var groupIconDirOffset = -1
            var iconDirOffset = -1
            
            // Look for RT_GROUP_ICON and RT_ICON in resource types
            for (i in 0 until (namedEntryCount + idEntryCount)) {
                val nameId = readInt(raf)
                val offsetToData = readInt(raf)
                
                val actualId = nameId and 0xFFFF
                val isSubDirectory = (offsetToData and 0x80000000.toInt()) != 0
                val offset = offsetToData and 0x7FFFFFFF
                
                if (actualId == RT_GROUP_ICON && isSubDirectory) {
                    groupIconDirOffset = resourceFileOffset + offset
                }
                if (actualId == RT_ICON && isSubDirectory) {
                    iconDirOffset = resourceFileOffset + offset
                }
            }
            
            if (groupIconDirOffset == -1 || iconDirOffset == -1) return null
            
            // Read first group icon to get icon ID
            raf.seek(groupIconDirOffset.toLong())
            raf.skipBytes(12) // Skip directory header
            val groupNamedCount = readShort(raf).toInt() and 0xFFFF
            val groupIdCount = readShort(raf).toInt() and 0xFFFF
            
            if (groupNamedCount + groupIdCount == 0) return null
            
            // Get first group icon entry
            val groupIconId = readInt(raf)
            val groupIconOffset = readInt(raf) and 0x7FFFFFFF
            
            // Navigate to the actual icon group data
            raf.seek((resourceFileOffset + groupIconOffset).toLong())
            raf.skipBytes(12)
            val iconNamedCount = readShort(raf).toInt() and 0xFFFF
            val iconIdCount = readShort(raf).toInt() and 0xFFFF
            
            if (iconNamedCount + iconIdCount == 0) return null
            
            // Get first icon language entry
            raf.skipBytes(4) // Skip name/id
            val iconLangOffset = readInt(raf) and 0x7FFFFFFF
            
            // Read icon data location
            raf.seek((resourceFileOffset + iconLangOffset).toLong())
            raf.skipBytes(12)
            val dataNamedCount = readShort(raf).toInt() and 0xFFFF
            val dataIdCount = readShort(raf).toInt() and 0xFFFF
            
            if (dataNamedCount + dataIdCount == 0) return null
            
            raf.skipBytes(4) // Skip language id
            val dataEntryOffset = readInt(raf) and 0x7FFFFFFF
            
            // Read actual data entry
            raf.seek((resourceFileOffset + dataEntryOffset).toLong())
            val iconDataRVA = readInt(raf)
            val iconDataSize = readInt(raf)
            
            // Calculate file offset for icon data
            val iconDataOffset = resourceFileOffset + (iconDataRVA - resourceVirtualAddress)
            
            // Read icon data
            raf.seek(iconDataOffset.toLong())
            val iconData = ByteArray(iconDataSize)
            raf.readFully(iconData)
            
            // Try to decode as ICO format
            return try {
                // ICO files can be decoded directly by BitmapFactory
                BitmapFactory.decodeByteArray(iconData, 0, iconData.size)
            } catch (e: Exception) {
                // If direct decode fails, try adding ICO header
                val icoData = createIcoFile(iconData)
                BitmapFactory.decodeByteArray(icoData, 0, icoData.size)
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun createIcoFile(iconData: ByteArray): ByteArray {
        // Simple ICO header: reserved(2) + type(2) + count(2) = 6 bytes
        // + directory entry: width, height, colors, reserved, planes, bpp, size, offset = 16 bytes
        val header = ByteArray(6 + 16)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.putShort(0) // Reserved
        buffer.putShort(1) // Type (1 = ICO)
        buffer.putShort(1) // Count
        
        // Directory entry (use default values)
        buffer.put(0) // Width (0 = 256)
        buffer.put(0) // Height (0 = 256)
        buffer.put(0) // Colors
        buffer.put(0) // Reserved
        buffer.putShort(1) // Planes
        buffer.putShort(32) // BPP
        buffer.putInt(iconData.size) // Size
        buffer.putInt(22) // Offset (6 + 16)
        
        return header + iconData
    }
    
    private fun readShort(raf: RandomAccessFile): Short {
        val b1 = raf.read()
        val b2 = raf.read()
        return ((b2 shl 8) or b1).toShort()
    }
    
    private fun readInt(raf: RandomAccessFile): Int {
        val b1 = raf.read()
        val b2 = raf.read()
        val b3 = raf.read()
        val b4 = raf.read()
        return (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
    }
}
