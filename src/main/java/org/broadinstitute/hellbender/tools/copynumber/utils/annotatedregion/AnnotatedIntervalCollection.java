package org.broadinstitute.hellbender.tools.copynumber.utils.annotatedregion;

import com.google.common.collect.Sets;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.CloseableTribbleIterator;
import htsjdk.tribble.FeatureReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.codecs.xsvLocatableTable.XsvLocatableTableCodec;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Represents a collection of annotated regions.  The annotations do not need to be known ahead of time, if reading from a file.
 *
 *  This class supports reading xsv (tsv, csv) files with comments ("#") and SAM headers ("@").  The default is tsv.
 *   If the file has a preamble of comments ("#"), these will all be comments in the generated SamFileHeader
 */
public class AnnotatedIntervalCollection {

    private static final Logger logger = LogManager.getLogger(AnnotatedIntervalCollection.class);

    // Rename to annotated interval default.config
    public static final String ANNOTATED_INTERVAL_DEFAULT_CONFIG_RESOURCE = "org/broadinstitute/hellbender/tools/copynumber/utils/annotatedregion/annotated_region_default.config";
    private final SAMFileHeader samFileHeader;

    /** Does not include the locatable fields. */
    private final List<String> annotations;

    private final List<AnnotatedInterval> records;

    private AnnotatedIntervalCollection(final SAMFileHeader samFileHeader, final List<String> annotations,
                                        final List<AnnotatedInterval> records) {
        this.samFileHeader = samFileHeader;
        this.annotations = annotations;
        this.records = records;
    }

    /**
     *  Same as {@link #create(Path, Path, Set)} , but uses the default annotated
     *   interval config file in the GATK.
     * @param input See {@link #create(Path, Path, Set)}
     * @param headersOfInterest See {@link #create(Path, Path, Set)}
     * @return See {@link #create(Path, Path, Set)}
     */
    public static AnnotatedIntervalCollection create(final Path input, final Set<String> headersOfInterest) {

        final String resourcePath = ANNOTATED_INTERVAL_DEFAULT_CONFIG_RESOURCE;
        try {
            final File tmpResourceFile = Resource.getResourceContentsAsFile(resourcePath);
            return create(input, tmpResourceFile.toPath(), headersOfInterest);
        } catch (final IOException ioe) {
            throw new GATKException.ShouldNeverReachHereException("Could not read config file: " + resourcePath,
                    ioe);
        }
    }



    /**
     * Create a collection from components.
     *
     * @param regions regions to use in the resulting collection.  Never {@code null}.
     * @param samFileHeader SAMFileHeader to include in the collection.  Represents the sample(s)/references that were used for these segments.
     *                      {@code null} is allowed.
     * @param annotations List of annotations to preserve in the regions.  Never {@code null}.  These are the only annotations that will be preserved in the returned collection.
     * @return collection based on the inputs.  Never {@code null}.
     */
    public static AnnotatedIntervalCollection create(final List<AnnotatedInterval> regions,
                                                     final SAMFileHeader samFileHeader,
                                                     final List<String> annotations) {

        Utils.nonNull(regions);
        Utils.nonNull(annotations);

        final List<AnnotatedInterval> updatedAnnotatedIntervals = regions.stream()
                .map(r -> AnnotatedIntervalUtils.copyAnnotatedInterval(r, new HashSet<>(annotations)))
                .collect(Collectors.toList());

        return new AnnotatedIntervalCollection(samFileHeader, annotations, updatedAnnotatedIntervals);
    }

