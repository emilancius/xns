package org.emgen.io

import java.io.File
import java.math.BigDecimal
import java.math.MathContext

object FSys {

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

        val target = File("${this.parent}${File.separator}$name")

        if (target.exists()) {
            throw IllegalArgumentException("Cannot rename to \"$name\" - \"${target.toPath()}\" exists")
        }

        if (this.renameTo(target)) {
            return target
        } else {
            throw RuntimeException("\"${this.toPath()}\" could not be renamed to \"${target}\"")
        }
    }
}