package net.deniz.nio

import net.deniz.nio.utils.FileCommonUtils

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

/**
 * author: TRYavasU
 * date: 16/11/2014
 */
class ZipFileVisitor extends SimpleFileVisitor<Path> {

    FileSystem fileSystem
    FileOperationResult result
    Path fileOrFolderToZip

    ZipFileVisitor(Path zipFilePath, FileOperationResult result, Path fileOrFolderToZip) throws IOException {
        this.result = result
        this.fileOrFolderToZip = fileOrFolderToZip

        // check if file exists
        def env = ["create": Boolean.toString(!Files.exists(zipFilePath))]

        // use a Zip filesystem URI
        URI fileUri = zipFilePath.toUri()
        fileSystem = FileSystems.newFileSystem(URI.create("jar:${fileUri.scheme}:${fileUri.path}"), env)
    }

    @Override
    FileVisitResult visitFile(Path fileToZip, BasicFileAttributes attrs) throws IOException {
        //Default file behavior is to include file in zip default folder behavior is not to include folder in zip
        Path root = Files.isRegularFile(fileOrFolderToZip) ? fileOrFolderToZip.parent : fileOrFolderToZip

        Path locationInZip = fileSystem.getPath(root.relativize(fileToZip).toString())

        //Create folder structure in zip file system
        FileCommonUtils.createParent(locationInZip)

        // copy fileToZip to its location in zip
        Files.copy(fileToZip, locationInZip, StandardCopyOption.REPLACE_EXISTING)

        result.filesTracked << fileToZip
        FileVisitResult.CONTINUE
    }
}