/**
 *
 */
package org.theseed.sequence;

import org.theseed.genome.Contig;

/**
 * This class represents a record from a FASTA file.  It contains a sequence,
 * a label, and a comment.
 *
 * @author Bruce Parrello
 *
 */
public class Sequence {

    // FIELDS
    String label;
    String comment;
    String sequence;

    /**
     * Construct a blank, empty sequence.
     */
    public Sequence() {
        this.label = "";
        this.comment = "";
        this.sequence = "";
    }

    /**
     * Construct a sequence from known strings.
     *
     * @param label		the label to assign
     * @param comment	the command about the sequence
     * @param sequence	the sequence text
     */
    public Sequence(String label, String comment, String sequence) {
        super();
        this.label = label;
        this.comment = comment;
        this.sequence = sequence;
    }

    /**
     * @return the length of the sequence
     */
    public int length() {
        return this.sequence.length();
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return the sequence
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * @param sequence the sequence to set
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    /**
     * @return a new sequence object with the reverse complement of this one (DNA sequences only, obviously)
     */
    public Sequence reverse() {
        String rev = Contig.reverse(this.sequence);
        return new Sequence(this.label, this.comment, rev);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.comment == null) ? 0 : this.comment.hashCode());
        result = prime * result + ((this.label == null) ? 0 : this.label.hashCode());
        result = prime * result + ((this.sequence == null) ? 0 : this.sequence.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (! (obj instanceof Sequence))
            return false;
        Sequence other = (Sequence) obj;
        if (this.comment == null) {
            if (other.comment != null)
                return false;
        } else if (!this.comment.equals(other.comment))
            return false;
        if (this.label == null) {
            if (other.label != null)
                return false;
        } else if (!this.label.equals(other.label))
            return false;
        if (this.sequence == null) {
            if (other.sequence != null)
                return false;
        } else if (!this.sequence.equals(other.sequence))
            return false;
        return true;
    }

}
