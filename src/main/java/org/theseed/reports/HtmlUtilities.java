/**
 *
 */
package org.theseed.reports;

import j2html.tags.DomContent;
import static j2html.TagCreator.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

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
    private static DomContent joinDelimitedInternal(Stream<DomContent> list, Object sep) {
        List<Object> parts = new ArrayList<Object>(50);
        Iterator<DomContent> iter = list.iterator();
        if (iter.hasNext()) {
            DomContent item = iter.next();
            parts.add(item);
            while (iter.hasNext()) {
                item = iter.next();
                parts.add(sep);
                parts.add(item);
            }
        }
        return join(parts.toArray());
    }

    /**
     * @return a textual HTML object containing the elements of a collection separated by a delimiter
     *
     * @param list	stream of HTML entities to join
     * @param sep	separator (String)
     */
    public static DomContent joinDelimited(Stream<DomContent> list, String sep) {
        return joinDelimitedInternal(list, sep);
    }

    /**
     * @return a textual HTML object containing the elements of a collection separated by a delimiter
     *
     * @param list	collection of HTML entities to join
     * @param sep	separator (String)
     */
    public static DomContent joinDelimited(Collection<DomContent> list, String sep) {
        return joinDelimitedInternal(list.stream(), sep);
    }

    /**
     * @return a textual HTML object containing the elements of a collection separated by a delimiter
     *
     * @param list	collection of HTML entities to join
     * @param sep	separator (DomContent)
     */
    public static DomContent joinDelimited(Collection<DomContent> list, DomContent sep) {
        return joinDelimitedInternal(list.stream(), sep);
    }

}
