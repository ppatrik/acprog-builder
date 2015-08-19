package net.acprog.builder.platform;

/**
 * Basic 16bit Arduino platform.
 */
public class ArduinoPlatform extends Platform {

    @Override
    public int getSizeOf(String datatype) {
	switch (datatype) {
	case "bool":
	case "byte":
	case "char":
	case "unsigned char":
	case "signed char":
	    return 1;
	case "word":
	case "int":
	case "unsigned int":
	case "signed int":
	    return 2;
	case "long":
	case "unsigned long":
	case "signed long":
	    return 4;
	case "float":
	case "double":
	    return 4;
	}

	return 0;
    }

    @Override
    public String getEepromWrapperClass(String datatype, int offset, boolean cached) {
	switch (datatype) {
	case "bool":
	case "byte":
	case "char":
	case "unsigned char":
	case "signed char":
	case "word":
	case "int":
	case "unsigned int":
	case "signed int":
	case "long":
	case "unsigned long":
	case "signed long":
	case "float":
	case "double":
	    return cached ? "acp::EEPROMCachedVar<" + datatype + ", " + offset + ">" : "acp::EEPROMVar<" + datatype
		    + ", " + offset + ">";
	}

	return null;
    }

    @Override
    public boolean checkValue(String datatype, String value) {
	// Boolean type
	if ("bool".equals(datatype)) {
	    return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
	}

	// Character type
	if ("char".equals(datatype)) {
	    if (value == null) {
		return false;
	    }

	    value = value.trim();
	    if (value.isEmpty()) {
		value = " ";
	    }

	    if (value.startsWith("\\")) {
		value = value.substring(1);
		if (value.length() == 1) {
		    return ("\\\'\"?abfnrtv".indexOf(value.charAt(0)) >= 0);
		}

		int base = 8;
		if (value.charAt(0) == 'x') {
		    value = value.substring(1);
		    base = 16;
		}

		int numericValue;
		try {
		    numericValue = Integer.parseInt(value.substring(1), base);
		} catch (Exception e) {
		    numericValue = -1;
		}

		return (0 <= numericValue) && (numericValue <= 255);
	    }

	    return value.length() == 1;
	}

	// Unsigned numeric types
	if ("byte".equals(datatype) || "word".equals(datatype) || "unsigned char".equals(datatype)
		|| "unsigned int".equals(datatype) || "unsigned long".equals(datatype)) {
	    long numericValue;
	    try {
		numericValue = Long.parseLong(value);
	    } catch (Exception e) {
		return false;
	    }

	    if (numericValue < 0) {
		return false;
	    }

	    if ("byte".equals(datatype) || "unsigned char".equals(datatype)) {
		return numericValue <= 255;
	    }

	    if ("word".equals(datatype) || "unsigned int".equals(datatype)) {
		return numericValue <= 65535;
	    }

	    if ("unsigned long".equals(datatype)) {
		return numericValue <= 4294967295l;
	    }

	    return false;
	}

	// Signed numeric types
	if ("signed char".equals(datatype) || "signed int".equals(datatype) || "signed long".equals(datatype)
		|| "int".equals(datatype) || "long".equals(datatype)) {
	    int spacePos = datatype.indexOf(' ');
	    if (spacePos >= 0) {
		datatype = datatype.substring(spacePos + 1);
	    }

	    long numericValue;
	    try {
		numericValue = Long.parseLong(value);
	    } catch (Exception e) {
		return false;
	    }

	    if ("char".equals(datatype)) {
		return (-128 <= numericValue) && (numericValue <= 127);
	    }

	    if ("int".equals(datatype)) {
		return (-32768 <= numericValue) && (numericValue <= 32767);
	    }

	    if ("long".equals(datatype)) {
		return (-2147483648 <= numericValue) && (numericValue <= 2147483647);
	    }
	}

	// Floating point numeric types
	if ("float".equals(datatype) || "double".equals(datatype)) {
	    try {
		Double.parseDouble(value);
	    } catch (Exception e) {
		return false;
	    }

	    return true;
	}

	// String literals
	if ("string".equals(datatype) || "f-string".equals(datatype)) {
	    return true;
	}

	// Analog input pins
	if ("analog-pin".equals(datatype)) {
	    if (value == null) {
		return false;
	    }

	    if (!value.startsWith("A")) {
		return false;
	    }

	    value = value.substring(1);
	    int pinCode;
	    try {
		pinCode = Integer.parseInt(value);
	    } catch (Exception e) {
		return false;
	    }

	    return (0 <= pinCode) && (pinCode < getNumberOfAnalogInputPins());
	}

	// Digital pin
	if ("digital-pin".equals(datatype)) {
	    if (value == null) {
		return false;
	    }

	    int pinCode;
	    try {
		pinCode = Integer.parseInt(value);
	    } catch (Exception e) {
		return false;
	    }

	    return (0 <= pinCode) && (pinCode < getNumberOfDigitalPins());
	}

	// Arbitrary pin
	if ("pin".equals(datatype)) {
	    return checkValue("analog-pin", value) || checkValue("digital-pin", value);
	}

	// Interrput
	if ("interrupt".equals(datatype)) {
	    if (value == null) {
		return false;
	    }

	    int interruptCode;
	    try {
		interruptCode = Integer.parseInt(value);
	    } catch (Exception e) {
		return false;
	    }

	    return (0 <= interruptCode) && (interruptCode < getNumberOfInterrupts());
	}

	// Hardware serial
	if ("hardware-serial".equals(datatype)) {
	    if (value == null) {
		return false;
	    }

	    int nrOfSerials = getNumberOfHardwareSerials();
	    if (nrOfSerials <= 0) {
		return false;
	    }

	    if ("Serial".equals(value)) {
		return true;
	    }

	    for (int i = 1; i < nrOfSerials; i++) {
		if (value.equals("Serial" + i)) {
		    return true;
		}
	    }

	    return false;
	}
	
	// Enumeration (type control is managed by restriction on property values) 
	if ("enumeration".equals(datatype)) {
	    return (value != null) && (!value.trim().isEmpty());
	}


	return false;
    }

