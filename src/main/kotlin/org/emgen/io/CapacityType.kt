package org.emgen.io

enum class CapacityType(
    val bytesCount: Long = 1L
) {
    BYTE,
    KILOBYTE(1024L),
    MEGABYTE(KILOBYTE.bytesCount * 1024L),
    GIGABYTE(MEGABYTE.bytesCount * 1024L),
    TERABYTE(GIGABYTE.bytesCount * 1024L),
    PETABYTE(TERABYTE.bytesCount * 1024L)
}
