#ifndef ACP_EEPROM_VARS_H_
#define ACP_EEPROM_VARS_H_

#include <Arduino.h>
#include <EEPROM.h>

namespace $privateNamespace {

	//--------------------------------------------------------------------------------
	// Reads a value from EEPROM
	template<typename TYPE> TYPE readValueFromEeprom(size_t offset) {
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
	template<typename TYPE> void writeValueToEeprom(size_t offset, TYPE value) {
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
	template<typename TYPE> void updateValueInEeprom(size_t offset, TYPE value) {
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
	 * The class wrapping access to an EEPROM variable.
	 ********************************************************************************/
	template <typename TYPE, int OFFSET> class EEPROMVar {
	public:
		//--------------------------------------------------------------------------------
		// Returns the value of variable by reading content of variable directly from EEPROM.
		inline TYPE getValue() {
			return $privateNamespace::readValueFromEeprom<TYPE>(OFFSET);
		}

		//--------------------------------------------------------------------------------
		// Initializes the variable
		inline void init() {

		}

		//--------------------------------------------------------------------------------
		// Returns the value of variable by reading content of variable directly from EEPROM.
		inline void setValue(TYPE value) {
			$privateNamespace::updateValueInEeprom<TYPE>(OFFSET, value);
		}
	};

	/********************************************************************************
	 * The class wrapping access to an EEPROM variable with a SRAM cached value.
	 ********************************************************************************/
	template <typename TYPE, int OFFSET> class EEPROMCachedVar {
	private:
		TYPE value;
	public:
		//--------------------------------------------------------------------------------
		// Initializes the variable
		inline void init() {
			value = $privateNamespace::readValueFromEeprom<TYPE>(OFFSET);
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
				$privateNamespace::writeValueToEeprom<TYPE>(OFFSET, value);
			}
		}
	};

	/********************************************************************************
	 * The class wrapping access to an EEPROM array.
	 ********************************************************************************/
	template <typename TYPE, int OFFSET, int LENGTH> class EEPROMArray {
	public:
		//--------------------------------------------------------------------------------
		// Returns the size of the array
		inline int size() {
			return LENGTH;
		}

		//--------------------------------------------------------------------------------
		// Returns the value at given index of the array by reading its content directly from EEPROM.
		// The method does not check whether the index is valid.
		inline TYPE get(int index) {
			return $privateNamespace::readValueFromEeprom<TYPE>(OFFSET + index);
		}

		//--------------------------------------------------------------------------------
		// Sets new value at given index of the array and writes the changes directly to EEPROM.
		inline void set(int index, TYPE value) {
			if ((0 <= index) && (index < LENGTH)) {
				$privateNamespace::updateValueInEeprom<TYPE>(OFFSET + index, value);
			}
		}

		//--------------------------------------------------------------------------------
		// Writes values directly from memory to EEPROM array.
		inline void write(const TYPE* src, int count) {
			if (count > LENGTH) {
				count = LENGTH;
			}

			for (int i=0; i<count; i++) {
				$privateNamespace::updateValueInEeprom<TYPE>(OFFSET + i, *src);
				src++;
			}
		}

		//--------------------------------------------------------------------------------
		// Reads values directly from EEPROM array to memory.
		inline int read(TYPE* dst, int size) {
			if (size > LENGTH) {
				size = LENGTH;
			}

			for (int i=0; i<size; i++) {
				*dst = $privateNamespace::readValueFromEeprom<TYPE>(OFFSET + i);
				dst++;
			}

			return size;
		}

		//--------------------------------------------------------------------------------
		// Initializes the variable
		inline void init() {

		}

		//--------------------------------------------------------------------------------
		// Assigns the value to all items of the array.
		inline void fill(TYPE value) {
			for (int i=0; i<LENGTH; i++) {
				$privateNamespace::updateValueInEeprom<TYPE>(OFFSET + i, value);
			}
		}
	};

	/********************************************************************************
	 * The class wrapping access to a cached EEPROM array.
	 ********************************************************************************/
	template <typename TYPE, int OFFSET, int LENGTH> class EEPROMCachedArray {
	private:
		TYPE values[LENGTH];
	public:
		//--------------------------------------------------------------------------------
		// Returns the size of the array
		inline int size() {
			return LENGTH;
		}

		//--------------------------------------------------------------------------------
		// Returns the value at given index of the array.
		// The method does not check whether the index is valid.
		inline TYPE get(int index) {
			return values[index];
		}

		//--------------------------------------------------------------------------------
		// Sets new value at given index of the array.
		inline void set(int index, TYPE value) {
			if ((0 <= index) && (index < LENGTH)) {
				if (value != values[index]) {
					values[index] = value;
					$privateNamespace::writeValueToEeprom<TYPE>(OFFSET + index, value);
				}
			}
		}

		//--------------------------------------------------------------------------------
		// Writes values from memory to EEPROM array.
		inline void write(const TYPE* src, int count) {
			if (count > LENGTH) {
				count = LENGTH;
			}

			for (int i=0; i<count; i++) {
				if (values[i] != *src) {
					values[i] = *src;
					$privateNamespace::writeValueToEeprom<TYPE>(OFFSET + i, *src);
				}

				src++;
			}
		}

		//--------------------------------------------------------------------------------
		// Reads values directly from EEPROM array to memory.
		inline int read(TYPE* dst, int size) {
			if (size > LENGTH) {
				size = LENGTH;
			}

			mmemcpy(dst, values, sizeof(TYPE) * size);
			return size;
		}

		//--------------------------------------------------------------------------------
		// Initializes the array
		inline void init() {
			for (int i=0; i<LENGTH; i++) {
				values[i] = $privateNamespace::readValueFromEeprom<TYPE>(OFFSET + i);
			}
		}

		//--------------------------------------------------------------------------------
		// Assigns the value to all items of the array.
		inline void fill(TYPE value) {
			for (int i=0; i<LENGTH; i++) {
				if (values[i] != value) {
					values[i] = value;
					$privateNamespace::writeValueToEeprom<TYPE>(OFFSET + i, value);
				}
			}
		}
	};
}

#endif /* ACP_EEPROM_VARS_H_ */
