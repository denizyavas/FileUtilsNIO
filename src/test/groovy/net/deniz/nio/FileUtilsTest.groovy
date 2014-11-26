package net.deniz.nio

import org.apache.commons.lang.StringUtils
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.Permission
import java.util.zip.ZipInputStream

import static org.junit.Assert.*

/**
 * author: TRYavasU
 * date: 16/11/2014
 */
class FileUtilsTest {

    def basePath = Paths.get(System.getProperty("java.io.tmpdir") + "/fileTest/a/b/c")
    def filePath1 = Paths.get("${basePath}/file1.txt")
    def filePath2 = Paths.get("${basePath}/file2.txt")
    def filePath3 = Paths.get("${basePath}/file3.txt")
    def filePath4 = Paths.get("${basePath}/d/file4.txt")
    def filePath5 = Paths.get("${basePath}/d/e/file5.txt")
    def filePath6 = Paths.get("${basePath}/d")
    def filePath7 = Paths.get("${basePath}/d/file7.txt")
    def filePath8 = Paths.get("${basePath}/d/f/file8.txt")
    def filePath9 = Paths.get("${basePath}/g/h")
    def filePath10 = Paths.get("${basePath}/g/h/file7.txt")
    def zipPath = Paths.get("${basePath.parent}/a.zip")

    @Before
    void doBefore() {
        resetFilePermissions()
    }

    @After
    void doAfter() {
        resetFilePermissions()
        FileUtils.delete(basePath, false)
        //Delete zip file in zip tests
        FileUtils.delete(zipPath, false)
    }

    @Test
    void writeFile() throws Exception {
        assertTrue(FileUtils.writeFile(filePath1, "test".bytes))
    }

    @Test
    void writeFile_unableToWrite_alreadyExists() {
        assertTrue(FileUtils.writeFile(filePath1, "test".bytes))
        assertFalse(FileUtils.writeFile(filePath1, "test".bytes, false))
    }


    @Test
    void writeFile_unableToWrite_writeToDirectory() throws Exception {
        assertTrue(FileUtils.writeFile(filePath1, "test".bytes))

        // basePath is a directory now
        assertFalse(FileUtils.writeFile(basePath, "test".bytes))
    }

    @Test
    void deleteDirectory() throws Exception {
        FileUtils.writeFile(filePath1, "test".bytes)
        FileUtils.writeFile(filePath2, "test".bytes)
        FileOperationResult result = FileUtils.delete(basePath, false)

        assertFalse(Files.exists(basePath))
        assertTrue(result.filesTracked.contains(filePath1))
        assertTrue(result.filesTracked.contains(filePath2))
        assertNotEquals(result.filesFailed.size(), 0)
    }

    @Test
    void deleteFile() throws Exception {
        FileUtils.writeFile(filePath1, "test".bytes)

        FileOperationResult result = FileUtils.delete(filePath1, false)
        assertEquals(filePath1, result.filesTracked.get(0))

        assertTrue(Files.exists(basePath))
        assertFalse(Files.exists(filePath1))
        assertTrue(!result.filesFailed)
    }

    @Test
    void deleteDirectory_unableToDelete_missingFolder_throwException() throws Exception {
        FileUtils.writeFile(filePath1, "test".bytes)

        FileOperationResult result = FileUtils.delete(basePath, true)
        assertTrue(result.filesTracked.contains(filePath1))
        assertFalse(Files.exists(basePath))
        assertEquals(result.filesFailed.size(), 0)

        // delete already deleted folder
        result = FileUtils.delete(basePath, true)
        assertTrue(result.filesTracked.isEmpty())
        assertTrue(result.filesTracked.isEmpty())
    }

    @Test
    void deleteDirectory_unableToDelete_missingFolder_noException() throws Exception {
        FileUtils.writeFile(filePath1, "test".bytes)

        FileOperationResult result = FileUtils.delete(basePath, true)
        assertTrue(result.filesTracked.contains(filePath1))
        assertFalse(Files.exists(basePath))
        assertEquals(result.filesFailed.size(), 0)

        // delete already deleted folder
        result = FileUtils.delete(basePath, false)
        assertTrue(result.filesTracked.isEmpty())
        assertTrue(result.filesTracked.isEmpty())
    }

    @Test
    void deleteFile_unableToDelete_securityIssuesOnOneFile() {
        FileUtils.writeFile(filePath1, "test".bytes)
        FileUtils.writeFile(filePath2, "test".bytes)
        FileUtils.writeFile(filePath3, "test".bytes)

        // disable delete permissions for filePath2
        System.setSecurityManager(new SecurityManager() {
            @Override
            void checkDelete(String file) {
                if (file.equals(filePath2.toString()))
                    throw new SecurityException("Unable to delete " + file)
            }

            @Override
            void checkPermission(Permission perm) {
            }
        })
        FileOperationResult result = FileUtils.delete(basePath, true)

        assertTrue(Files.exists(basePath))
        assertFalse(Files.exists(filePath1))
        assertTrue(Files.exists(filePath2))
        assertFalse(Files.exists(filePath3))

        assertEquals(2, result.filesTracked.size())
        assertTrue(result.filesTracked.contains(filePath1))
        assertTrue(result.filesTracked.contains(filePath3))

        assertEquals(result.filesFailed.size(), 1)
        assertTrue(result.filesFailed.contains(filePath2))
    }

