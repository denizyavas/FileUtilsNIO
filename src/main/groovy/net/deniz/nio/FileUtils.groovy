package net.deniz.nio

import groovy.util.logging.Slf4j
import net.deniz.nio.utils.FileCommonUtils

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * author: TRYavasU
 * date: 16/11/2014
 */
@Slf4j
class FileUtils {

    FileUtils() throws InstantiationException {
        throw new InstantiationException("Utility classes should not be instantiated")
    }

    /**
     * Deletes the given file or folder. If any file or folder cannot be deleted due to any issues,
     * deletion process continues with the other ones.
     *
     * @param dir as the path of file or folder to delete
     * @return List of deleted paths
     */
    static FileOperationResult delete(Path dir, boolean failIfNoFile) {
        final def result = new FileOperationResult()

        def visitor = [
                visitFile         : { Path file, BasicFileAttributes attrs ->
                    try {
                        Files.deleteIfExists file
                        result.filesTracked << file
                    } catch (Exception e) {
                        result.filesFailed << file
                        log.debug "Unable to delete file [${file}] due to errors", e
                    }
                    FileVisitResult.CONTINUE
                },
                postVisitDirectory: { Path directory, IOException exc ->
                    if (exc == null) {
                        try {
                            Files.deleteIfExists directory
                        } catch (Exception e) {
                            log.debug "Unable to delete directory [${directory}] due to errors", e
                        }
                    } else {
                        log.debug "Unable to list directory [${directory}] due to errors", exc
                    }
                    FileVisitResult.CONTINUE
                }

        ] as SimpleFileVisitor<Path>

        if (!Files.exists(dir) && !failIfNoFile) {
            log.debug "File/folder in path [${dir}] is not found avoiding exception since failIFNoFile is false"
            result.message = "File/folder in path [${dir}] is not found"
            result
        } else {
            try {
                Files.walkFileTree dir, visitor
                log.debug "File/folder in path [${dir}] is deleted"
                result.message = "File/folder in path [${dir}] is deleted"
            } catch (Exception e) {
                log.debug "Unable to walk in directory [${dir}] due to errors", e
                result.message = "Unable to walk in directory [${dir}] due to errors"
            }
            result
        }
    }

    static boolean writeFile(Path path, byte[] content, boolean override = true) {
        try {
            if (!override && Files.exists(path)) {
                log.debug "File already exists in path [${path}] and override is not allowed"
                false
            } else {
                FileCommonUtils.createParent path
                Files.write path, content
                log.debug "File is written in path [${path}]"
                true
            }
        }
        catch (
                IOException e
                ) {
            log.debug "Content cannot be written to path [${path}]", e
            false
        }
    }

    static FileOperationResult zip(Path zipFilePath, Path fileOrFolderToZip) {
        def result = new FileOperationResult()
        try {
            def visitor = new ZipFileVisitor(zipFilePath, result, fileOrFolderToZip)
            Files.walkFileTree fileOrFolderToZip, visitor
            visitor.fileSystem.close()
            log.debug "File/folder in path [${fileOrFolderToZip}] is zipped into [${zipFilePath}]"
        } catch (Exception e) {
            log.debug "Unable to zip directory [${fileOrFolderToZip}] to path [${zipFilePath}] due to errors", e
        }
        result
    }

    static void copy(Path sourcePath, Path targetPath) throws IOException {
        Files.walkFileTree sourcePath, new CopyFileVisitor(targetPath)
    }

}
