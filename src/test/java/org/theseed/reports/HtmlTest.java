/**
 *
 */
package org.theseed.reports;

import junit.framework.TestCase;
import static org.theseed.reports.HtmlUtilities.*;
import static j2html.TagCreator.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

/**
 * @author Bruce Parrello
 *
 */
public class HtmlTest extends TestCase {

    public void testJoin() {
        String test1 = joinDelimited(Arrays.asList(b("item 1"), code("item 2"), span("item 3")), br()).render();
        assertThat(test1, equalTo("<b>item 1</b> <br> <code>item 2</code> <br> <span>item 3</span>"));
        test1 = joinDelimited(Arrays.asList(b("item 1"), code("item 2"), span("item 3")), ",").render();
        assertThat(test1, equalTo("<b>item 1</b>, <code>item 2</code>, <span>item 3</span>"));
    }
}
