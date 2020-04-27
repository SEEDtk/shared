/**
 *
 */
package org.theseed.reports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.theseed.io.Shuffler;

import j2html.tags.ContainerTag;
import static j2html.TagCreator.*;

/**
 * This class returns a J2HTML container tag for displaying a portion of a large sequence in HTML as an SVG object.
 * The object consists of an SVG tag containing a title, a reference line, and then arrows representing
 * the features.  The caller includes a color and opacity.
 *
 * The constructor must specify the left and right coordinates of the contig.  The width, margin, and
 * arrow height are tuning parameters. The height of the canvas will be determined automatically.
 *
 * The entire object is represented as a paragraph element.  The first sub-element will be a text label,
 * and the second will be the SVG itself.
 *
 * @author Bruce Parrello
 *
 */
public class HtmlFullSequence implements Iterable<HtmlHitSequence> {

    // FIELDS
    /** canvas width */
    private int width;
    /** canvas margin */
    private int margin;
    /** arrow height */
    private int arrowHeight;
    /** arrowhead width */
    private int headWidth;
    /** left display position of contig */
    private int contigLeft;
    /** right display position of contig */
    private int contigRight;
    /** list of features to display, sorted by location */
    private SortedSet<HtmlHitSequence> features;
    /** scale factor for coordinates */
    private double scaleFactor;
    /** title for this contig */
    private String title;

    /**
     * Construct a new HTML contig display object.
     *
     * @param left		left position to show on contig
     * @param right		right position to show on contig
     * @param title		title to display above contig
     */
    public HtmlFullSequence(int left, int right, String title) {
        this.title = title;
        this.contigLeft = left;
        this.contigRight = right;
        // Specify defaults
        this.width = 1000;
        this.margin = 5;
        this.arrowHeight = 20;
        this.headWidth = 10;
        computeScale();
        // Initialize the feature list.
        this.features = new TreeSet<HtmlHitSequence>();
    }

    /**
     * Add a feature to this contig.
     *
     * @param feat	feature to add
     */
    public HtmlFullSequence add(HtmlHitSequence feat) {
        this.features.add(feat);
        return this;
    }

    /**
     * Compute the scale factor.
     */
    private void computeScale() {
        this.scaleFactor = ((double) (this.width - 2 * this.margin)) / (this.contigRight - this.contigLeft);
    }

    /**
     * @return the canvas width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Specify a new canvas width.
     *
     * @param width the width to set
     */
    public HtmlFullSequence setWidth(int width) {
        this.width = width;
        computeScale();
        return this;
    }

    /**
     * @return the margin
     */
    public int getMargin() {
        return margin;
    }

    /**
     * Specify a new margin.
     *
     * @param margin the margin to set
     */
    public HtmlFullSequence setMargin(int margin) {
        this.margin = margin;
        computeScale();
        return this;
    }

    /**
     * @return the arrow height
     */
    public int getArrowHeight() {
        return arrowHeight;
    }

    /**
     * Specify a new arrow height.
     *
     * @param arrowHeight the arrow height to set
     */
    public HtmlFullSequence setArrowHeight(int arrowHeight) {
        this.arrowHeight = arrowHeight;
        return this;
    }

    /**
     * @return the canvas coordinate for the specified location on the contig.
     *
     * @param x		location to convert to a canvas coordinate
     */
    public int xPos(int x) {
        int retVal = (int) Math.round((x - this.contigLeft) * this.scaleFactor);
        return retVal;
    }

    /**
     * @return the arrowhead width
     */
    public int getHeadWidth() {
        return headWidth;
    }

    /**
     * @param headWidth 	the arrowhead width to set
     */
    public void setHeadWidth(int headWidth) {
        this.headWidth = headWidth;
    }

    /**
     * Compute the appropriate display level for each feature.
     *
     * @return the number of levels assigned
     */
    protected int assignLevels() {
        List<List<HtmlHitSequence>> levels = new ArrayList<List<HtmlHitSequence>>();
        // Assign the first level to the first feature.
        Iterator<HtmlHitSequence> iter = this.features.iterator();
        if (iter.hasNext()) {
            HtmlHitSequence feat = iter.next();
            feat.setLevel(0);
            levels.add(new Shuffler<HtmlHitSequence>(this.features.size()).add1(feat));
            // Loop through the rest of the features.
            while (iter.hasNext()) {
                final HtmlHitSequence f = iter.next();
                // Find a safe level to contain this feature.
                boolean found = false;
                for (int i = 0; ! found && i < levels.size(); i++) {
                    List<HtmlHitSequence> level = levels.get(i);
                    found = level.stream().allMatch(x -> ! x.isOverlapping(f));
                    if (found) {
                        // The feature belongs here.
                        level.add(f);
                        feat.setLevel(i);
                    }
                }
                if (! found) {
                    // The feature belongs on a new level.
                    f.setLevel(levels.size());
                    levels.add(new Shuffler<HtmlHitSequence>(5).add1(f));
                }
            }
        }
        return levels.size();
    }

    /**
     * @return an HTML tag displaying this contig
     *
     * @param linker	object to generate links to features
     */
    public ContainerTag draw(LinkObject linker) {
        // Insure every arrow has a level.
        int levels = this.assignLevels();
        // Create the canvas.
        ContainerTag canvas = new ContainerTag("svg").attr("width", this.width)
                .attr("height", 2 * this.margin + levels * (this.margin + this.arrowHeight));
        // Add the contig line.
        canvas.with(new ContainerTag("line").attr("x1", this.margin).attr("y1", this.margin)
                .attr("x2", this.width - this.margin).attr("y2", this.margin)
                .withStyle("stroke:black;stroke-width:4"));
        // Add the features.
        for (HtmlHitSequence feat : this)
            canvas.with(feat.draw(this, linker));
        // Return the whole thing with the title.
        ContainerTag retVal = p(text(this.title), br(), canvas)
                .withStyle("text-align: center");
        return retVal;
    }

    @Override
    public Iterator<HtmlHitSequence> iterator() {
        return this.features.iterator();
    }




}
