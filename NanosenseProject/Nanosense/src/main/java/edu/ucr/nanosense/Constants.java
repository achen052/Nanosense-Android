package edu.ucr.nanosense;

/**
 * Written by Albert Chen.
 * Last updated 12/04/13
 *
 * Constants class for shared constants used in multiple classes.
 */
public class Constants {

    /**
     * Constants used as a fallback for default or invalid settings.
     */
    public class Options {
        public static final int DEFAULT_POLLING_RATE = 100;
        public static final int DEFAULT_SERVER_PORT = 8080;
        public static final int DEFAULT_BASELINE_DURATION = 600;
        public static final String DEFAULT_SERVER_IP = "127.0.0.1";
    }

    /**
     * Constants used by both {@link GraphView} and {@link GraphDeltaView} to determine what to draw.
     */
    public class GraphView {
        public static final int VIEW_NANOSENSOR = 0;
        public static final int VIEW_HUMIDITY = 1;
        public static final int VIEW_TEMPERATURE = 2;

        /** Number of sensors (16 nanosensors, 1 temp, 1 humidity **/
        public static final int NUM_SENSORS = 18;

        /** Max resistance in kOhms **/
        public static final int MAX_RESISTANCE = 200;
    }

}
