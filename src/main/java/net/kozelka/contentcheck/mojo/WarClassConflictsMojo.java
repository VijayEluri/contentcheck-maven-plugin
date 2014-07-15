package net.kozelka.contentcheck.mojo;

import net.kozelka.contentcheck.conflict.ArchiveInfo;
import net.kozelka.contentcheck.conflict.ClassConflictDetector;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Looks for conflicts within the libraries in given war.
 *
 * @author Petr Kozelka
 */
@Mojo(name="warcc", defaultPhase = LifecyclePhase.PACKAGE)
public class WarClassConflictsMojo extends AbstractMojo {
    /**
     * The archive file to be checked
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.war", property = "war")
    File war;

    /**
     * How many class conflicts to list directly. Use <code>-1</code> to list all.
     */
    @Parameter(defaultValue = "5")
    int previewThreshold;

    /**
     * How many conflicts are we tolerating.
     * Useful to ensure that the number is not growing, when you cannot fix everything.
     * @todo replace this with include/exclude lists
     */
    @Parameter(defaultValue = "0")
    int toleratedConflictCount;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final ClassConflictDetector ccd = ClassConflictDetector.exploreWar(war);
            final List<ArchiveInfo> conflictingArchives = ccd.getConflictingArchives();
            if (!conflictingArchives.isEmpty()) {
                final int totalConflicts = ccd.printResults(previewThreshold, new ClassConflictDetector.LineOutput() {
                    @Override
                    public void println(String line) {
                        getLog().error(line);
                    }
                });
                final String errorMessage = String.format("Found %d conflicts in %d archives in %s", totalConflicts, conflictingArchives.size(), war);
                getLog().error(errorMessage);
                if (totalConflicts > toleratedConflictCount) {
                    throw new MojoFailureException(errorMessage);
                }
                if (totalConflicts > 0 && toleratedConflictCount > 0) {
                    getLog().warn(String.format("We currently tolerate %d conflicts", toleratedConflictCount));
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(war.getAbsolutePath(), e);
        }
    }
}
