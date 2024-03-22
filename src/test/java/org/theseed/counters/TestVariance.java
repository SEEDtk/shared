/**
 *
 */
package org.theseed.counters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.stats.CorrelationVariance;

/**
 * @author Bruce Parrello
 *
 */
class TestVariance {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TestVariance.class);


    @Test
    void testShortOnes() {
        double[] series1 = new double[0];
        double[] series2 = new double[1];
        CorrelationVariance corr = new CorrelationVariance();
        try {
            corr.variation(series1, series2);
            assertThat("Exception should have been thrown.", false);
        } catch (IllegalArgumentException e) { }
        assertThat(corr.getTrimean(), equalTo(0.0));
        assertThat(corr.getMAE(), equalTo(0.0));
        assertThat(corr.getMSE(), equalTo(0.0));
        assertThat(corr.getStandardDeviation(), equalTo(0.0));
        series1 = new double[] { 6.0 };
        series2 = new double[] { 3.2 };
        corr.variation(series1, series2);
        assertThat(corr.size(), equalTo(1));
        assertThat(corr.getTrimean(), equalTo(0.0));
        assertThat(corr.getMAE(), equalTo(0.0));
        assertThat(corr.getMSE(), equalTo(0.0));
        assertThat(corr.getStandardDeviation(), equalTo(0.0));
        assertThat(corr.getIQR(), equalTo(0.0));
        series1 = new double[] { 1.0, 0.0 };
        series2 = new double[] { -2.0, 6.0 };
        corr.variation(series1, series2);
        assertThat(corr.size(), equalTo(2));
        assertThat(corr.getTrimean(), equalTo(1.0));
        assertThat(corr.getMAE(), equalTo(1.0));
        assertThat(corr.getMSE(), equalTo(1.0));
        assertThat(corr.getStandardDeviation(), equalTo(0.0));
        assertThat(corr.getIQR(), equalTo(0.0));
        series1 = new double[] { 1.0, 1.40, 2.00, 2.20, 3.00 };
        series2 = new double[] { 1.0, 0.45, 0.00, 0.60, 0.25 };
        corr.variation(series1, series2);
        assertThat(corr.size(), equalTo(5));
        assertThat(corr.getTrimean(), closeTo(0.5000, 0.0001));
        assertThat(corr.getMAE(), closeTo(0.5000, 0.0001));
        assertThat(corr.getMSE(), closeTo(0.3750, 0.0001));
        assertThat(corr.getStandardDeviation(), closeTo(0.3953, 0.0001));
        assertThat(corr.getIQR(), closeTo(0.7500, 0.0001));
        series1 = new double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 };
        series2 = new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.5, 2.0, 0.0 };
        corr.variation(series1, series2);
        assertThat(corr.size(), equalTo(9));
        assertThat(corr.getTrimean(), closeTo(0.3125, 0.0001));
        assertThat(corr.getMAE(), closeTo(0.3056, 0.0001));
        assertThat(corr.getMSE(), closeTo(0.1236, 0.0001));
        assertThat(corr.getStandardDeviation(), closeTo(0.1845, 0.0001));
        assertThat(corr.getIQR(), closeTo(0.3500, 0.0001));
    }

}
