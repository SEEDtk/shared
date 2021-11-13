/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;
import org.theseed.counters.RegressionStatistics;

/**
 * @author Bruce Parrello
 *
 */
public class TestRegressionStats {

    @Test
    public void test() {
        double[] expect  = new double[] {  0.0,  0.0,  0.0,  0.0,  0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double[] predict = new double[] { -3.0, -5.0, -1.0, -3.0, -2.0, 2.0, 0.0, 1.0, 5.0, 2.0, 3.0, 0.0, 1.0 };
        RegressionStatistics stats = new RegressionStatistics(5);
        for (int i = 0; i < expect.length; i++)
            stats.add(expect[i], predict[i]);
        stats.finish();
        assertThat(stats.getQ1(), equalTo(-2.5));
        assertThat(stats.getQ2(), equalTo(0.0));
        assertThat(stats.getQ3(), equalTo(2.0));
        assertThat(stats.trimean(), equalTo(-0.125));
        assertThat(stats.iqr(), equalTo(4.5));
        assertThat(stats.trimmedMean(0.0), closeTo(2.1538, 0.0001));
        assertThat(stats.trimmedMean(0.25), equalTo(1.125));
    }

}
