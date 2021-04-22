/**
 *
 */
package org.theseed.reports;

import static j2html.TagCreator.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import j2html.tags.ContainerTag;
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
     * @param element	element to hyperlink
     */
    public abstract ContainerTag genomeLink(String genomeId, DomContent element);

    /**
     * @return a hyperlink to the specified genome's overview page
     *
     * @param genomeId	ID of the genome to view
     */
    public ContainerTag genomeLink(String genomeId) {
        return genomeLink(genomeId, text(genomeId));
    }

    /**
     * @return a hyperlink to the specified feature's overview page
     *
     * @param fid	ID of the feature to view
     */
    public ContainerTag featureLink(String fid) {
        return featureLink(fid, text(fid));
    }

    /**
     * @return a hyperlink to the specified feature's overview page
     *
     * @param fid		ID of the feature to view
     * @param element	element to hyperlink
     */
    public abstract ContainerTag featureLink(String fid, DomContent element);

    /**
     * @return a hyperlink to the specified feature's context page
     *
     * @param fid	ID of the feature to view
     */
    public abstract ContainerTag featureRegionLink(String fid);

    /**
     * @return a hyperlink to a page listing the specified features
     *
     * @param fidList	IDs of the features to list
     */
    public abstract ContainerTag featureListLink(Collection<String> fidList);

    /**
     * URLs for PATRIC genomes
     */
    public static class Patric extends LinkObject {

        private static final String GENOME_VIEW_LINK = "https://www.patricbrc.org/view/Genome/%s";

        private static final String FEATURE_CR_LINK = "https://www.patricbrc.org/view/Feature/%s#view_tab=compareRegionViewer";

        private static final String FEATURE_VIEW_LINK = "https://www.patricbrc.org/view/Feature/%s";

        private static final String FEATURE_LIST_LINK = "https://www.patricbrc.org/view/FeatureList/?in(patric_id,(\"%s\"))";

        @Override
        public ContainerTag genomeLink(String genomeId, DomContent element) {
            return a(element).withHref(String.format(GENOME_VIEW_LINK, genomeId))
                    .withTarget("_blank");
        }

        @Override
        public ContainerTag featureLink(String fid, DomContent element) {
            return a(element).withHref(String.format(FEATURE_VIEW_LINK, fid))
                    .withTarget("_blank");
        }

        @Override
        public ContainerTag featureRegionLink(String fid) {
            return a(fid).withHref(String.format(FEATURE_CR_LINK, fid))
                    .withTarget("_blank");
        }

        @Override
        public ContainerTag featureListLink(Collection<String> fidList) {
            ContainerTag retVal = null;
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

        private static final String GENOME_VIEW_LINK = "https://core.theseed.org/FIG/seedviewer.cgi?page=Organism;organism=%s";

        private static final String FEATURE_VIEW_LINK = "https://core.theseed.org/FIG/seedviewer.cgi?page=Annotation;feature=%s";

        /** subsystem link format */
        public static final String SUBSYSTEM_LINK = "https://core.theseed.org/FIG/seedviewer.cgi?page=Subsystems;subsystem=%s";

        @Override
        public ContainerTag genomeLink(String genomeId, DomContent element) {
            return a(element).withHref(String.format(GENOME_VIEW_LINK, genomeId))
                    .withTarget("_blank");
        }

        @Override
        public ContainerTag featureLink(String fid, DomContent element) {
            return a(element).withHref(String.format(FEATURE_VIEW_LINK, fid))
                    .withTarget("_blank");
        }

        @Override
        public ContainerTag featureRegionLink(String fid) {
            return featureLink(fid);
        }


        @Override
        public ContainerTag featureListLink(Collection<String> fidList) {
            ContainerTag retVal = null;
            if (fidList.size() == 1) {
                retVal = featureLink(fidList.iterator().next());
            } else {
                retVal = span(String.format("%d features", fidList.size()));
            }
            return retVal;
        }

        /**
         * @return a link to the CoreSEED page for the subsystem with the specified name
         *
         * @param ssName	name of the subsystem
         */
        public static ContainerTag subsystemLink(String ssName) {
            String ssId = cleanSubsystemName(ssName);
            return a(ssName).withHref(String.format(SUBSYSTEM_LINK, ssId)).withTarget("_blank");
        }

        /**
         * @return a subsystem name suitable for appearing in a link
         *
         * @param ssName	subsystem name to convert
         */
        public static String cleanSubsystemName(String ssName) {
            String ssId = ssName.replace(' ', '_');
            try {
                ssId = URLEncoder.encode(ssId, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return ssId;
        }

    }

    /**
     * URLs for homeless genomes
     */
    public static class None extends LinkObject {

        @Override
        public ContainerTag genomeLink(String genomeId, DomContent element) {
            return span(element);
        }

        @Override
        public ContainerTag genomeLink(String genomeId) {
            return span(genomeId);
        }

        @Override
        public ContainerTag featureLink(String fid) {
            return span(fid);
        }

        @Override
        public ContainerTag featureLink(String fid, DomContent element) {
            return span(element);
        }

        @Override
        public ContainerTag featureRegionLink(String fid) {
            return span(fid);
        }

        @Override
        public ContainerTag featureListLink(Collection<String> fidList) {
            ContainerTag retVal = null;
            if (fidList.size() == 1) {
                retVal = featureLink(fidList.iterator().next());
            } else {
                retVal = span(String.format("%d features", fidList.size()));
            }
            return retVal;
        }

    }
}
