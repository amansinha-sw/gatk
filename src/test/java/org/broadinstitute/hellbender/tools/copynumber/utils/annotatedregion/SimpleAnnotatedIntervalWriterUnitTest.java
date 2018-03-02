package org.broadinstitute.hellbender.tools.copynumber.utils.annotatedregion;

import org.broadinstitute.hellbender.GATKBaseTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class SimpleAnnotatedIntervalWriterUnitTest extends GATKBaseTest {

    private static final File TEST_FILE_NO_SAMHEADER = new File(toolsTestDir,
            "copynumber/utils/combine-segment-breakpoints-no-samheader.tsv");
    private static final File TEST_FILE = new File(toolsTestDir,
            "copynumber/utils/combine-segment-breakpoints.tsv");
    private static final File TEST_CONFIG = new File(toolsTestDir,
            "copynumber/utils/test.config");
    private static final File TEST_NAMED_CONFIG = new File(toolsTestDir,
            "copynumber/utils/test_col_names.config");

    @Test
    public void testNullSamFileHeader() throws IOException {
        final File outputFile = File.createTempFile("simpleannotatedintervalwriter_", ".tsv");
        final AnnotatedIntervalCollection collection = AnnotatedIntervalCollection.create(TEST_FILE_NO_SAMHEADER.toPath(), null);

        final SimpleAnnotatedIntervalWriter writer = new SimpleAnnotatedIntervalWriter(outputFile);

        //TODO: Have the writer ingest a config file?  Since we force it on the input, why not the output?
        writer.writeHeader(
                new AnnotatedIntervalHeader("CONTIG", "START", "END", collection.getAnnotations(), null, collection.getComments()));
        collection.getRecords().forEach(r -> writer.add(r));
        writer.close();

        final AnnotatedIntervalCollection testCollection = AnnotatedIntervalCollection.create(outputFile.toPath(), null);

        // Reminder: In this case, the output will have additional comments for the header
        Assert.assertEquals(testCollection.getComments().subList(0,2), collection.getComments());
        Assert.assertTrue(testCollection.getComments().stream().anyMatch(c -> c.equals("_ContigHeader=CONTIG")));
        Assert.assertTrue(testCollection.getComments().stream().anyMatch(c -> c.equals("_StartHeader=START")));
        Assert.assertTrue(testCollection.getComments().stream().anyMatch(c -> c.equals("_EndHeader=END")));
        Assert.assertEquals(testCollection.getRecords(), collection.getRecords());
        Assert.assertEquals(testCollection.getSamFileHeader(), collection.getSamFileHeader());
        Assert.assertEquals(testCollection.getAnnotations(), collection.getAnnotations());
    }

    @Test
    public void testRoundtrip() throws IOException {
        final File outputFile = File.createTempFile("simpleannotatedintervalwriter_", ".tsv");
        final AnnotatedIntervalCollection collection = AnnotatedIntervalCollection.create(TEST_FILE.toPath(), null);

        final SimpleAnnotatedIntervalWriter writer = new SimpleAnnotatedIntervalWriter(outputFile);

        writer.writeHeader(
                new AnnotatedIntervalHeader("CONTIG", "START", "END", collection.getAnnotations(), collection.getSamFileHeader(), collection.getComments()));
        collection.getRecords().forEach(r -> writer.add(r));
        writer.close();

        final AnnotatedIntervalCollection testCollection = AnnotatedIntervalCollection.create(outputFile.toPath(), null);

        // Reminder: In this case, the output will have additional comments for the header.  The input had none.
        Assert.assertEquals(testCollection.getComments().size(), 3);
        Assert.assertTrue(testCollection.getComments().stream().anyMatch(c -> c.equals("_ContigHeader=CONTIG")));
        Assert.assertTrue(testCollection.getComments().stream().anyMatch(c -> c.equals("_StartHeader=START")));
        Assert.assertTrue(testCollection.getComments().stream().anyMatch(c -> c.equals("_EndHeader=END")));
        Assert.assertEquals(testCollection.getRecords(), collection.getRecords());
        Assert.assertEquals(testCollection.getSamFileHeader(), collection.getSamFileHeader());
        Assert.assertEquals(testCollection.getAnnotations(), collection.getAnnotations());
    }
}
