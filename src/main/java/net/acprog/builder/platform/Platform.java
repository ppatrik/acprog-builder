package net.acprog.builder.platform;

/**
 * Hardware and compilation platform.
 */
public abstract class Platform {

    /**
     * Returns the size of data type in bytes.
     * 
     * @param datatype
     *            the name of data type
     * @return size in bytes or 0 if the sizeOf is irrelevant (not defined) for
     *         the data type.
     */
    public abstract int getSizeOf(String datatype);

    /**
     * Returns the name of a wrapper class for accessing a value of this type
     * stored in EEPROM.
     * 
     * @param datatype
     *            the name of data type
     * @param offset
     *            the offset of data item in EEPROM
     * @param cached
     *            true, if the wrapper is a caching wrapper.
     * @param arrayLength
     *            the number of items (of given datatype) forming the variable.
     *            The negative value indicates a simple variable, zero or
     *            positive value an array.
     * @return the name of wrapper class, or null, if the EEPROM storage is not
     *         supported for given data type.
     */
    public abstract String getEepromWrapperClass(String datatype, int offset, boolean cached, int arrayLength);

    /**
     * Checks whether the value is valid for given datatype.
     * 
     * @param datatype
     *            the name of data type.
     * @param value
     *            the string representation of the value.
     * @return true, if the string representation is valid for given datatype.
     */
    public abstract boolean checkValue(String datatype, String value);

    /**
     * Generates a representation of a value that can be used in a C/C++ code.
     * 
     * @param datatype
     *            the name of data type.
     * @param value
     *            the string representation of the value
     * @return string containing representation of the value that can be used in
     *         a C/C++ code or null, if the platform does not support the type
     *         or the value is invalid for given type.
     */
    public abstract String escapeValue(String datatype, String value);

    /**
     * Returns the number of digital pins provided by the platform.
     * 
     * @return the number of digital pins.
     */
    public abstract int getNumberOfDigitalPins();

    /**
     * Returns the number of analog input pins provided by the platform.
     * 
     * @return the number of analog input pins.
     */

    public abstract int getNumberOfAnalogInputPins();

    /**
     * Returns the number of interrupts provided by the platform.
     * 
     * @return the number of interrupts.
     */
    public abstract int getNumberOfInterrupts();

    /**
     * Returns the maximal watchdog level supported by the platform.
     * 
     * @return the maximal watchdog level available on the platform.
     */
    public abstract int getMaxWatchdogLevel();

    /**
     * Returns the number of hardware serials supported by the platform.
     * 
     * @return the number of hardware serials
     */
    public abstract int getNumberOfHardwareSerials();

    /**
     * Instantiates compilation platform with given name.
     * 
     * @param platformName
     *            the name of platform.
     * @return platform implementation or null, if such a platform is not
     *         supported.
     */
    public static Platform loadPlatform(String platformName) {
	if (platformName == null) {
	    return null;
	}

	switch (platformName.trim()) {
	case "Arduino":
	    return new ArduinoPlatform();
	case "ArduinoMega":
	    return new ArduinoMegaPlatform();
	case "ArduinoUno":
	    return new ArduinoUnoPlatform();
	case "ArduinoNano":
	    return new ArduinoUnoPlatform();
	}

	return null;
    }
}
