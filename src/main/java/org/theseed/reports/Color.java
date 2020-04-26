/**
 *
 */
package org.theseed.reports;

/**
 * This is a simple color class, designed to output HTML colors.
 *
 * @author Bruce Parrello
 *
 */
public class Color {

    // FIELDS
    short red;
    short green;
    short blue;

    /**
     * Create a color.
     *
     * @param r		red channel (between 0 and 1)
     * @param g		green channel (between 0 and 1)
     * @param b		blue channel (between 0 and 1)
     */
    public Color(double r, double g, double b) {
        this.red = (short) Math.round(r * 0xFF);
        this.green = (short) Math.round(g * 0xFF);
        this.blue = (short) Math.round(b * 0xFF);
    }

    /**
     * Scale this color closer to black by the specified factor.
     *
     * @param factor	factor to use to scale the color
     */
    public Color darken(double factor) {
        double x = 1.0 - factor;
        return new Color((this.red / 255.0) * x,
                (this.green / 255.0) * x,
                (this.blue / 255.0) * x
                );
    }

    /**
     * Scale this color closer to white by the specified factor.
     *
     * @param factor	factor to use to scale the color
     */
    public Color brighten(double factor) {
        return new Color(Color.brighten(this.red, factor),
                Color.brighten(this.green, factor),
                Color.brighten(this.blue, factor));
    }

    /**
     * Scale a color component brighter by the specified factor.
     *
     * @param c			color component
     * @param factor	factor by which to brighten
     *
     * @return the color factor as a fraction from 0 to 1
     */
    private static double brighten(short c, double factor) {
        double x = c / 256.0;
        return x + factor * (1 - x);
    }

    /**
     * Convert this color to HTML.
     */
    public String html() {
        return String.format("#%02X%02X%02X", this.red & 0xFF,
                this.green & 0xFF, this.blue & 0xFF);
    }

    /** the color white */
    public final static Color WHITE = new Color(1.0, 1.0, 1.0);
    /** the color light gray */
    public final static Color LIGHT_GRAY = new Color(0.75, 0.75, 0.75);
    /** the color gray */
    public final static Color GRAY = new Color(0.5, 0.5, 0.5);
    /** the color dark gray */
    public final static Color DARK_GRAY = new Color(0.25, 0.25, 0.25);
    /** the color black */
    public final static Color BLACK = new Color(0.0, 0.0, 0.0);
    /** the color red */
    public final static Color RED = new Color(1.0, 0.0, 0.0);
    /** the color pink */
    public final static Color PINK = new Color(1.0, 0.68, 0.68);
    /** the color orange */
    public final static Color ORANGE = new Color(1.0, 0.78, 0.0);
    /** the color yellow */
    public final static Color YELLOW = new Color(1.0, 1.0, 0.0);
    /** the color green */
    public final static Color GREEN = new Color(0.0, 1.0, 0.0);
    /** the color magenta */
    public final static Color MAGENTA = new Color(1.0, 0.0, 1.0);
    /** the color cyan */
    public final static Color CYAN = new Color(0.0, 1.0, 1.0);
    /** the color blue */
    public final static Color BLUE = new Color(0.0, 0.0, 1.0);
    /** the color dark yellow */
    public final static Color DARK_YELLOW = new Color(0.70, 0.70, 0.0);
    /** the color dark green */
    public static final Color DARK_GREEN = new Color(0.0, 0.70, 0.0);


}
