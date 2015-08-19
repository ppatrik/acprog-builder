#ifndef ACP_EEPROM_VARS_H_
#define ACP_EEPROM_VARS_H_

#include <Arduino.h>
#include <EEPROM.h>

namespace $privateNamespace {

	//--------------------------------------------------------------------------------
	// Reads a value from EEPROM
	template<typename TYPE> TYPE readEEPROMVar(size_t offset) {
		noInterrupts();
		TYPE result;
		uint8_t* p = (uint8_t*)&result;
		for (int i=0; i<sizeof(TYPE); i++) {
			*p = EEPROM.read(offset);
			p++;
			offset++;
		}
		interrupts();
		return result;
	}

	//--------------------------------------------------------------------------------
	// Writes a value to EEPROM
	template<typename TYPE> void writeEEPROMVar(size_t offset, TYPE value) {
		noInterrupts();
		uint8_t* p = (uint8_t*)&value;
		for (int i=0; i<sizeof(TYPE); i++) {
			EEPROM.write(offset, *p);
			p++;
			offset++;
		}
		interrupts();
	}

	//--------------------------------------------------------------------------------
	// Updates the value in EEPROM
	template<typename TYPE> void updateEEPROMVar(size_t offset, TYPE value) {
		noInterrupts();
		uint8_t* p = (uint8_t*)&value;
		for (int i=0; i<sizeof(TYPE); i++) {
			uint8_t oldValue = EEPROM.read(offset);
			if (oldValue != *p) {
				EEPROM.write(offset, *p);
			}
			p++;
			offset++;
		}
		interrupts();
	}

	//--------------------------------------------------------------------------------
	// Checks current version of eeprom layout
	bool checkEepromVersion(unsigned long versionCode);

	//--------------------------------------------------------------------------------
	// Stores version of eeprom layout
	void writeEepromVersion(unsigned long versionCode);
}

namespace acp {

	/********************************************************************************
	 * The class warping access to a EEPROM variable.
	 ********************************************************************************/
	template <typename TYPE, int OFFSET> class EEPROMVar {
	public:
		//--------------------------------------------------------------------------------
		// Returns the value of variable by reading content of variable directly from EEPROM.
		inline TYPE getValue() {
			return $privateNamespace::readEEPROMVar<TYPE>(OFFSET);
		}

		//--------------------------------------------------------------------------------
		// Initializes the variable
		inline void init() {

		}

		//--------------------------------------------------------------------------------
		// Returns the value of variable by reading content of variable directly from EEPROM.
		inline void setValue(TYPE value) {
			$privateNamespace::updateEEPROMVar<TYPE>(OFFSET, value);
		}
	};

	/********************************************************************************
	 * The class warping access to a EEPROM variable with a SRAM cached value.
	 ********************************************************************************/
	template <typename TYPE, int OFFSET> class EEPROMCachedVar {
	private:
		TYPE value;
	public:
		//--------------------------------------------------------------------------------
		// Initializes the variable
		inline void init() {
			value = $privateNamespace::readEEPROMVar<TYPE>(OFFSET);
		}

		//--------------------------------------------------------------------------------
		// Returns the value of variable.
		inline TYPE getValue() {
			return value;
		}

		//--------------------------------------------------------------------------------
		// Sets the value of variable.
		inline void setValue(TYPE newValue) {
			if (newValue != value) {
				value = newValue;
				$privateNamespace::updateEEPROMVar<TYPE>(OFFSET, value);
			}
		}
	};
}

#endif /* ACP_EEPROM_VARS_H_ */
