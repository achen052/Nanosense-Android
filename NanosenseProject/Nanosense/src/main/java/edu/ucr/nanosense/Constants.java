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

    public class Commands {
        public static final byte RHEOSTAT_WRITE = 0x04;
        public static final byte RHEOSTAT_READ_UPPER = (byte) 0x80;
        public static final byte RHEOSTAT_READ_LOWER = 0x00;
    }

    /**
     * Constants used for device properties.
     */
    public class Device {
        /** IOIO Onboard LED pin **/
        public static final int PIN_LED = 0;

        /** MUX pin numbers **/
        public static final int PIN_MUX_SEL0 = 1;
        public static final int PIN_MUX_SEL1 = 2;
        public static final int PIN_MUX_SEL2 = 3;
        public static final int PIN_MUX_SEL3 = 4;
        /** Rover TX/RX pins **/
        public static final int PIN_ROVER_RX = 5;
        public static final int PIN_ROVER_TX = 6;
        /** SPI pin numbers **/
        public static final int PIN_SPI_MISO = 9;
        public static final int PIN_SPI_SS = 10;
        public static final int PIN_SPI_MOSI = 11;
        public static final int PIN_SPI_CLK = 13;
        /** ADC Pin for Nano Sensor */
        public static final int PIN_ADC0 = 31;
        /** ADC Pin for Humidity */
        // TODO: SWAP ORDER BETWEEN HUMIDITY AND TEMPERATURE IN HARDWARE IF POSSIBLE. not a big issue though. just makes the code a bit confusing.
        public static final int PIN_ADC1 = 32;
         /** ADC Pin for Temperature */
        public static final int PIN_ADC2 = 33;
         /** ADC Pin for Thermistor in Bridge with 10kOhm as R2. */
        public static final int PIN_ADC3 = 34;

        /** Order of the Nanosensor/Mux in the ADC. */
        public static final int ADC_NANO_SENSOR = 0;
        /** Order of the Humidity sensor in the ADC. */
        public static final int ADC_HUMIDITY = 1;
        /** Order of the Temperature sensor in the ADC. */
        public static final int ADC_TEMPERATURE = 2;
        /** Order of the Thermistor in the ADC. */
        public static final int ADC_THERMISTOR = 3;

        /** Number of pins for MUX Select. */
        public static final int NUM_PINS_MUX = 4;
        /** Number of pins for AnalogInput. */
        public static final int NUM_PINS_ANALOG = 4;

        /** 14 gas sensors **/
        public static final int NUM_PINS_NANOSENSOR = 14;

        /** 10-Bit ADC 2^10 - 1 = 1023**/
        public static final int MAX_BIT_VOLTAGE = 1023;
        public static final int MAX_BIT_RESISTANCE = 255;

        /** Reference voltage used for the analog read **/
        public static final double VOLTAGE_REFERENCE = 3.3;

        /** Max rheostat resistance in kOhms **/
        public static final double MAX_RHEO_RESISTANCE = 100.0;

        /** Uart baud rate **/
        public static final int UART_RATE = 115200;
    }

    public class Temperature {
        public static final double VOLTAGE_OFFSET = -0.75;
        public static final double TEMPERATURE_OFFSET = 25;
        public static final double TEMPERATURE_SCALE = 100;
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

    public class Humidity {
        public static final double VOLTAGE_OFFSET = 0.16;
        public static final double VOLTAGE_SCALE = 0.0062;
        public static final double TEMPERATURE_OFFSET = 1.0546;
        public static final double TEMPERATURE_SCALE = -0.00216;
    }

    public class Thermistor {
        /** Voltage divider R2 in kOhms **/
        public static final double DIVIDER_RESISTANCE = 10.0;
        public static final double TEMPERATURE_OFFSET = -507.59;
        public static final double TEMPERATURE_SCALE = 115.6;
    }
}