    @Override
    public String escapeValue(String datatype, String value) {
	// TODO just simple escaping is implemented

	// Boolean type
	if ("bool".equals(datatype)) {
	    return value.toLowerCase();
	}

	// Character type
	if ("char".equals(datatype)) {
	    if (value == null) {
		return null;
	    }

	    value = value.trim();
	    if (value.isEmpty()) {
		value = " ";
	    }

	    return "\'" + value + "\'";
	}

	// Unsigned numeric types
	if ("byte".equals(datatype) || "word".equals(datatype) || "unsigned char".equals(datatype)) {
	    return value;
	}

	if ("unsigned int".equals(datatype)) {
	    if (value == null) {
		return null;
	    } else {
		return value + "u";
	    }
	}

	if ("unsigned long".equals(datatype)) {
	    if (value == null) {
		return null;
	    } else {
		return value + "ul";
	    }
	}

	// Signed numeric types
	if ("signed char".equals(datatype) || "signed int".equals(datatype) || "int".equals(datatype)) {
	    return value;
	}

	if ("long".equals(datatype) || "signed long".equals(datatype)) {
	    if (value == null) {
		return null;
	    } else {
		return value + "l";
	    }
	}

	// Floating point numeric types
	if ("float".equals(datatype) || "double".equals(datatype)) {
	    return value;
	}

	// String literals
	if ("string".equals(datatype)) {
	    if (value == null) {
		return "NULL";
	    } else {
		return "\"" + value + "\"";
	    }
	}

	if ("f-string".equals(datatype)) {
	    if (value == null) {
		return "NULL";
	    } else {
		return "F(\"" + value + "\")";
	    }
	}

	// Pins
	if ("pin".equals(datatype) || "digital-pin".equals(datatype) || "analog-pin".equals(datatype)) {
	    return value;
	}

	// Interrupt
	if ("interrupt".equals(datatype)) {
	    return value;
	}

	// Hardware serial
	if ("hardware-serial".equals(datatype)) {
	    return value;
	}
	
	// Enumeration (type control is managed by restriction on property values) 
	if ("enumeration".equals(datatype)) {
	    return value;
	}

	return null;
    }

    @Override
    public int getNumberOfDigitalPins() {
	return 0;
    }

    @Override
    public int getNumberOfAnalogInputPins() {
	return 0;
    }

    @Override
    public int getNumberOfInterrupts() {
	return 0;
    }

    @Override
    public int getMaxWatchdogLevel() {
	return 9;
    }

    @Override
    public int getNumberOfHardwareSerials() {
	return 1;
    }
}
