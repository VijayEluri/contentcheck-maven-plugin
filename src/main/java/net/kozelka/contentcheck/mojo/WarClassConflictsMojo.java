package net.kozelka.contentcheck.mojo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.kozelka.contentcheck.conflict.api.ArchiveConflict;
import net.kozelka.contentcheck.conflict.api.ClassConflictReport;
import net.kozelka.contentcheck.conflict.impl.ClassConflictAnalyzer;
import net.kozelka.contentcheck.conflict.impl.ClassConflictPrinter;
import net.kozelka.contentcheck.conflict.impl.ConflictingResourcesReport;
import net.kozelka.contentcheck.conflict.model.ArchiveInfo;
import net.kozelka.contentcheck.conflict.util.ArchiveLoader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Looks for conflicts within the libraries in given sourceFile.
 *
 * @author Petr Kozelka
 * @since 1.0.3
 */
@Mojo(name="warcc", defaultPhase = LifecyclePhase.VERIFY)
public class WarClassConflictsMojo extends AbstractMojo {
    /**
     * If true, no check is performed.
     */
    @Parameter(defaultValue = "false", property = "contentcheck.skip")
    boolean skip;

    /**
     * The archive file to be checked
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.war", property = "sourceFile")
    File sourceFile;

    /**
     * How many class conflicts to list directly. Use <code>-1</code> to list all.
     */
    @Parameter(defaultValue = "5")
    int previewThreshold;

    /**
     * How many overlaps are we tolerating.
     * Useful to ensure that the number is not growing, when you cannot fix everything.
     * @todo replace this with include/exclude lists
     */
    @Parameter(defaultValue = "0")
    int toleratedOverlapCount;

    /**
     * Reports jar pairs in the log. Each two conflicting jars are displayed, with number of their overlaps and conflicts.
     */
    @Parameter(defaultValue = "true")
    boolean reportJarPairs;

    /**
     * Reports every overlaping resource in the log, with all files that provide it.
     * This report is not very mature to it is turned off by default.
     */
    @Parameter(defaultValue = "false")
    boolean reportResources;

    /**
     * @deprecated Use {@link #toleratedOverlapCount} instead.
     */
    @Deprecated
    @Parameter(defaultValue = "-1")
    int toleratedConflictCount;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Content conflict checking is skipped.");
            return;
        }

        if (toleratedConflictCount > -1) {
            getLog().warn("Parameter 'toleratedConflictCount' is deprecated - use 'toleratedOverlapCount' instead");
            if (toleratedOverlapCount > 0) {
                getLog().warn("Parameter 'toleratedConflictCount' is just a deprecated variant of 'toleratedOverlapCount'. You specified both, using only the latter.");
            } else {
                toleratedOverlapCount = toleratedConflictCount;
            }
        }
        //
        try {
            final ClassConflictAnalyzer ccd = new ClassConflictAnalyzer();
            final List<ArchiveInfo> archives = ArchiveLoader.loadWar(sourceFile);
            final ClassConflictReport report = ccd.analyze(archives);
            final List<ArchiveConflict> archiveConflicts = report.getArchiveConflicts();
            final int totalOverlaps = report.getTotalOverlaps();
            if (archiveConflicts.isEmpty()) {
                getLog().info("No overlaps detected.");
            } else {
                final StreamConsumer output = new StreamConsumer() {
                    public void consumeLine(String line) {
                        getLog().error(line);
                    }
                };
                if (reportJarPairs) {
                    final ClassConflictPrinter printer = new ClassConflictPrinter();
                    printer.setPreviewThreshold(previewThreshold);
                    printer.setOutput(output);
                    printer.print(report);
                }
                if (reportResources) {
                    final ConflictingResourcesReport printer = new ConflictingResourcesReport();
                    printer.setOutput(output);
                    printer.print(report);
                }
                final String errorMessage = String.format("Found %d overlapping resources in %d competing archives in %s",
                    totalOverlaps,
                    archiveConflicts.size(),
                    sourceFile);
                getLog().error(errorMessage);
                if (totalOverlaps > toleratedOverlapCount) {
                    throw new MojoFailureException(errorMessage);
                }
            }
            if (totalOverlaps < toleratedOverlapCount) {
                getLog().warn(String.format("We currently tolerate %d overlaps; please reduce the tolerance to prevent growing mess", toleratedOverlapCount));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(sourceFile.getAbsolutePath(), e);
        }
    }
}
