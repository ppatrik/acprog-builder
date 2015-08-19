#include <$acpEepromHeaderFile>

namespace $privateNamespace {

	//--------------------------------------------------------------------------------
	// Checks current version of eeprom layout
	bool checkEepromVersion(unsigned long versionCode) {
		noInterrupts();
		for (int i=0; i<4; i++) {
			if (versionCode % 256 != EEPROM.read(i)) {
				interrupts();
				return false;
			}
			versionCode = versionCode / 256;
		}
		interrupts();
		return true;
	}

	//--------------------------------------------------------------------------------
	// Stores version of eeprom layout
	void writeEepromVersion(unsigned long versionCode) {
		noInterrupts();
		for (int i=0; i<4; i++) {
			uint8_t newValue = versionCode % 256;
			if (newValue != EEPROM.read(i)) {
				EEPROM.write(i, newValue);
			}

			versionCode = versionCode / 256;
		}
		interrupts();
	}
}
