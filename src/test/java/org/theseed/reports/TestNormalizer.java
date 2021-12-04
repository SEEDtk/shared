/**
 *
 */
package org.theseed.reports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestNormalizer {

    @Test
    void testMaxStuff() {
        RangeNormalizer rn = new RangeNormalizer();
        assertThat(rn.getCount(), equalTo(0));
        assertThat(rn.getRange(), equalTo(0.0));
        assertThat(rn.normalize(1.0), equalTo(0.0));
        assertThat(rn.difference(1.0, 2.0), equalTo(0.0));
        rn.addElement(1.0);
        assertThat(rn.getCount(), equalTo(1));
        assertThat(rn.getRange(), equalTo(0.0));
        assertThat(rn.normalize(1.0), equalTo(0.0));
        assertThat(rn.difference(1.0, 2.0), equalTo(0.0));
        assertThat(rn.getMin(), equalTo(1.0));
        assertThat(rn.getMax(), equalTo(1.0));
        rn.addElement(0.7);
        assertThat(rn.getCount(), equalTo(2));
        assertThat(rn.getRange(), closeTo(0.3, 0.0001));
        assertThat(rn.normalize(1.0), closeTo(1.0, 0.0001));
        assertThat(rn.normalize(0.7), closeTo(0.0, 0.0001));
        assertThat(rn.normalize(0.9), closeTo(0.6666, 0.0001));
        assertThat(rn.difference(1.0, 0.7), closeTo(1.0, 0.0001));
        assertThat(rn.getMin(), equalTo(0.7));
        assertThat(rn.getMax(), equalTo(1.0));
        rn.addElement(0.8);
        assertThat(rn.getCount(), equalTo(3));
        assertThat(rn.getRange(), closeTo(0.3, 0.0001));
        assertThat(rn.normalize(1.0), closeTo(1.0, 0.0001));
        assertThat(rn.normalize(0.7), closeTo(0.0, 0.0001));
        assertThat(rn.normalize(0.9), closeTo(0.6666, 0.0001));
        assertThat(rn.difference(1.0, 0.7), closeTo(1.0, 0.0001));
        assertThat(rn.getMin(), equalTo(0.7));
        assertThat(rn.getMax(), equalTo(1.0));
        rn.addElement(0.9);
        assertThat(rn.getCount(), equalTo(4));
        assertThat(rn.getRange(), closeTo(0.3, 0.0001));
        assertThat(rn.normalize(1.0), closeTo(1.0, 0.0001));
        assertThat(rn.normalize(0.7), closeTo(0.0, 0.0001));
        assertThat(rn.normalize(0.9), closeTo(0.6666, 0.0001));
        assertThat(rn.difference(1.0, 0.7), closeTo(1.0, 0.0001));
        assertThat(rn.getMin(), equalTo(0.7));
        assertThat(rn.getMax(), equalTo(1.0));
        rn.addElement(2.7);
        assertThat(rn.getCount(), equalTo(5));
        assertThat(rn.getRange(), closeTo(2.0, 0.0001));
        assertThat(rn.normalize(1.0), closeTo(0.15, 0.0001));
        assertThat(rn.normalize(0.7), closeTo(0.0, 0.0001));
        assertThat(rn.normalize(0.9), closeTo(0.1, 0.0001));
        assertThat(rn.difference(1.0, 0.7), closeTo(0.15, 0.0001));
        assertThat(rn.getMin(), equalTo(0.7));
        assertThat(rn.getMax(), equalTo(2.7));

    }
    // FIELDS
    // TODO data members for TestNormalizer

    // TODO constructors and methods for TestNormalizer
}
