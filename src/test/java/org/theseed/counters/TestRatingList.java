/**
 *
 */
package org.theseed.counters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestRatingList {

    @Test
    public void testRatingList() throws IOException {
        RatingList<String> ratings = new RatingList<String>(10);
        assertThat(ratings.size(), equalTo(0));
        ratings.add("A100", 100.0);
        assertThat(ratings.size(), equalTo(1));
        ratings.add("B200", 200.0);
        assertThat(ratings.getBest(), contains("B200", "A100"));
        ratings.add("C300", 300.0);
        assertThat(ratings.getBest(), contains("C300", "B200", "A100"));
        ratings.add("D250", 250.0);
        ratings.add("E250", 250.0);
        assertThat(ratings.getBest(), contains("C300", "D250", "E250", "B200", "A100"));
        ratings.add("F050",  50.0);
        ratings.add("G070",  70.0);
        ratings.add("H060",  60.0);
        ratings.add("I050",  50.0);
        ratings.add("J050",  50.0);
        ratings.add("K050",  50.0);
        assertThat(ratings.getBest(), contains("C300", "D250", "E250", "B200", "A100",
                "G070", "H060", "F050", "I050", "J050",
                "K050"));
        ratings.add("L150", 150.0);
        assertThat(ratings.getBest(), contains("C300", "D250", "E250", "B200", "L150",
                "A100", "G070", "H060", "F050", "I050",
                "J050", "K050"));
        ratings.add("M010",  10.0);
        ratings.add("N010",  10.0);
        assertThat(ratings.getBest(), contains("C300", "D250", "E250", "B200", "L150",
                "A100", "G070", "H060", "F050", "I050",
                "J050", "K050"));
        ratings.add("O120", 120.0);
        ratings.add("P220", 220.0);
        assertThat(ratings.getBest(), contains("C300", "D250", "E250", "P220", "B200",
                "L150", "O120", "A100", "G070", "H060"));
        Iterator<Rating<String>> iter = ratings.iterator();
        assertThat(iter.hasNext(), equalTo(true));
        Rating<String> curr = iter.next();
        assertThat(curr.getKey(), equalTo("C300"));
        assertThat(curr.getRating(), equalTo(300.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("D250"));
        assertThat(curr.getRating(), equalTo(250.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("E250"));
        assertThat(curr.getRating(), equalTo(250.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("P220"));
        assertThat(curr.getRating(), equalTo(220.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("B200"));
        assertThat(curr.getRating(), equalTo(200.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("L150"));
        assertThat(curr.getRating(), equalTo(150.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("O120"));
        assertThat(curr.getRating(), equalTo(120.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("A100"));
        assertThat(curr.getRating(), equalTo(100.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("G070"));
        assertThat(curr.getRating(), equalTo(70.0));
        assertThat(iter.hasNext(), equalTo(true));
        curr = iter.next();
        assertThat(curr.getKey(), equalTo("H060"));
        assertThat(curr.getRating(), equalTo(60.0));
        assertThat(iter.hasNext(), equalTo(false));
    }

}
