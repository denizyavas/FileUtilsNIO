package net.deniz.nio

import groovy.util.logging.Slf4j
import net.deniz.nio.utils.FileCommonUtils

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * author: TRYavasU
 * date: 15/11/2014
 */
@Slf4j
class CopyFileVisitor extends SimpleFileVisitor<Path> {
    Path targetPath
    Path sourcePath

    CopyFileVisitor(Path targetPath) {
        this.targetPath = targetPath
    }

    @Override
    FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (sourcePath) {
            Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)))
        } else {
            sourcePath = dir
        }
        FileVisitResult.CONTINUE
    }

    @Override
    FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        try {
            if (!sourcePath) {
                sourcePath = file.parent
            }

            Path targetFile = targetPath.resolve(sourcePath.relativize(file))
            FileCommonUtils.createParent(targetFile)
            Files.copy file, targetFile, StandardCopyOption.REPLACE_EXISTING
            log.info "File [${file}] is copied to target [${targetFile}]"
        } catch (IOException e) {
            log.debug "Unable to copy file [${file}] due to errors", e
        }
        FileVisitResult.CONTINUE
    }
}
