package net.deniz.nio.utils

import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path

/**
 * author: TRYavasU
 * date: 16/11/2014
 */
@Slf4j
class FileCommonUtils {

    FileCommonUtils() throws InstantiationException {
        throw new InstantiationException("Utility classes should not be instantiated");
    }

    static void createParent(Path file) {
        def parent = file.parent
        if (parent != null && Files.notExists(parent)) {
            log.debug "Destination path ${file} does not exist, so creating parent folder"
            Files.createDirectories parent
        }
    }
}
