/**
 *
 */
package org.theseed.reports;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.theseed.locations.Location;

/**
 * Test the HTML features.
 *
 * @author Bruce Parrello
 *
 */
public class HtmlTests extends TestCase {

    public void testColors() {
        Color color = Color.WHITE.darken(0.79);
        assertThat(color.html(), equalTo("#363636"));
        color = Color.RED.brighten(0.5);
        assertThat(color.html(), equalTo("#FF8080"));
    }

    public void testHtmlContig() {
        LinkObject linker = new LinkObject.Patric();
        HtmlFullSequence hContig = new HtmlFullSequence(1000, 2000, "Test contig");
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.211", "first feature", Location.create("c1", 100, 200), Color.GREEN));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.212", "second feature", Location.create("c1", 500, 1100), Color.RED));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.213", "third feature", Location.create("c1", 1200, 1050), Color.BLUE));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.214", "fourth feature", Location.create("c1", 1150, 1250), Color.BLACK));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.215", "fifth feature", Location.create("c1", 1025, 1300), Color.ORANGE));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.216", "sixth feature", Location.create("c1", 1300, 1100), Color.PINK));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.217", "little feature", Location.create("c1", 1110, 1120), Color.GRAY));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.218", "little back feature", Location.create("c1", 1130, 1121), Color.GRAY));
        int max = hContig.assignLevels();
        for (HtmlHitSequence f1 : hContig) {
            for (HtmlHitSequence f2 : hContig) {
                if (f1 != f2 && f1.isOverlapping(f2))
                    assertThat(String.format("%s vs %s", f1, f2), f1.getLevel(), not(equalTo(f2.getLevel())));
                assertThat(f1.toString(), f1.getLevel(), lessThan(max));
            }
        }
        String html = hContig.draw(linker).renderFormatted();
        System.out.println(html);
    }

}
