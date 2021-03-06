package org.emgen.io

import org.emgen.Resources
import org.emgen.io.FSys.clear
import org.emgen.io.FSys.contents
import org.emgen.io.FSys.copyAs
import org.emgen.io.FSys.create
import org.emgen.io.FSys.move
import org.emgen.io.FSys.name
import org.emgen.io.FSys.remove
import org.emgen.io.FSys.rename
import org.emgen.io.FSys.size
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.io.File
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FSysSpec {

    companion object {
        private val resources = Resources()
    }

    @AfterEach
    fun cleanUp() {
        cleanSpecResources()
    }

    @Test
    fun `name() returns {@link File}'s name, containing an extension in case it exists`() {
        assertEquals("FILE.txt", File(resources.createResourceURI("resources", "FILE.txt")).name())
        assertEquals(".FILE.txt", File(resources.createResourceURI("resources", ".FILE.txt")).name())
        assertEquals("FILE", File(resources.createResourceURI("resources", "FILE")).name())
        assertEquals(".FILE", File(resources.createResourceURI("resources", ".FILE")).name())
    }

    @Test
    fun `name() returns {@link File}'s name, containing no extension`() {
        assertEquals("FILE", File(resources.createResourceURI("resource", "FILE.txt")).name(extension = false))
        assertEquals(".FILE", File(resources.createResourceURI("resource", ".FILE.txt")).name(extension = false))
        assertEquals("FILE", File(resources.createResourceURI("resource", "FILE")).name(extension = false))
        assertEquals(".FILE", File(resources.createResourceURI("resource", ".FILE")).name(extension = false))
    }

    @Test
    fun `contents() produces {@link IllegalArgumentException} in case argument 'depth' is less than 1`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).contents(depth = 0)
        }
    }

    @Test
    fun `contents() produces {@link IllegalArgumentException} in case {@link File} does not exist`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).contents()
        }
    }

    @Test
    fun `contents() produces {@link IllegalArgumentException} in case {@link File} is not a directory`() {
        val resource = resources.createResource(resources.createResourceURI("resources", "FILE.txt"), directory = false)

        assertThrows<IllegalArgumentException> {
            resource.contents()
        }
    }

    @Test
    fun `contents() lists directory contents, based on 'depth' argument provided`() {
        // resources
        //     DIRECTORY_ROOT
        //         DIRECTORY_DEPTH_1
        //             FILE_DEPTH_2.txt
        //         FILE_DEPTH_1.txt
        val target = resources.createResource(resources.createResourceURI("resources", "DIRECTORY_ROOT"))
        resources.createResource(resources.createResourceURI("resources", "DIRECTORY_ROOT", "DIRECTORY_DEPTH_0"))
        resources.createResource(
            resources.createResourceURI("resources", "DIRECTORY_ROOT", "FILE_DEPTH_0.txt"),
            directory = false
        )
        resources.createResource(
            resources.createResourceURI("resources", "DIRECTORY_ROOT", "DIRECTORY_DEPTH_0", "FILE_DEPTH_1.txt"),
            directory = false
        )

        assertEquals(2, target.contents(depth = 1).size)
        assertEquals(3, target.contents(depth = 2).size)
    }

    @Test
    fun `contents() returns empty list in case directory is empty`() {
        val directory = resources.createResource(resources.createResourceURI("resources", "DIRECTORY"))

        assertTrue { directory.contents().isEmpty() }
    }

    @Test
    fun `size() produces {@link IllegalArgumentException} in case {@link File} does not exist`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).size()
        }
    }

    @Test
    fun `size() returns {@link File}'s size, based on capacity type provided`() {
        val empty = resources.createResource(
            path = resources.createResourceURI("resources", "FILE_DEPTH_0.txt"),
            directory = false
        )
        val directory = resources.createResource(resources.createResourceURI("resources", "DIRECTORY_DEPTH_0.txt"))
        val res8 = resources.createResource(
            path = resources.createResourceURI("resources", "DIRECTORY_DEPTH_0.txt", "FILE_DEPTH_1_A.txt"),
            directory = false
        )
        val res16 = resources.createResource(
            path = resources.createResourceURI("resources", "DIRECTORY_DEPTH_0.txt", "FILE_DEPTH_1_B.txt"),
            directory = false
        )
        res8
            .printWriter()
            .use { out -> out.print("12345678") } // 8 bytes
        res16
            .printWriter()
            .use { out -> out.print("1234567890123456") } // 16 bytes

        assertEquals(BigDecimal(0), empty.size(capacityType = CapacityType.BYTE))
        assertEquals(BigDecimal(8), res8.size(capacityType = CapacityType.BYTE))
        assertEquals(BigDecimal(16), res16.size(capacityType = CapacityType.BYTE))
        assertEquals(BigDecimal(24), directory.size(capacityType = CapacityType.BYTE))
        assertEquals(BigDecimal(0.0234375), resources.getResource("resources").size())
    }

    @Test
    fun `remove() produces {@link IllegalArgumentException} in case {@link File} does not exist`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).remove()
        }
    }

    @Test
    fun `remove() removes {@link File} and returns true`() {
        val res = resources.createResource(
            path = resources.createResourceURI("resources", "FILE_DEPTH_0.txt"),
            directory = false
        )
        val directory = resources.createResource(resources.createResourceURI("resources", "DIRECTORY_DEPTH_0"))
        resources.createResource(
            path = resources.createResourceURI("resources", "DIRECTORY_DEPTH_0", "FILE_DEPTH_1.txt"),
            directory = false
        )

        assertTrue { res.remove() }
        assertTrue { !res.exists() }

        assertTrue { directory.remove() }
        assertTrue { !directory.exists() }
    }

    @Test
    fun `clear() produces {@link IllegalArgumentException} in case {@list File} does not exist`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "DIRECTORY")).clear()
        }
    }

    @Test
    fun `clear() produces {@link IllegalArgumentException} in case {@list File} is not a directory`() {
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )

        assertThrows<IllegalArgumentException> {
            resource.clear()
        }
    }

    @Test
    fun `clear() removes (clears) directory contents and returns true`() {
        val directory = resources.createResource(resources.createResourceURI("resources", "DIRECTORY_DEPTH_0"))
        resources.createResource(resources.createResourceURI("resources", "DIRECTORY_DEPTH_0", "DIRECTORY_DEPTH_1"))
        resources.createResource(
            path = resources.createResourceURI(
                "resources",
                "DIRECTORY_DEPTH_0",
                "DIRECTORY_DEPTH_1",
                "FILE_DEPTH_2.txt"
            ),
            directory = false
        )

        assertTrue { directory.clear() }
        assertTrue { directory.exists() }
    }

    @Test
    fun `rename() produces {@link IllegalArgumentException} in case argument 'name' is empty or contains no text`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).rename("")
        }
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).rename(" ")
        }
    }

    @Test
    fun `rename() produces {@link IllegalArgumentException} in case {@link File} does not exist`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).rename("renamed.txt")
        }
    }

    @Test
    fun `rename() produces {@link IllegalArgumentException} in case there is another {@link File} that has the same name`() {
        resources.createResource(
            path = resources.createResourceURI("resources", "renamed.txt"),
            directory = false
        )
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )

        assertThrows<IllegalArgumentException> {
            resource.rename("renamed.txt")
        }
    }

    @Test
    fun `rename() produces {@link RuntimeException} in case {@link File} could not be renamed`() {
        val resource = mock(File::class.java)
        `when`(resource.exists()).thenReturn(true)
        `when`(resource.parent).thenReturn(resources.createResourceURI("resources"))
        `when`(resource.renameTo(any(File::class.java))).thenReturn(false)

        assertThrows<RuntimeException> {
            resource.rename("renamed.txt")
        }
    }

    @Test
    fun `rename() renames {@link File} and returns renamed {@link File}`() {
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )
        val renamed = resource.rename("renamed.txt")

        assertTrue { renamed.exists() }
        assertEquals("renamed.txt", renamed.name)
    }

    @Test
    fun `copyAs() produces {@link IllegalArgumentException} in case source {@link File} does not exist`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).copyAs()
        }
    }

    @Test
    fun `copyAs() produces {@link IllegalArgumentException} in case destination directory does not exist`() {
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )

        assertThrows<IllegalArgumentException> {
            resource.copyAs(destination = resources.createResourceURI("resources", "DIRECTORY"))
        }
    }

    @Test
    fun `copyAs() produces {@link IllegalArgumentException} in case argument 'name' is provided and it points to existing {@link File}`() {
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )
        val destination = resources.createResource(resources.createResourceURI("resources", "DIRECTORY"))
        resources.createResource(
            path = resources.createResourceURI("resources", "DIRECTORY", "existing.txt"),
            directory = false
        )

        assertThrows<IllegalArgumentException> {
            resource.copyAs(destination = destination.toPath().toString(), name = "existing.txt")
        }
    }

    @Test
    fun `copyAs() copies {@link File} to a destination directory, using provided name`() {
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )
        val destination = resources.createResource(resources.createResourceURI("resources", "DIRECTORY"))
        val copied = resource.copyAs(destination = destination.toPath().toString(), name = "copied.txt")

        assertTrue { copied.exists() }
        assertEquals("copied.txt", copied.name)
    }

    @Test
    fun `copyAs() copies {@link File} to (unprovided) destination directory, using generated name`() {
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )

        val copyA = resource.copyAs()
        val copyB = resource.copyAs()

        assertTrue { copyA.exists() }
        assertEquals("FILE copy.txt", copyA.name)

        assertTrue { copyB.exists() }
        assertEquals("FILE copy (2).txt", copyB.name)
    }

    @Test
    fun `copyAs() copies source directory and it's contents`() {
        val resource = resources.createResource(resources.createResourceURI("resources", "DIRECTORY"))
        resources.createResource(
            path = resources.createResourceURI("resources", "DIRECTORY", "FILE.txt"),
            directory = false
        )

        val copy = resource.copyAs()

        assertTrue { copy.exists() }
        assertEquals("DIRECTORY copy", copy.name)
        assertEquals(1, copy.list()?.size)
    }

    @Test
    fun `move() produces {@link IllegalArgumentException} in case source {@link File} does not exist`(){
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt"))
                .move(File(resources.createResourceURI("resources", "DIRECTORY")))
        }
    }

    @Test
    fun `move() produces {@link IllegalArgumentException} in case destination directory does not exist`(){
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )

        assertThrows<IllegalArgumentException> {
            resource.move(File(resources.createResourceURI("resources", "DIRECTORY")))
        }
    }

    @Test
    fun `move() moves {@link File} to destination directory and returns moved {@link File}`() {
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )
        val directory = resources.createResource(resources.createResourceURI("resources", "DIRECTORY"))

        val res = resource.move(directory)

        assertTrue { res.exists() }
        assertTrue { !resource.exists() }
        assertEquals("FILE.txt", res.name)
    }

    @Test
    fun `create() produces {@link IllegalArgumentException} in case parent directory does not exist`() {
        assertThrows<IllegalArgumentException> {
            File(resources.createResourceURI("resources", "FILE.txt")).create()
        }
    }

    @Test
    fun `create() produces {@link IllegalArgumentException} in case source {@link File} exists`() {
        val resource = resources.createResource(
            path = resources.createResourceURI("resources", "FILE.txt"),
            directory = false
        )

        assertThrows<IllegalArgumentException> {
            resource.create(directory = false)
        }
    }

    @Test
    fun `create() creates directory and returns true`() {
        val directory = resources.createResource(resources.createResourceURI("resources"))
        val resource = File(resources.createResourceURI(directory.toPath().toString(), "DIRECTORY"))

        assertTrue { resource.create() }
        assertTrue { resource.isDirectory }
    }

    @Test
    fun `create() creates {@link File} and returns true`() {
        val directory = resources.createResource(resources.createResourceURI("resources"))
        val resource = File(resources.createResourceURI(directory.toPath().toString(), "FILE.txt"))

        assertTrue { resource.create(directory = false) }
        assertTrue { resource.isFile }
        assertEquals(0, resource.length())
    }

    private fun cleanSpecResources() {
        try {
            val resource = resources.getResource("resources")

            if (resource.isDirectory) {
                resource.deleteRecursively()
            } else {
                resource.delete()
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}