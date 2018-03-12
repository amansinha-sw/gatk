package org.broadinstitute.hellbender.tools.copynumber.utils.annotatedregion;

import com.google.common.collect.ImmutableSortedMap;
import htsjdk.samtools.util.Locatable;
import htsjdk.tribble.Feature;
import org.broadinstitute.hellbender.utils.SimpleInterval;

import java.util.SortedMap;

/**
 * Simple class that just has an interval and sorted name-value pairs.
 */
public final class AnnotatedInterval implements Locatable, Feature {

    private final SimpleInterval interval;
    private final SortedMap<String, String> annotations;

    public AnnotatedInterval(final SimpleInterval interval, final SortedMap<String, String> annotations) {
        this.interval = interval;
        this.annotations = annotations;
    }

    /** Returns a copy */
    public SimpleInterval getInterval() {
        return new SimpleInterval(this.interval);
    }

    @Override
    public String getContig() {
        return interval.getContig();
    }

    @Override
    public int getStart() {
        return interval.getStart();
    }

    @Override
    public int getEnd() {
        return interval.getEnd();
    }

    public String getAnnotationValue(final String annotationName) {
        return annotations.get(annotationName);
    }

    public String getAnnotationValueOrDefault(final String annotationName, final String defaultValue) {
        return annotations.getOrDefault(annotationName, defaultValue);
    }

    /** Returns a copy of the annotations as a map.
     * Dev note: this does not always create a copy.  See {@link ImmutableSortedMap#copyOfSorted(SortedMap)}*/
    public ImmutableSortedMap<String, String> getAnnotations() {
        return ImmutableSortedMap.copyOfSorted(this.annotations);
    }

    public boolean hasAnnotation(final String annotationName) {
        return annotations.containsKey(annotationName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AnnotatedInterval that = (AnnotatedInterval) o;
        return this.interval.equals(that.getInterval()) && this.getAnnotations().equals(that.getAnnotations());
    }

    @Override
    public int hashCode() {
        int result = interval.hashCode();
        result = 31 * result + annotations.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AnnotatedInterval{" +
                "interval=" + interval +
                ", annotations=" + annotations +
                '}';
    }
}
