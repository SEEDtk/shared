/**
 *
 */
package org.theseed.reports;

import org.theseed.locations.Location;
import org.theseed.reports.Color;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

/**
 * This represents a feature on an HTML contig.  For each feature we need to know the color,
 * location, ID, and label.  The comparison is by location.
 *
 * @author Bruce Parrello
 *
 */
public class HtmlFeature implements Comparable<HtmlFeature> {

    // FIELDS
    /** ID of this feature */
    private String id;
    /** label of this feature (does not include ID) */
    private String label;
    /** location of this feature */
    private Location loc;
    /** color to give to this feature */
    private Color color;
    /** vertical level of this feature */
    private int level;
    /** format for arrow points */
    private final String POINT_FMT = "%d,%d %d,%d %d,%d %d,%d %d,%d";

    /**
     * Construct a feature.
     *
     * @param id		ID of this feature
     * @param label		label to display when hovering over the feature
     * @param loc		location of the feature on the contig
     * @param color		fill color for this feature
     */
    public HtmlFeature(String id, String label, Location loc, Color color) {
        this.id = id;
        this.label = label;
        this.loc = loc;
        this.color = color;
    }

    /**
     * @return the tag for rendering this feature's arrow.
     *
     * @param parent	parent contig canvas
     * @param linker
     */
    public DomContent draw(HtmlContig parent, LinkObject linker) {
        int left = parent.xPos(this.loc.getLeft());
        int right = parent.xPos(this.loc.getRight());
        int top = parent.getMargin() * 2 + (parent.getMargin() + parent.getArrowHeight()) * this.level;
        int bottom = top + parent.getArrowHeight();
        int pointY = (top + bottom) / 2;
        String points;
        if (this.loc.getDir() == '+') {
            int headLeft = Math.max(left, right - parent.getHeadWidth());
            points = String.format(POINT_FMT, left, top, headLeft, top, right, pointY,
                headLeft, bottom, left, bottom);
        } else {
            int headRight = Math.min(right, left + parent.getHeadWidth());
            points = String.format(POINT_FMT, right, top, headRight, top, left, pointY,
                    headRight, bottom, right, bottom);
        }
        String colorString = this.color.html();
        DomContent element = new ContainerTag("polygon").attr("points", points).attr("fill", colorString)
                .with(new ContainerTag("title").withText(this.id + " " + this.label));
        DomContent retVal = linker.featureLink(this.id, element);
        return retVal;
    }


    @Override
    public int compareTo(HtmlFeature o) {
        int retVal = this.loc.compareTo(o.loc);
        if (retVal == 0)
            retVal = this.id.compareTo(o.id);
        return retVal;
    }

    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Specify a new arrow level
     *
     * @param level the level to set
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * @return TRUE if this feature overlaps the other feature
     *
     * @param o		other feature to compare
     */
    public boolean isOverlapping(HtmlFeature o) {
        return this.loc.isOverlapping(o.loc);
    }

    @Override
    public String toString() {
        return "HtmlFeature [id=" + id + ", loc=" + loc + ", level=" + level + "]";
    }
}
