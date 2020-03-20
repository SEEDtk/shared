/**
 *
 */
package org.theseed.genome;

import com.github.cliftonlabs.json_simple.JsonArray;

/**
 * This represents a feature annotation.  A list of these is maintained in the feature object.
 *
 * @author Bruce Parrello
 *
 */
public class Annotation {

    // FIELDS
    /** description of the annotation event */
    private String comment;
    /** person responsible for the annotation */
    private String annotator;
    /** time of the event */
    private double annotationTime;


    /**
     * Create an annotation.
     *
     * @param comment		annotation comment
     * @param annotator		name of the responsible annotator
     */
    public Annotation(String comment, String annotator) {
        this.comment = comment;
        this.annotator = annotator;
        this.annotationTime = System.currentTimeMillis() / 1000.0;
    }

    /**
     * Construct an annotation from a JSON object.  Annotations are tuples, so they are
     * read in as JSON arrays.
     *
     * @param json	source JSON array
     */
    public Annotation(JsonArray json) {
        JsonArray array = (JsonArray) json;
        this.comment = array.getString(0);
        this.annotator = array.getString(1);
        this.annotationTime = array.getDouble(2);
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the annotator name
     */
    public String getAnnotator() {
        return annotator;
    }

    /**
     * @return the annotation time
     */
    public double getAnnotationTime() {
        return annotationTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(annotationTime);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((annotator == null) ? 0 : annotator.hashCode());
        result = prime * result + ((comment == null) ? 0 : comment.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Annotation other = (Annotation) obj;
        if (Double.doubleToLongBits(annotationTime) != Double.doubleToLongBits(other.annotationTime))
            return false;
        if (annotator == null) {
            if (other.annotator != null)
                return false;
        } else if (!annotator.equals(other.annotator))
            return false;
        if (comment == null) {
            if (other.comment != null)
                return false;
        } else if (!comment.equals(other.comment))
            return false;
        return true;
    }


}
