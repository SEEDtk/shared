/**
 *
 */
package org.theseed.genome;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.theseed.utils.BaseProcessor;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This object represents an analysis event for a genome.  Analysis events are tracked in the GTO to form
 * a useful audit trail.
 *
 * @author Bruce Parrello
 *
 */
public class AnalysisEvent {

    // FIELDS
    /** uuid for event */
    private String id;
    /** time of analysis, in seconds since the epoch */
    private double execute_time;
    /** host on which analysis was run */
    private String hostname;
    /** analysis tool name */
    private String tool_name;
    /** parameter list */
    private List<String> parameters;

    /**
     * This enum defines the JSON keys for the event.
     */
    public static enum EventKeys implements JsonKey {
        ID(""),
        EXECUTE_TIME(0),
        HOSTNAME("localhost"),
        TOOL_NAME(""),
        PARAMETERS(null);

        private final Object m_value;

        EventKeys(final Object value) {
            this.m_value = value;
        }

        /** This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /** This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    }

    /**
     * Create a new analysis event from a JSON object.
     *
     * @param json	JSON object containing the analysis event
     */
    public AnalysisEvent(JsonObject json) {
        this.id = json.getStringOrDefault(EventKeys.ID);
        this.execute_time = json.getDoubleOrDefault(EventKeys.EXECUTE_TIME);
        this.hostname = json.getStringOrDefault(EventKeys.HOSTNAME);
        this.tool_name = json.getStringOrDefault(EventKeys.TOOL_NAME);
        // The parameter list is the only tricky one, as there is no default, and
        // it must be converted from a JSON array to a string list.
        JsonArray parms = json.getCollectionOrDefault(EventKeys.PARAMETERS);
        if (parms == null)
            this.parameters = Collections.emptyList();
        else {
            this.parameters = new ArrayList<String>(parms.size());
            this.appendParms(parms);
        }
    }

    /**
     * This is a recursive method to add all the parameter strings to the parameter list.  We have to
     * do it this crazy way because the BV-BRC software is sloppy about the way it passes around parameters.
     *
     * @param parms		list of parameters and/or parameter lists
     */
    private void appendParms(JsonArray parms) {
        final int n = parms.size();
        for (int i = 0; i < n; i++) {
            if (parms.get(i) instanceof JsonArray)
                this.appendParms((JsonArray) parms.get(i));
            else
                this.parameters.add(parms.getString(i));
        }
    }

    /**
     * Create a new analysis event for the specified running command.
     *
     * @param command		current command name (and sub-name)
     * @param processor		processor running the command
     */
    public AnalysisEvent(String command, BaseProcessor processor) {
        this.id = UUID.randomUUID().toString();
        this.execute_time = ((double) System.currentTimeMillis()) / 1000.0;
        try {
            this.hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            this.hostname = "(unknown)";
        }
        this.tool_name = command;
        // Set up the parameters.
        String[] parms = processor.getOptions();
        this.parameters = Arrays.asList(parms);
    }

    /**
     * @return this analysis event as a JSON object
     */
    public JsonObject toJson() {
        JsonObject retVal = new JsonObject();
        retVal.put(EventKeys.ID.getKey(), this.id);
        retVal.put(EventKeys.EXECUTE_TIME.getKey(), (Double) this.execute_time);
        retVal.put(EventKeys.HOSTNAME.getKey(), this.hostname);
        retVal.put(EventKeys.TOOL_NAME.getKey(), this.tool_name);
        JsonArray parms = new JsonArray().addAllChain(this.parameters);
        retVal.put(EventKeys.PARAMETERS.getKey(), parms);
        return retVal;
    }

    /**
     * @return the event ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the time stamp of the event
     */
    public double getExecuteTime() {
        return this.execute_time;
    }

    /**
     * @return the name of the machine that executed the analysis
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * @return the name of the analysis tool
     */
    public String getToolName() {
        return this.tool_name;
    }

    /**
     * @return the command parameters
     */
    public List<String> getParameters() {
        return this.parameters;
    }

}
