
#define RESET_TIME 60000
#define COUNTER_1 0
#define COUNTER_2 1

const byte CONFIRM = 'c';
const byte NAME = '3';
const byte PULSES_TEXT = '4';

const int BAUD_RATE = 19200;
const byte LED_PIN_1 = 13; // LED connected to digital pin 13
const byte LED_PIN_2 = 14; // LED connected to digital pin 13
const byte BT_RESET_PIN = 15; // BlueToothe module reset

const byte LED_PINS[2] = {LED_PIN_1,LED_PIN_2};

const byte PULSE_PIN_1 = 2; //
const byte PULSE_PIN_2 = 3; //



long pulseCount[2] = { 0, 0 }; //Number of pulses, used to measure energy.
unsigned long pulseTime[2], lastTime[2]; //Used to measure power.
double power[2]; //power and energy
int ppwh[2] = { 1, 1 }; ////1000 pulses/kwh = 1 pulse per wh - Number of pulses per wh - found or set on the meter.

long lastSerial;

boolean ledState = false;

unsigned long pulses = 0;

void setup() {
	Serial.begin(BAUD_RATE);
	pinMode(LED_PIN_1, OUTPUT); // sets the digital pin as output
	pinMode(LED_PIN_2, OUTPUT); // sets the digital pin as output
	pinMode(BT_RESET_PIN, OUTPUT); // sets the digital pin as output
	resetBtModule();

	attachInterrupt(0, onPulse1, FALLING); // KWH interrupt attached to IRQ 1  = pin2 
	attachInterrupt(1, onPulse2, FALLING); // KWH interrupt attached to IRQ 1  = pin3 

}

void loop() {
	if (Serial.available() > 0) {
		lastSerial = millis();
		// get incoming byte:
		byte inByte = Serial.read();
		switch (inByte) {
		case PULSES_TEXT:
			getCountText(Serial.read());
			break;
		case CONFIRM:
			confirmCount(Serial.read());
			break;
		case NAME:
			getName();
			break;
		}
	}
	if ((lastSerial + RESET_TIME) < millis()) {
		resetBtModule();
	}

}

void onPulse1() {
	onPulse(COUNTER_1);  
}

void onPulse2() {
	onPulse(COUNTER_2);  
}

void onPulse(int counter) {
	digitalWrite(LED_PINS[counter], HIGH); //flash LED - very quickly  

	lastTime[counter] = pulseTime[counter]; //used to measure time between pulses.
	pulseTime[counter] = micros();

	pulseCount[counter]++; //pulseCounter               

	power[counter] = int((3600000000.0 / (pulseTime[counter] - lastTime[counter])) / ppwh[counter]); //Calculate power

	digitalWrite(LED_PINS[counter], LOW);
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

void resetBtModule() {
	digitalWrite(BT_RESET_PIN, LOW);
	delay(10);
	digitalWrite(BT_RESET_PIN, HIGH);
	lastSerial = millis();
}

void getName() {
	Serial.println("Main power");
}

void getCountText(byte ind) {
	Serial.print("pulses:");
	Serial.println(pulseCount[ind], DEC);
	Serial.print(",power:");
	Serial.println(power[ind], DEC);
}

