package net.kozelka.contentcheck.introspection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Flexible strategory for handling input various inputs containing files to be checked.
 * Two basic options are:
 * <ul>
 *     <li>ZIP archive file</li>
 *     <li>directory</li>
 * </ul>
 */
interface IntrospectorInputStrategy {
    /**
     * Lists all file entries in specified container.
     * Entries should be represented by theirs paths.
     * <p>
     *     Example: <br />
     *     For container file /user/home/myTestDir:
     *     <pre>
     *     - dir1
     *       -- dir1file1
     *       -- dir1file2
     *     - file1
     *     </pre>
     *     The following list should be returned:
     *     <pre>
     *         [dir1, dir1/dir1file1, dir1/dir1file2, file1]
     *     </pre>
     *
     * </p>
     *
     *
     *
     * @param containerFile directory or ZIP archive
     * @return list of all entries names
     * @throws IOException
     */
    Set<String> list(final File containerFile) throws IOException;

    /**
     * Returns specified entry as an {@link java.io.InputStream inputstream}.
     *
     * @param containerFile base file containing the entry
     * @param entryPath entry path within specified {@code containerFile}
     * @return entry data as input stream
     */
    InputStream getInputStream(File containerFile, String entryPath) throws IOException;
}
