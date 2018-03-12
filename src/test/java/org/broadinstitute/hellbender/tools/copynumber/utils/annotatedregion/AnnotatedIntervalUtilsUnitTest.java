package org.broadinstitute.hellbender.tools.copynumber.utils.annotatedregion;

import com.google.common.collect.ImmutableSortedMap;
import org.broadinstitute.hellbender.GATKBaseTest;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.reference.ReferenceUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class AnnotatedIntervalUtilsUnitTest extends GATKBaseTest {
    @Test(dataProvider = "mergeTests")
    public void testMerge(List<AnnotatedInterval> test1, List<AnnotatedInterval> gt) {
        List<AnnotatedInterval> mergeTestResults = AnnotatedIntervalUtils.mergeRegions(test1,
                ReferenceUtils.loadFastaDictionary(new File(ReferenceUtils.getFastaDictionaryFileName(hg19MiniReference))),
                "__", l -> {});
        Assert.assertEquals(mergeTestResults, gt);
    }

    @Test(dataProvider = "copyAnnotatedIntervalTests")
    public void testCopyAnnotatedInterval(AnnotatedInterval test1, List<String> annotationsToPreserve, AnnotatedInterval gt) {
        final AnnotatedInterval copy = AnnotatedIntervalUtils.copyAnnotatedInterval(test1, new HashSet<>(annotationsToPreserve));
        Assert.assertEquals(copy, gt);
        Assert.assertFalse(copy == test1);
        Assert.assertFalse(copy.getAnnotations() == test1.getAnnotations());
        Assert.assertFalse(copy.getInterval() == test1.getInterval());
    }

    @DataProvider(name = "copyAnnotatedIntervalTests")
    public Object [][] createCopyAnnotatedIntervalTests() {
        return new Object[][] {
                {
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        Arrays.asList("Foo", "Foo1"),
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),

                },
                {
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        Arrays.asList("Foo"),
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar")),

                },
                {
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        Arrays.asList("Foo","Not_Present"),
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar")),

                },
                {
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        Collections.emptyList(),
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of()),

                },
                {
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        Arrays.asList("Not_Present", "Not_Present2", "Not_Present3"),
                        new AnnotatedInterval(new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of()),

                }
        };
    }

    @DataProvider(name = "mergeTests")
    public Object [][] createMergeTests() {

        return new Object[][] {
            {
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1"))),
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")))
            }, {
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar2")),
                        new AnnotatedInterval( new SimpleInterval("1", 201, 300),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1"))),
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar2")),
                        new AnnotatedInterval( new SimpleInterval("1", 201, 300),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")))
            }, {
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        new AnnotatedInterval( new SimpleInterval("2", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foobar1", "bar1"))),
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        new AnnotatedInterval( new SimpleInterval("2", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foobar1", "bar1")))
            }, {
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar2")),
                        new AnnotatedInterval( new SimpleInterval("1", 190, 300),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        new AnnotatedInterval( new SimpleInterval("1", 301, 500),
                                ImmutableSortedMap.of("Foo2", "bar", "Foo3", "bar1"))),
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 300),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1__bar2")),
                        new AnnotatedInterval( new SimpleInterval("1", 301, 500),
                                ImmutableSortedMap.of("Foo2", "bar", "Foo3", "bar1")))
            }, {
                Arrays.asList(
                        // Same as previous test, but trying to see if input order hoses things.  Note that the output will
                        //  be sorted.
                        new AnnotatedInterval( new SimpleInterval("1", 190, 300),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1")),
                        new AnnotatedInterval( new SimpleInterval("1", 100, 200),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar2")),
                        new AnnotatedInterval( new SimpleInterval("1", 301, 500),
                                ImmutableSortedMap.of("Foo2", "bar", "Foo3", "bar1"))),
                Arrays.asList(
                        new AnnotatedInterval( new SimpleInterval("1", 100, 300),
                                ImmutableSortedMap.of("Foo", "bar", "Foo1", "bar1__bar2")),
                        new AnnotatedInterval( new SimpleInterval("1", 301, 500),
                                ImmutableSortedMap.of("Foo2", "bar", "Foo3", "bar1")))
            }
        };
    }

}
