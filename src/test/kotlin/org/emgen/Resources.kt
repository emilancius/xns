package org.emgen

import java.io.File
import java.nio.file.Files

class Resources {

    companion object {
        private val FILE_PATH_SEPARATOR = File.separator
    }

    fun createResourceURI(vararg parts: String): String = parts.joinToString(FILE_PATH_SEPARATOR, FILE_PATH_SEPARATOR)

    fun createResource(path: String, directory: Boolean = true): File {
        val resources = getResource("")
        val resource = File("${resources.toPath()}$FILE_PATH_SEPARATOR$path")

        if (resource.exists()) {
            // resource exists, no need to create
            return resource
        }

        if (directory) {
            Files.createDirectories(resource.toPath())
        } else {
            val parent = resource.toPath().parent

            if (!Files.exists(parent)) {
                Files.createDirectories(parent)
            }

            Files.createFile(resource.toPath())
        }

        if (!resource.exists()) {
            throw RuntimeException("\"${resource.toPath()}\" could not be created")
        }

        return resource
    }

    fun getResource(path: String): File {
        val resource = Resources::class.java.classLoader.getResource(path)?.toURI()
            ?: throw RuntimeException("Resource \"$path\" could not be found")
        return File(resource)
    }
}