    /** Create a collection based on the contents of an input file and a given config file.  The config file must be the same as
     * is ingested by {@link XsvLocatableTableCodec}.
     *
     * @param input readable path to use for the xsv file.  Must be readable.  Never {@code null}.
     * @param inputConfigFile config file for specifying the format of the xsv file.  Must be readable.  Never {@code null}.
     * @param headersOfInterest Only preserve these headers.  These must be present in the input file.  This parameter should not include the locatable columns
     *                          defined by the config file, which are always preserved.
     *                          Use {@code null} to indicate "all headers are of interest".
     * @return never {@code null}
     */
    public static AnnotatedIntervalCollection create(final Path input, final Path inputConfigFile, final Set<String> headersOfInterest) {

        IOUtils.assertFileIsReadable(input);
        IOUtils.assertFileIsReadable(inputConfigFile);

        final AnnotatedIntervalCodec codec = new AnnotatedIntervalCodec(inputConfigFile);
        final List<AnnotatedInterval> regions = new ArrayList<>();

        if (codec.canDecode(input.toUri().toString())) {
            try (final FeatureReader<AnnotatedInterval> reader = AbstractFeatureReader.getFeatureReader(input.toUri().toString(), codec, false)){

                // This cast is an artifact of the tribble framework.
                @SuppressWarnings("unchecked")
                final AnnotatedIntervalHeader header = (AnnotatedIntervalHeader) reader.getHeader();

                warnAllHeadersOfInterestNotPresent(headersOfInterest, header.getAnnotations());

                // If no headers of interest were specified, then grab all available annotations.  Otherwise, just get a sorted list of the specified headers of interest.
                final List<String> finalHeadersOfInterest = (headersOfInterest == null) ? header.getAnnotations() : headersOfInterest.stream().sorted().collect(Collectors.toList());

                final CloseableTribbleIterator<AnnotatedInterval> it = reader.iterator();
                StreamSupport.stream(it.spliterator(), false)
                        .filter(r -> r != null)
                        .map(r -> AnnotatedIntervalUtils.copyAnnotatedInterval(r,  new HashSet<>(finalHeadersOfInterest)))
                        .forEach(r -> regions.add(r));

                return new AnnotatedIntervalCollection(header.getSamFileHeader(), finalHeadersOfInterest, regions);

            } catch ( final IOException ex ) {
                throw new GATKException("Error - IO problem with file " + input, ex);
            }
        }
        else {
            throw new UserException.BadInput("Could not parse xsv file.");
        }
    }

    private static void warnAllHeadersOfInterestNotPresent(final Set<String> headersOfInterest, final List<String> header) {
        if ((headersOfInterest != null) && !header.containsAll(headersOfInterest)) {
            final Set<String> unusedColumnsOfInterest = Sets.difference(new HashSet<>(headersOfInterest), new HashSet<>(header));
            if (unusedColumnsOfInterest.size() > 0) {
                final List<String> missingColumns = new ArrayList<>(unusedColumnsOfInterest);
                logger.warn("Some headers of interest specified by the user were not seen in input: " + StringUtils.join(missingColumns, ", "));
            }
        }
    }

    /**
     *  Write this collection to a file
     *
     *  Dev note:  This method will force the default xsv config file on the output.
     *
     * @param outputFile destination file, must be writable.
     */
    public void write(final File outputFile) {
        final AnnotatedIntervalWriter writer = new SimpleAnnotatedIntervalWriter(outputFile);
        writer.writeHeader(AnnotatedIntervalCodec.createHeaderForWriter(annotations, samFileHeader));
        getRecords().forEach(writer::add);
        writer.close();
    }

    /** Can return {@code null} */
    public SAMFileHeader getSamFileHeader() {
        return samFileHeader;
    }

    /** Creates a copy from the SAM File header or an empty list if no sam file header. */
    public List<String> getComments() {
        if (getSamFileHeader() == null) {
            return Collections.emptyList();
        } else {
            return getSamFileHeader().getComments().stream().map(c -> c.replaceFirst(SAMTextHeaderCodec.COMMENT_PREFIX, "")).collect(Collectors.toList());
        }
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public List<AnnotatedInterval> getRecords() {
        return records;
    }

    public int size() {
        return getRecords().size();
    }
}
