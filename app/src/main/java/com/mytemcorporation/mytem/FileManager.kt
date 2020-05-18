package com.mytemcorporation.mytem

import android.content.Context
import java.io.File

class FileManager
{
    companion object {

        private fun GetFile(context: Context, fileName: String) : File
        {
            val path = context.filesDir
            val directory = File(path, InternalStorageDirectory)
            val file = File(directory, "${fileName}.txt")
            return file
        }

        public fun WriteAdditiveToFile(context: Context, fileName: String, data: String, maxLineCount: Int = 4,
                                       dataUniquePredicate: (it: String) -> Boolean = { it == data}, overwriteIfEntryAlreadyExists : Boolean = true)
        {
            var lines = mutableListOf<String>()
            var indexOfExisting = -1

            val path = context.filesDir
            val directory = File(path, InternalStorageDirectory)
            if (!directory.exists())
                directory.mkdirs()

            val file = File(directory, "${fileName}.txt")
            if (file.exists())
            {
                lines = file.readLines().toMutableList()

                indexOfExisting = lines.indexOfFirst(dataUniquePredicate)

                // If data already exists in the file, either overwrite or do nothing.
                if (indexOfExisting != -1)
                {
                    if  (!overwriteIfEntryAlreadyExists)
                        return
                    else
                        lines[indexOfExisting] = data
                }
                // If data doesn't exist in the file yet, remove the first line, if neccessary.
                else
                {
                    val totalCount = lines.count() + 1
                    if (totalCount > maxLineCount)
                    {
                        lines.removeAt(0)
                    }
                }
            }

            // If data didn't already exist in the file, add the data as new line to the end of the file.
            if (indexOfExisting == -1)
                lines.add(data)

            file.bufferedWriter().use {out ->
                lines.forEach {
                    out.write(it)
                    out.newLine()
                }
            }
        }


        public fun TryReadFile(context: Context, fileName: String) : Array<String>?
        {
            val file = GetFile(context, fileName)
            if (file.exists())
                return file.readLines().toTypedArray()

            return null
        }

        public fun DeleteFile(context: Context, fileName: String)
        {
            val file = GetFile(context, fileName)

            if (file.exists())
                file.delete()
        }

        public fun TryDeleteLineFromFile(context: Context, fileName: String, data: String, lineToDeletePredicate: (it: String) -> Boolean = { it.contains(data)}) : Boolean
        {
            val file = GetFile(context, fileName)
            val lines = TryReadFile(context, fileName)!!.toMutableList()
            if (lines == null)
                return false

            val index = lines.indexOfFirst(lineToDeletePredicate)
            lines.removeAt(index)

            file.bufferedWriter().use {out ->
                lines.forEach {
                    out.write(it)
                    out.newLine()
                }
            }

            return true
        }
    }
}