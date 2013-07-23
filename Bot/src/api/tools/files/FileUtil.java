/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package api.tools.files;

import lombok.extern.log4j.Log4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Log4j
@ParametersAreNonnullByDefault
public class FileUtil {
    private FileUtil() {
    }

    public static void createDirectoryIfNotExists(final String path) {
        checkNotNull(path);
        try {
            File dir = new File(path);
            if (!dir.isDirectory()) Files.createDirectory(dir.toPath());
        } catch (IOException e) {
            log.error("Failed to create directory: " + path);
        }
    }

    /**
     * Creates a BotFileVisitor and fills it with information from walking the file tree
     *
     * @param dir       the starting point directory
     * @param recursive whether to walk the tree recursively
     * @param matchers  FileMatchers to choose which files to do something with
     * @return a BotFileVisitor containing the matched files and directories
     * @throws IOException .
     */
    public static BotFileVisitor visitDirectory(final Path dir, final boolean recursive, final FileMatcher... matchers) throws IOException {
        BotFileVisitor visitor = new BotFileVisitor(recursive, matchers);
        Files.walkFileTree(dir, visitor);
        return visitor;
    }

    /**
     * @param file the file
     * @return the file extension
     */
    public static String getFileExtension(final Path file) {
        return getExtensionOrEmpty(file.getFileName().toString());
    }

    private static String getExtensionOrEmpty(final String string) {
        int pos = string.lastIndexOf('.');
        return pos > 0 && pos < string.length() ? string.substring(pos + 1) : "";
    }

    /**
     * A class that can be used to collect information while walking a file tree
     */
    @ParametersAreNonnullByDefault
    public static class BotFileVisitor implements FileVisitor<Path> {
        private final List<Path> files = new ArrayList<>();
        private final Collection<Path> dirs = new ArrayList<>();
        private final boolean recursive;
        private final FileMatcher[] matchers;

        public BotFileVisitor(final boolean recursive, final FileMatcher... matchers) {
            this.recursive = recursive;
            this.matchers = matchers.clone();
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            dirs.add(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            if (matchers == null || matchers.length == 0) {
                files.add(file);
            } else {
                for (FileMatcher matcher : matchers) {
                    if (matcher.matches(file)) files.add(file);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            return recursive ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
            log.error("Could not visit file: " + file.getFileName(), exc);
            return FileVisitResult.CONTINUE;
        }

        /**
         * @return a List of all the matched files
         */
        public List<Path> getFiles() {
            return Collections.unmodifiableList(files);
        }

        /**
         * @return a List of all the matched directories
         */
        public Collection<Path> getDirs() {
            return Collections.unmodifiableCollection(dirs);
        }
    }
}
