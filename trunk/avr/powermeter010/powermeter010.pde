// 
// 66902


const byte PULSES = '1';
const byte CONFIRM = 'c';
const byte NAME = '3';
const byte PULSES_TEXT = '4';

const int BAUD_RATE = 19200;
const byte LED_PIN = 13; // LED connected to digital pin 13

const byte PULSE_PIN_1 = 2; //

const unsigned long secPerYear = 31556952UL;
const unsigned long msPerHour = 3600000UL;
unsigned long tickLastX, tickLastY;
boolean ledState = false;

unsigned long pulses = 0;

void setup() {
	pinMode(PULSE_PIN_1, INPUT);
	Serial.begin(BAUD_RATE);
	pinMode(LED_PIN, OUTPUT); // sets the digital pin as output
}

void loop() {
	static byte lastX = 0;
	static byte newX = 0;
	newX = digitalRead(PULSE_PIN_1);
	if (newX == 1 && lastX == 0) {
		pulses++;
		ledState = !ledState;
		digitalWrite(LED_PIN, ledState);
		last_pulse_time = millis();
//		if (first_pulse_time == 0) {// we were reset
//			first_pulse_time = last_pulse_time;
//		}

	}
	lastX = newX;

	if (Serial.available() > 0) {
		// get incoming byte:
		byte inByte = Serial.read();
		switch (inByte) {
		case PULSES:
			getCount();
			break;
		case PULSES_TEXT:
			getCountText();
			break;
		case CONFIRM:
			confirmCount();
			break;
		case NAME:
			getName();
			break;
		}

		//    Serial.print();    
	}

}

void getCount() {
	long p = pulses;
	Serial.write(p >> 24);
	Serial.write(p >> 16);
	Serial.write(p >> 8);
	Serial.write(p);
}

void confirmCount() {
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
			pulses -= ll;
			//      Serial.print("Value:");
			//      Serial.print(ll, DEC);
			//      Serial.print( " pulses:");
			//      Serial.println(pulses,DEC);
			// clear the string for new input:
			return;
		}
	}
}

void getName() {
	Serial.println("Main power");
}

void getCountText() {
	Serial.print("pulses:");
	Serial.println(pulses, DEC);
}

