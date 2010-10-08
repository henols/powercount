/*
 Title:		SerialCom2313.c
 Target:		Atmel ATtiny2313
 Environment:	AVR-GCC

 Note: The makefile is set up for a 2313 running at
 8 MHz from its internal oscillator.


 //  This program sends an ASCII A (byte of value 65) on startup
 //  and repeats that until it gets some data in.
 //  Then it waits for a byte in the serial port, and
 //  sends three (faked) sensor values whenever it gets a byte in.




 -------------------------------------------------
 USAGE: How to compile and install



 A makefile is provided to compile and install this program using AVR-GCC and avrdude.

 To use it, follow these steps:
 1. Update the header of the makefile as needed to reflect the type of AVR programmer that you use.
 2. Open a terminal window and move into the directory with this file and the makefile.
 3. At the terminal enter
 make clean   <return>
 make all     <return>
 make program <return>
 4. Make sure that avrdude does not report any errors.  If all goes well, the last few lines output by avrdude
 should look something like this:

 avrdude: verifying ...
 avrdude: XXXX bytes of flash verified

 avrdude: safemode: lfuse reads as E2
 avrdude: safemode: hfuse reads as D9
 avrdude: safemode: efuse reads as FF
 avrdude: safemode: Fuses OK

 avrdude done.  Thank you.


 If you a different programming environment, make sure that you copy over
 the fuse settings from the makefile.


 -------------------------------------------------
 
 */

#include <avr/io.h> 
//#include <avr/interrupt.h>

#define F_CPU 10000000	// Oscillator frequency.
#define BaudRate 9600
#define MYUBRR (F_CPU / 16 / BaudRate ) - 1 

void delayLong() {
	unsigned int delayvar;
	delayvar = 0;
	while (delayvar <= 65500U) {
		asm("nop");
		delayvar++;
	}
}

unsigned char serialCheckRxComplete(void) {
	return (UCSRA & _BV(RXC)); // nonzero if serial data is available to read.
}

unsigned char serialCheckTxReady(void) {
	return (UCSRA & _BV(UDRE)); // nonzero if transmit register is ready to receive new data.
}

unsigned char serialRead(void) {
	while (serialCheckRxComplete() == 0) // While data is NOT available to read
	{
		;;
	}
	return UDR;
}

void serialWrite(unsigned char DataOut) {
	while (serialCheckTxReady() == 0) // while NOT ready to transmit
	{
		;;
	}
	UDR = DataOut;
}

void establishContact() {
	while (serialCheckRxComplete() == 0) {
		serialWrite('A');
		delayLong();
		delayLong();
		delayLong();
		delayLong();
		delayLong();
		delayLong();
		delayLong();
	}
}

int main(void) {

	//Interrupts are not needed in this program. You can optionally disable interrupts.
	//asm("cli");		// DISABLE global interrupts.


	DDRD = _BV(1) | _BV(6);
	DDRB = _BV(6);

	//Serial Initialization

	/*Set baud rate */
	UBRRH = (unsigned char) (MYUBRR >> 8);
	UBRRL = (unsigned char) MYUBRR;
	/* Enable receiver and transmitter   */
	UCSRB = (1 << RXEN) | (1 << TXEN);
	/* Frame format: 8data, No parity, 1stop bit */
	UCSRC = (3 << UCSZ0);

	int firstSensor = 0; // first analog sensor
	int secondSensor = 0; // second analog sensor
	int thirdSensor = 0; // digital sensor
	int inByte = 0; // incoming serial byte


	PORTD |= _BV(6); // Turn on LED @ PB6


	establishContact(); // send a byte to establish contact until Processing responds

	PORTD &= 191U; // Turn off LED


	for (;;) // main loop
	{

		if (serialCheckRxComplete()) {
			PORTD |= _BV(6); // Turn on LED @ PB6

			inByte = serialRead();

			// Simulated data!
			firstSensor++;

			secondSensor = firstSensor * firstSensor;

			thirdSensor = firstSensor + secondSensor;

			serialWrite(firstSensor & 255U);
			serialWrite(secondSensor & 255U);
			serialWrite(thirdSensor & 255U);

			PORTD &= 191U; // Turn off LED

		}

	} //End main loop.
	return 0;
}
