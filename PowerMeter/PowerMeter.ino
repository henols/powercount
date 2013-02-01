#include <OneWire.h>
#include <DallasTemperature.h>

// Data wire is plugged into port 2 on the Arduino
#define ONE_WIRE_BUS 10
#define TEMPERATURE_PRECISION 9

#define COUNTER_1 0
#define COUNTER_2 1

const byte CONFIRM = 'c';
const byte NAME = '3';
const byte PULSES_TEXT = '4';
const byte DUMP = 'd';
const byte TEMPERATURE = 't';

const int BAUD_RATE = 19200;
const byte LED_PIN_1 = 13; // LED connected to digital pin 13
const byte LED_PIN_2 = 9; // LED connected to digital pin 14

const byte LED_PINS[2] = { LED_PIN_1, LED_PIN_2 };

const byte PULSE_PIN_1 = 2; //
const byte PULSE_PIN_2 = 3; //

long pulseCount[2] = { 0, 0 }; //Number of pulses, used to measure energy.
unsigned long pulseTime[2], lastTime[2]; //Used to measure power.
int power[2]; //power and energy
int ppwh[2] = { 1, 1 }; ////1000 pulses/kwh = 1 pulse per wh - Number of pulses per wh - found or set on the meter.

long lastSerial;

boolean ledState[] = { false, false };

// Setup a oneWire instance to communicate with any OneWire devices (not just Maxim/Dallas temperature ICs)
OneWire oneWire(ONE_WIRE_BUS);

// Pass our oneWire reference to Dallas Temperature. 
DallasTemperature sensors(&oneWire);

DeviceAddress tempDeviceAddress; // We'll use this variable to store a found device address

void setup() {
	pinMode(LED_PIN_1, OUTPUT); // sets the digital pin as output
	pinMode(LED_PIN_2, OUTPUT); // sets the digital pin as output

	changeLedState(COUNTER_1);
	changeLedState(COUNTER_2);

	Serial.begin(BAUD_RATE);

	attachInterrupt(0, onPulse1, FALLING); // KWH interrupt attached to IRQ 1  = pin2 
	attachInterrupt(1, onPulse2, FALLING); // KWH interrupt attached to IRQ 1  = pin3

	changeLedState(COUNTER_1);
	changeLedState(COUNTER_2);

	// Start up the library
	sensors.begin();

	// Loop through each device, print out address
	for (int i = 0; i < sensors.getDeviceCount(); i++) {
		// Search the wire for address
		if (sensors.getAddress(tempDeviceAddress, i)) {
			// set the resolution to TEMPERATURE_PRECISION bit (Each Dallas/Maxim device is capable of several different resolutions)
			sensors.setResolution(tempDeviceAddress, TEMPERATURE_PRECISION);
		}
	}
}

void loop() {
	if (Serial.available() >= 2) {
		lastSerial = millis();
		// get incoming byte:
		byte inByte = Serial.read();
		int counter = Serial.read() - '0';
		switch (inByte) {
		case PULSES_TEXT:
			getCountText(counter);
			break;
		case CONFIRM:
			confirmCount(counter);
			break;
		case DUMP:
			dump(counter);
			break;
		case NAME:
			getName();
			break;
		case TEMPERATURE:
			getTemperatures();
			break;
		}
	}
}

void onPulse1() {
	onPulse(COUNTER_1);
}

void onPulse2() {
	onPulse(COUNTER_2);
}

void onPulse(int counter) {
	changeLedState(counter);

	lastTime[counter] = pulseTime[counter]; //used to measure time between pulses.
	pulseTime[counter] = micros();

	pulseCount[counter]++; //pulseCounter               

	long timeDiff;
	if (pulseTime[counter] < lastTime[counter]) { // If there has been an overflow in micros()
		timeDiff = 4294967295 - lastTime[counter] + pulseTime[counter];
	} else {
		timeDiff = (pulseTime[counter] - lastTime[counter]);
	}

	power[counter] = int(3600000000.0 / timeDiff / ppwh[counter]); //Calculate power
}

void confirmCount(byte ind) {
	String inString = "";
	//  Serial.println("Confirm:");
	while (true) {
		int inChar = Serial.read();
		if (isDigit(inChar)) {
			// convert the incoming byte to a char 
			// and add it to the string:
			inString += (char) inChar;
		}
		// if you get a newline, print the string,
		// then the string's value:
		if (inChar == '\n') {
			unsigned long ll = inString.toInt();
			pulseCount[ind] -= ll;
			return;
		}
	}
}

void getName() {
	Serial.println("Main power");
}

void dump(int dummy) {
	Serial.println("Dump.");
	getName();
	getCountText(COUNTER_1);
	getCountText(COUNTER_2);
	getLedState(COUNTER_1);
	getLedState(COUNTER_2);
	
	  Serial.print("Found ");
	  Serial.print(sensors.getDeviceCount(), DEC);
	  Serial.println(" temperature devices.");

	sensors.requestTemperatures(); // Send the command to get temperatures
	// Loop through each device, print out temperature data
	for (int i = 0; i < sensors.getDeviceCount(); i++) {
		// Search the wire for address
		if (sensors.getAddress(tempDeviceAddress, i)) {
			printTemperature(tempDeviceAddress); 
			Serial.println();
		}
	}
}

void getLedState(int counter) {
	Serial.print("LED ");
	Serial.print(counter, DEC);
	Serial.print(" at pin ");
	Serial.print(LED_PINS[counter], DEC);
	Serial.print(" has state ");
	Serial.println(ledState[counter]);
}

void getCountText(byte ind) {
	Serial.print(ind, DEC);
	Serial.print(",pulses:");
	Serial.print(pulseCount[ind], DEC);
	Serial.print(",power:");
	Serial.println(power[ind], DEC);
}

void changeLedState(byte counter) {
	ledState[counter] = !ledState[counter];
	digitalWrite(LED_PINS[counter], ledState[counter]);
}

void getTemperatures(void) {
	// call sensors.requestTemperatures() to issue a global temperature 
	// request to all devices on the bus
	sensors.requestTemperatures(); // Send the command to get temperatures
	// Loop through each device, print out temperature data
	for (int i = 0; i < sensors.getDeviceCount(); i++) {
		// Search the wire for address
		if (sensors.getAddress(tempDeviceAddress, i)) {
			printTemperature(tempDeviceAddress);
			if(i < sensors.getDeviceCount() -1){
				Serial.print(",");
			}
		}
	}
	Serial.println();
}

void printTemperature(DeviceAddress deviceAddress) {
	float tempC = sensors.getTempC(deviceAddress);
	printAddress(deviceAddress);
	Serial.print(":");
	Serial.print(tempC);
}

void printAddress(DeviceAddress deviceAddress) {
	for (uint8_t i = 0; i < 8; i++) {
		if (deviceAddress[i] < 16) {
			Serial.print("0");
		}
		Serial.print(deviceAddress[i], HEX);
	}
}

