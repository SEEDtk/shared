/**
 *
 */
package org.theseed.rna;

/**
 * This is the base class for converting RNA Seq expression data prior to output in a training set.  The conversion
 * is always from a number to a number, but it can be scaled or compressed in different ways.  The scores are
 * processed one "row" at a time, each row representing the expression values for a single feature.  The client
 * calls once to allow the class to examine the entire row, and again to convert the individual values.  The
 * majority of the scaling is in terms of the range of expression values, hence the two-call structure.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ExpressionConverter {

    // FIELDS
    /** current field's data row */
    private RnaData.Row row;

    /**
     * Analyze the current feature's data row.
     *
     * @param row0	data row to analyze
     */
    public void analyzeRow(RnaData.Row row0) {
        this.row = row0;
        this.processRow();
    }

    /**
     * Perform preliminary computations on the current row.
     */
    protected abstract void processRow();

    /**
     * @return the expression data for the specified sample in the current data row
     *
     * @param	jobInfo		job information for sample in question
     */
    public double getExpression(RnaJobInfo jobInfo) {
        double rawValue = jobInfo.getExpression(this.row);
        return this.convert(rawValue);
    }

    /**
     * @return the output value for the specified raw expression value
     *
     * @param rawValue	raw expression value to convert
     */
    public abstract double convert(double rawValue);


    /**
     * Enum for the different types of output conversions
     */
    public static enum Type {
        RAW {
            @Override
            public ExpressionConverter create(IBaselineProvider processor) {
                return new ExpressionConverter.Raw();
            }
        }, STD {
            @Override
            public ExpressionConverter create(IBaselineProvider processor) {
                return new StandardScoreExpressionConverter();
            }
        }, TRIAGE {
            @Override
            public ExpressionConverter create(IBaselineProvider processor) {
                return new TriageExpressionConverter(processor);
            }
        };

        public abstract ExpressionConverter create(IBaselineProvider processor);
    }

    /**
     * This simply returns the raw expression value
     */
    public static class Raw extends ExpressionConverter {

        @Override
        protected void processRow() {
        }

        @Override
        public double convert(double rawValue) {
            return rawValue;
        }

    }

    /**
     * @return the row data
     */
    protected RnaData.Row getRow() {
        return this.row;
    }

}

