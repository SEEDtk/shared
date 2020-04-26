/**
 *
 */
package org.theseed.reports;

import static j2html.TagCreator.*;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import j2html.tags.DomContent;

/**
 * This is an abstract class that's used to generate genome and feature URLs based on the
 * genome's home.  The home can be PATRIC, CORE, or NONE.
 *
 * @author Bruce Parrello
 *
 */
public abstract class LinkObject {

    /**
     * @return a hyperlink to the specified genome's overview page
     *
     * @param genomeId	ID of the genome to view
     */
    public abstract DomContent genomeLink(String genomeId);

    /**
     * @return a hyperlink to the specified feature's overview page
     *
     * @param fid	ID of the feature to view
     */
    public DomContent featureLink(String fid) {
        return featureLink(fid, text(fid));
    }

    /**
     * @return a hyperlink to the specified feature's overview page
     *
     * @param fid		ID of the feature to view
     * @param element	element to hyperlink
     */
    public abstract DomContent featureLink(String fid, DomContent element);

    /**
     * @return a hyperlink to the specified feature's context page
     *
     * @param fid	ID of the feature to view
     */
    public abstract DomContent featureRegionLink(String fid);

    /**
     * @return a hyperlink to a page listing the specified features
     *
     * @param fidList	IDs of the features to list
     */
    public abstract DomContent featureListLink(Collection<String> fidList);

    /**
     * URLs for PATRIC genomes
     */
    public static class Patric extends LinkObject {

        private static final String GENOME_VIEW_LINK = "https://www.patricbrc.org/view/Genome/%s";

        private static final String FEATURE_CR_LINK = "https://www.patricbrc.org/view/Feature/%s#view_tab=compareRegionViewer";

        private static final String FEATURE_VIEW_LINK = "https://www.patricbrc.org/view/Feature/%s";

        private static final String FEATURE_LIST_LINK = "https://www.patricbrc.org/view/FeatureList/?in(patric_id,(\"%s\"))";

        @Override
        public DomContent genomeLink(String genomeId) {
            return a(genomeId).withHref(String.format(GENOME_VIEW_LINK, genomeId))
                    .withTarget("_blank");
        }

        @Override
        public DomContent featureLink(String fid, DomContent element) {
            return a(element).withHref(String.format(FEATURE_VIEW_LINK, fid))
                    .withTarget("_blank");
        }

        @Override
        public DomContent featureRegionLink(String fid) {
            return a(fid).withHref(String.format(FEATURE_CR_LINK, fid))
                    .withTarget("_blank");
        }

        @Override
        public DomContent featureListLink(Collection<String> fidList) {
            DomContent retVal = null;
            if (fidList.size() == 1) {
                // Only one feature.  We go to the feature landing page and display the feature ID.
                String fid = fidList.iterator().next();
                retVal = this.featureLink(fid);
            } else {
                // Multiple features.  We go to a feature list view.  This requires the feature IDs to be enclosed in quotes.
                String rawUrl = String.format(FEATURE_LIST_LINK, StringUtils.join(fidList, "\",\""));
                // We also have to URLEncode the vertical bars.
                String linkUrl = StringUtils.replace(rawUrl, "|", "%7c");
                // Apply the URL to the text.
                String linkText = String.format("%d features", fidList.size());
                retVal = a(linkText).withHref(linkUrl).withTarget("_blank");
            }
            return retVal;
        }

    }

    /**
     * URLs for CoreSEED genomes
     */
    public static class Core extends LinkObject {

        private static final String GENOME_VIEW_LINK = "https://core.theseed.org/FIG/seedviewer.cgi?page=Organism&organism=%s";

        private static final String FEATURE_VIEW_LINK = "https://core.theseed.org/FIG/seedviewer.cgi?page=Annotation&feature=%s";


        @Override
        public DomContent genomeLink(String genomeId) {
            return a(genomeId).withHref(String.format(GENOME_VIEW_LINK, genomeId))
                    .withTarget("_blank");
        }

        @Override
        public DomContent featureLink(String fid, DomContent element) {
            return a(element).withHref(String.format(FEATURE_VIEW_LINK, fid))
                    .withTarget("_blank");
        }

        @Override
        public DomContent featureRegionLink(String fid) {
            return featureLink(fid);
        }


        @Override
        public DomContent featureListLink(Collection<String> fidList) {
            DomContent retVal = null;
            if (fidList.size() == 1) {
                retVal = featureLink(fidList.iterator().next());
            } else {
                retVal = text(String.format("%d features", fidList.size()));
            }
            return retVal;
        }

    }

    /**
     * URLs for homeless genomes
     */
    public static class None extends LinkObject {

        @Override
        public DomContent genomeLink(String genomeId) {
            return text(genomeId);
        }

        @Override
        public DomContent featureLink(String fid, DomContent element) {
            return element;
        }

        @Override
        public DomContent featureRegionLink(String fid) {
            return text(fid);
        }

        @Override
        public DomContent featureListLink(Collection<String> fidList) {
            DomContent retVal = null;
            if (fidList.size() == 1) {
                retVal = featureLink(fidList.iterator().next());
            } else {
                retVal = text(String.format("%d features", fidList.size()));
            }
            return retVal;
        }

    }
}