    @Test
    void deleteFolder_unableToDelete_securityIssuesOnOneFolder() {
        FileUtils.writeFile(filePath1, "test".bytes)
        FileUtils.writeFile(filePath2, "test".bytes)

        // disable delete permissions for basePath
        System.setSecurityManager(new SecurityManager() {
            @Override
            void checkDelete(String file) {
                if (file.equals(basePath.toString()))
                    throw new SecurityException("Unable to delete " + file)
            }

            @Override
            void checkPermission(Permission perm) {
            }
        })
        FileOperationResult result = FileUtils.delete(basePath, true)


        assertTrue(Files.exists(basePath))
        assertFalse(Files.exists(filePath1))
        assertFalse(Files.exists(filePath2))

        assertEquals(2, result.filesTracked.size())
        assertTrue(result.filesTracked.contains(filePath1))
        assertTrue(result.filesTracked.contains(filePath2))

        assertEquals(result.filesFailed.size(), 0)
    }

    @Test
    void zipDirectory() throws IOException {
        FileUtils.writeFile(filePath1, "test".bytes)
        FileUtils.writeFile(filePath2, "test".bytes)
        FileOperationResult result = FileUtils.zip(zipPath, basePath)

        assertTrue(Files.exists(Paths.get(basePath.parent.toString() + "/a.zip")))
        assertTrue(result.filesTracked.contains(filePath1))
        assertTrue(result.filesTracked.contains(filePath2))

        validateZip(zipPath, ["file1.txt", "file2.txt"])
    }

    @Test
    void zipFile() throws IOException {
        FileUtils.writeFile(filePath1, "test".bytes)
        FileOperationResult result = FileUtils.zip(zipPath, filePath1)

        assertTrue(Files.exists(zipPath))
        assertTrue(result.filesTracked.contains(filePath1))

        validateZip(zipPath, Arrays.asList("file1.txt"))
    }

    @Test
    void zipFile_noFilesToZip() throws IOException {
        FileOperationResult result = FileUtils.zip(Paths.get(filePath1.parent.toString() + "/a.zip"), filePath1)

        assertFalse(Files.exists(Paths.get(filePath1.parent.toString() + "/a.zip")))
        assertEquals(0, result.filesTracked.size())
    }

    @Test
    void zipFile_missingData() throws IOException {
        FileOperationResult result = FileUtils.zip(null, null)
        assertEquals(0, result.filesTracked.size())
    }

    @Test
    void zipStructure() throws IOException {
        FileUtils.writeFile(filePath1, "test".bytes)
        FileUtils.writeFile(filePath2, "test".bytes)
        Files.createDirectory(Paths.get(basePath.toString() + "/d"))
        FileUtils.writeFile(filePath4, "test".bytes)
        Files.createDirectory(Paths.get(basePath.toString() + "/d/e"))
        FileUtils.writeFile(filePath5, "test".bytes)
        FileOperationResult result = FileUtils.zip(zipPath, basePath)

        assertTrue(Files.exists(Paths.get(basePath.parent.toString() + "/a.zip")))
        assertTrue(result.filesTracked.contains(filePath1))
        assertTrue(result.filesTracked.contains(filePath2))
        assertTrue(result.filesTracked.contains(filePath4))
        assertTrue(result.filesTracked.contains(filePath5))

        validateZip(zipPath, Arrays.asList("d/", "d/e/", "d/e/file5.txt", "d/file4.txt", "file1.txt", "file2.txt"))
    }

    @Test(expected = InstantiationException.class)
    void utilityClassCheck() throws Throwable {
        try {
            Constructor c = Class.forName(FileUtils.class.name).declaredConstructors
            c.accessible = true
            c.newInstance()
        } catch (InvocationTargetException e) {
            throw e.targetException
            // no need to expect reflection errors
            // we are interested in our own exceptions
        }
    }

    @Test
    void copyFolderToFolder() throws IOException {
        FileUtils.writeFile(filePath7, "test".bytes)
        FileUtils.writeFile(filePath8, "test".bytes)

        FileUtils.copy(filePath6, filePath9)

        assertTrue(Files.exists(Paths.get(filePath9.toString() + "/file7.txt")))
        assertTrue(Files.exists(Paths.get(filePath9.toString() + "/f/file8.txt")))
    }

    @Test
    void copyFileToFolder() throws IOException {
        FileUtils.writeFile(filePath7, "test".bytes, true)

        FileUtils.copy(filePath7, filePath9)

        assertTrue(Files.exists(Paths.get(filePath9.toString() + "/file7.txt")))
    }

    @Test
    void copyFileToFolder_overrideExistingFile() throws IOException {
        FileUtils.writeFile(filePath7, "test".bytes)
        FileUtils.writeFile(filePath10, "test1".bytes)

        assertEquals(StringUtils.join(Files.readAllLines(filePath10, Charset.forName("UTF-8")), ""), "test1")
        //Override filePath10
        FileUtils.copy filePath7, filePath9

        assertTrue(Files.exists(filePath10))
        assertEquals(StringUtils.join(Files.readAllLines(filePath10, Charset.forName("UTF-8")), ""), "test")
    }

    static void resetFilePermissions() {
        System.setSecurityManager(new SecurityManager() {
            @Override
            void checkDelete(String file) {
            }

            @Override
            void checkPermission(Permission perm) {
            }
        })
    }

    static void validateZip(Path zipPath, List<String> content) throws IOException {
        def entry
        def zipContent = []
        while (!(entry = new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(zipPath))).nextEntry())) {
            zipContent << entry.name
        }

        assertEquals(content.size(), zipContent.size())
        content.each {
            assertTrue zipContent.contains(it)
        }
    }

}
