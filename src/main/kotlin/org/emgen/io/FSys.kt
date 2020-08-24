package org.emgen.io

import java.io.File
import java.math.BigDecimal
import java.math.MathContext

object FSys {

    private val FILE_PATH_SEPARATOR = File.separator
    private const val FILE_EXTENSION_SEPARATOR = '.'
    private const val FILE_COPY_POSTFIX = "copy"

    fun File.name(extension: Boolean = true): String {
        val name = this.name
        val extensionSeparatorIndex = name.lastIndexOf('.')

        return if (extension || extensionSeparatorIndex < 1) {
            name
        } else {
            name.substring(0, extensionSeparatorIndex)
        }
    }

    fun File.contents(depth: Int = 1): List<File> {
        require(depth > 0) { "Argument's \"depth\" value cannot be less than 1" }
        require(this.exists()) { "\"${this.toPath()}\" could not be found" }
        require(this.isDirectory) { "\"${this.toPath()}\" is not a directory" }

        val contents = ArrayList<File>()

        this.listFiles()?.forEach {
            if (it.isDirectory && depth > 1) {
                it.contents(depth.dec()).forEach { c -> contents.add(c) }
            }
            contents.add(it)
        }

        return contents
    }

    fun File.size(capacityType: CapacityType = CapacityType.KILOBYTE): BigDecimal {
        require(this.exists()) { "\"${this.toPath()}\" could not be found" }

        val bytesCount =
            if (this.isDirectory) {
                this.contents(depth = Int.MAX_VALUE)
                    .map { if (it.isDirectory) 0 else it.length() }
                    .sum()
            } else {
                this.length()
            }

        return BigDecimal(bytesCount).divide(BigDecimal(capacityType.bytesCount, MathContext.DECIMAL128))
    }

    fun File.remove(): Boolean {
        require(this.exists()) { "\"${this.toPath()}\" could not be found" }

        return if (this.isDirectory) this.deleteRecursively() else this.delete()
    }

    fun File.clear(): Boolean {
        require(this.exists()) { "\"${this.toPath()}\" could not be found" }
        require(this.isDirectory) { "\"${this.toPath()}\" is not a directory" }

        return this.contents(depth = 1).all { it.remove() }
    }

    fun File.rename(name: String): File {
        require(!name.trim().isEmpty()) { "Argument \"name\" cannot be empty" }
        require(this.exists()) { "\"${this.toPath()}\" could not be found" }

        val target = File(joinToURI(this.parent, name))

        if (target.exists()) {
            throw IllegalArgumentException("Cannot rename to \"$name\" - \"${target.toPath()}\" exists")
        }

        if (this.renameTo(target)) {
            return target
        } else {
            throw RuntimeException("\"${this.toPath()}\" could not be renamed to \"${target}\"")
        }
    }

    /**
     * Copies source {@link File} to destination directory. In case it's a directory, it's contents are copied too.
     *
     * @param destination - absolute path to destination directory. In case it's not provided,
     * parent directory of source {@link File} is used.
     * @param name - copied {@link File}'s name. In case it's not provided, name is going to be generated using patterns:
     * <ul>
     *     <li>[name].[extension]</li>
     *     <li>[name] copy.[extension]</li>
     *     <li>[name] copy ([copy_index]).[extension]</li>
     * </ul>
     *
     * @return copied {@link File}
     *
     * @throws IllegalArgumentException in case source {@link File} does not exist, provided {@param destination}
     * could not be found or provided {@param name} points to existing {@link File}.
     */
    fun File.copyAs(destination: String? = null, name: String? = null): File {
        require(this.exists()) { "\"${this.toPath()}\" could not be found" }

        val destination = destination ?: this.parent
        val directory = File(destination)

        if (!directory.exists()) {
            throw IllegalArgumentException("\"$destination\" destination directory could not be found")
        }

        if (name != null && File(joinToURI(destination, name)).exists()) {
            throw IllegalArgumentException("Could not copy - \"${joinToURI(destination, name)}\" exists")
        }

        val names = directory.contents().map { it.name }
        var name = name ?: this.name

        for (n in 0..1_000_000) {
            if (name in names) {
                val copyIndex = FILE_COPY_POSTFIX + (if (n > 1) " ($n)" else "")
                name =
                    "${this.name(false)} $copyIndex${if (this.isDirectory) "" else "$FILE_EXTENSION_SEPARATOR${this.extension}"}"
            } else {
                break
            }
        }

        val target = File(joinToURI(destination, name))

        if (this.isDirectory) {
            this.copyRecursively(target)
        } else {
            this.copyTo(target)
        }

        return target
    }

    fun File.move(destination: File): File {
        require(this.exists()) { "\"${this.toPath()}\" does not exist" }
        require(destination.exists()) { "\"${destination.toPath()}\" destination directory does not exist" }

        val copy = this.copyAs(destination = destination.toPath().toString())
        this.remove()
        return copy
    }

    private fun joinToURI(vararg parts: String): String = parts
        .joinToString(FILE_PATH_SEPARATOR, FILE_PATH_SEPARATOR)
        .replace(Regex("$FILE_PATH_SEPARATOR{2,}"), FILE_PATH_SEPARATOR)
}