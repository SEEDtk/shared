/**
 *
 */
package org.theseed.utils;

import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * This is a simple base class for all processing methods.  It handles basic
 * command-parsing and automatically includes the help option.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseProcessor implements ICommand {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(BaseProcessor.class);

    // COMMAND-LINE OPTIONS

    /** help option */
    @Option(name = "-h", aliases = { "--help" }, help = true)
    protected boolean help;

    /** debug-message flag */
    @Option(name = "-v", aliases = { "--verbose", "--debug" }, usage = "show more detailed progress messages")
    private boolean debug;


    @Override
    public boolean parseCommand(String[] args) {
        boolean retVal = false;
        this.help = false;
        this.debug = false;
        this.setDefaults();
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (this.help) {
                parser.printUsage(System.err);
            } else {
                retVal = this.validateParms();
                if (retVal && this.debug) {
                    // To get more progress messages, we set the log level in logback.
                    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                    ch.qos.logback.classic.Logger logger = loggerContext.getLogger("org.theseed");
                    logger.setLevel(Level.toLevel("TRACE"));
                }
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return retVal;
    }

    /**
     * Set the parameter defaults.
     */
    protected abstract void setDefaults();

    /**
     * Validate the parameters after parsing.
     */
    protected abstract boolean validateParms() throws IOException;

    @Override
    public abstract void run();

}