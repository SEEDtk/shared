/**
 *
 */
package org.theseed.reports;

import org.junit.jupiter.api.Test;

import static j2html.TagCreator.b;
import static j2html.TagCreator.br;
import static j2html.TagCreator.code;
import static j2html.TagCreator.span;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;

import static org.theseed.reports.HtmlUtilities.joinDelimited;

import org.theseed.locations.Location;

/**
 * Test the HTML features.
 *
 * @author Bruce Parrello
 *
 */
public class HtmlTests {

    @Test
    public void testJoin() {
        String test1 = joinDelimited(Arrays.asList(b("item 1"), code("item 2"), span("item 3")), br()).render();
        assertThat(test1, equalTo("<b>item 1</b> <br> <code>item 2</code> <br> <span>item 3</span>"));
        test1 = joinDelimited(Arrays.asList(b("item 1"), code("item 2"), span("item 3")), ",").render();
        assertThat(test1, equalTo("<b>item 1</b>, <code>item 2</code>, <span>item 3</span>"));
    }

    @Test
    public void testColors() {
        Color color = Color.WHITE.darken(0.79);
        assertThat(color.html(), equalTo("#363636"));
        color = Color.RED.brighten(0.5);
        assertThat(color.html(), equalTo("#FF8080"));
    }

    @Test
    public void testHtmlContig() {
        LinkObject linker = new LinkObject.Patric();
        HtmlFullSequence hContig = new HtmlFullSequence(1000, 2000, "Test contig");
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.211", "first feature", Location.create("c1", 100, 200), '+', Color.GREEN));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.212", "second feature", Location.create("c1", 500, 1100), '+', Color.RED));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.213", "third feature", Location.create("c1", 1200, 1050), '-', Color.BLUE));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.214", "fourth feature", Location.create("c1", 1150, 1250), '+', Color.BLACK));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.215", "fifth feature", Location.create("c1", 1025, 1300), '+', Color.ORANGE));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.216", "sixth feature", Location.create("c1", 1300, 1100), '-', Color.PINK));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.217", "little feature", Location.create("c1", 1110, 1120), '+', Color.GRAY));
        hContig.add(new HtmlHitSequence("fig|1705389.3.peg.218", "little back feature", Location.create("c1", 1130, 1121), '-', Color.GRAY));
        int max = hContig.assignLevels();
        for (HtmlHitSequence f1 : hContig) {
            for (HtmlHitSequence f2 : hContig) {
                if (f1 != f2 && f1.isOverlapping(f2))
                    assertThat(String.format("%s vs %s", f1, f2), f1.getLevel(), not(equalTo(f2.getLevel())));
                assertThat(f1.toString(), f1.getLevel(), lessThan(max));
            }
        }
        String html = hContig.draw(linker).render();
        assertThat(html, equalTo("<p style=\"text-align: center\">Test contig<br><svg width=\"1000\" height=\"110\"><line x1=\"5\" y1=\"5\" x2=\"995\" y2=\"5\" style=\"stroke:black;stroke-width:4\"></line><polygon points=\"-886,10 -797,10 -787,20 -797,30 -886,30\" fill=\"#00FF00\"><title>fig|1705389.3.peg.211 first feature</title></polygon><polygon points=\"-490,10 94,10 104,20 94,30 -490,30\" fill=\"#FF0000\"><title>fig|1705389.3.peg.212 second feature</title></polygon><polygon points=\"30,35 292,35 302,45 292,55 30,55\" fill=\"#FFC700\"><title>fig|1705389.3.peg.215 fifth feature</title></polygon><polygon points=\"203,60 65,60 55,70 65,80 203,80\" fill=\"#0000FF\"><title>fig|1705389.3.peg.213 third feature</title></polygon><polygon points=\"302,85 114,85 104,95 114,105 302,105\" fill=\"#FFADAD\"><title>fig|1705389.3.peg.216 sixth feature</title></polygon><polygon points=\"114,10 114,10 124,20 114,30 114,30\" fill=\"#808080\"><title>fig|1705389.3.peg.217 little feature</title></polygon><polygon points=\"134,10 134,10 125,20 134,30 134,30\" fill=\"#808080\"><title>fig|1705389.3.peg.218 little back feature</title></polygon><polygon points=\"154,10 243,10 253,20 243,30 154,30\" fill=\"#000000\"><title>fig|1705389.3.peg.214 fourth feature</title></polygon></svg></p>"));
    }

}
