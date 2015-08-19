// Loopers
namespace $privateNamespace {

// Type for pointer to LooperHandler function
typedef unsigned long (*LooperHandler)();

// Looper record
struct Looper {
	// Time of the next handler call
	unsigned long nextCall;
	// State of the looper
	byte state;
	// Handler of looper
	LooperHandler handler;
};

// Generated looper handlers
$looperHandlers
// End of looper handlers

#define ENABLED 1
#define DISABLED 0
#define EXECUTED_ENABLED 2
#define EXECUTED_DISABLED 3

// Loopers
#define LOOPERS_COUNT $numberOfLoopers
Looper loopers[LOOPERS_COUNT] = { $loopersInit };
Looper* pq[LOOPERS_COUNT] = $pqInit;
int pqSize = LOOPERS_COUNT;
unsigned long now = 0;

// Process loopers
inline void processLoopers() {
	if (pqSize == 0) {
		return;
	}

	// Update current time from the view of looper
	now = millis();

	// Process expired handlers
	while (true) {
		Looper* activeLooper = pq[0];

		// Check the first expected looper
		if ((pqSize == 0) || (activeLooper->nextCall > now)) {
			break;
		}

		// Execute handler and store time of the next call
		activeLooper->state = EXECUTED_ENABLED;
		activeLooper->nextCall = now + activeLooper->handler();

		// Move readPos to position of active looper
		Looper** readPos = pq;
		while (*readPos != activeLooper) {
			readPos++;
		}

		// Set writePos to position of active looper and readPos to position of next looper
		Looper** writePos = readPos;
		readPos++;
		Looper** const end = pq + pqSize;

		if (activeLooper->state == EXECUTED_ENABLED) {
			// EXECUTED_ENABLED
			const unsigned long nextCall = activeLooper->nextCall;
			while ((readPos != end) && ((*readPos)->nextCall <= nextCall)) {
				*writePos = *readPos;
				writePos++;
				readPos++;
			}
			*writePos = activeLooper;
			activeLooper->state = ENABLED;
		} else {
			// EXECUTED_DISABLED
			while (readPos != end) {
				*writePos = *readPos;
				writePos++;
				readPos++;
			}
			pqSize--;
			activeLooper->state = DISABLED;
		}
	}
}
}

// Accessible controller methods
namespace acp {

using namespace $privateNamespace;

// Enables a looper
void enableLooper(int looperId) {
	Looper* const looper = &loopers[looperId];
	if ((looper->state == ENABLED) || (looper->state == EXECUTED_ENABLED)) {
		return;
	}

	if (looper->state == EXECUTED_DISABLED) {
		looper->state = EXECUTED_ENABLED;
		return;
	}

	looper->state = ENABLED;
	looper->nextCall = now;

	Looper** writePos = pq + pqSize;
	Looper** readPos = writePos - 1;
	while (writePos != pq) {
		*writePos = *readPos;
		writePos--;
		readPos--;
	}

	pqSize++;
	pq[0] = looper;
}

// Disables a looper
void disableLooper(int looperId) {
	Looper* const looper = &loopers[looperId];
	if ((looper->state == DISABLED) || (looper->state == EXECUTED_DISABLED)) {
		return;
	}

	if (looper->state == EXECUTED_ENABLED) {
		looper->state = EXECUTED_DISABLED;
		return;
	}

	looper->state = DISABLED;

	Looper** readPos = pq;
	while (*readPos != looper) {
		readPos++;
	}

	Looper** const end = pq + pqSize;
	Looper** writePos = readPos;
	readPos++;
	while (readPos != end) {
		*writePos = *readPos;
		writePos++;
		readPos++;
	}
	pqSize--;
}
}
