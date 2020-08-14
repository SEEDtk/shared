/**
 *
 */
package org.theseed.reports;

import j2html.tags.DomContent;
import static j2html.TagCreator.*;

import java.util.Collection;

/**
 * This is a static class that contains useful HTML-related methods.
 *
 * @author Bruce Parrello
 *
 */
public class HtmlUtilities {

    /**
     * @return a textual HTML object containing the elements of a collection separated by a delimiter
     *
     * @param list	collection of HTML entities to join
     * @param sep	separator (may be String or DomContent)
     */
    private static DomContent joinDelimitedInternal(Collection<DomContent> list, Object sep) {
        Object[] retVal = new Object[list.size() * 2 - 1];
        int idx = 0;
        for (DomContent item : list) {
            if (idx > 0)
                retVal[idx++] = sep;
            retVal[idx++] = item;
        }
        return join(retVal);
    }

    /**
     * @return a textual HTML object containing the elements of a collection separated by a delimiter
     *
     * @param list	collection of HTML entities to join
     * @param sep	separator (String)
     */
    public static DomContent joinDelimited(Collection<DomContent> list, String sep) {
        return joinDelimitedInternal(list, sep);
    }

    /**
     * @return a textual HTML object containing the elements of a collection separated by a delimiter
     *
     * @param list	collection of HTML entities to join
     * @param sep	separator (DomContent)
     */
    public static DomContent joinDelimited(Collection<DomContent> list, DomContent sep) {
        return joinDelimitedInternal(list, sep);
    }

}
