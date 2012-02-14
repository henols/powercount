#include "Rego600.h"

#include <inttypes.h>
#include <stdint.h>
#include "Arduino.h"

int Rego600::getRegisterValue(uint16_t reg) {
	return getValue(REGO_600, REGISTER_R, reg);
}

double Rego600::getRegisterValueTemperature(uint16_t reg) {
	if (reg >= RADIATOR_RETURN_GT1
			&& reg <= HOT_WATER_EXTERNAL_GT3X) {
		reg -= 2;
	}
	double d = getValue(REGO_600, REGISTER_R, reg) * .1;
	return d;
}

int Rego600::getValue(uint8_t address, uint8_t command, uint16_t reg) {
	uint8_t *request = buildRequest(address, command, reg, 00);
	sendRead(request, shortResult);
	return result2int(shortResult);
}

char *Rego600::getDisplayLine(uint8_t line) {
	return getText(REGO_600, DISPLAY_R, line);
}

char *Rego600::getText(uint8_t address, uint8_t command, uint16_t reg) {
	char *result = getLongData(address, command, reg);
	return result2text(result);
}

char *Rego600::getLongData(uint8_t address, uint8_t command, uint16_t reg) {
	uint8_t *request = buildRequest(address, command, reg, 00);
	sendRead(request, longResult);
	return longResult;
}

uint8_t *Rego600::buildRequest(uint8_t address, uint8_t command, uint16_t reg, int value) {
	request[0] = address;
	request[1] = command;

	uint8_t sum = 0;

	for (int poc = 0; poc <= 2; poc++) {
		uint8_t b = (reg & 0x7F);
		request[4 - poc] = b;
		sum = (sum ^ b);
		reg = reg >> 7;
	}
	for (int poc = 0; poc <= 2; poc++) {
		uint8_t b = (value & 0x7F);
		request[7 - poc] = b;
		sum = (sum ^ b);
		value = value >> 7;
	}
	request[8] = sum;
	return request;
}

int Rego600::result2int(char *result) {
	int num = result[1] * 16384 + result[2] * 128 + result[3];
	if (result[1] >= 2) {
		num -= (65536);
	}
	return num;
}

char *Rego600::result2text(char *result) {
	for (int i = 1; i <= 20; i++) {
		char c = (result[(i * 2)] << 4) + result[1 + (i * 2)];
		textResult[i]=c;
	}
	return textResult;
}

void Rego600::sendRead(uint8_t *request, char *result) {
	send(request);
	read(result);
}

void Rego600::read(char *result) {
//	while (inputStream::available() < length) {
//			delay(100);
//	}
//	inputStream::read(result);
//	if (result[0] != 01) {
//		return 0;
//	}
//	int checksum = 0;
//	for (int j = 1; j < sizeof(result) / sizeof(result[0]) - 1; j++) {
//		checksum ^= result[j];
//	}
//	Serial.read(result);
}

void Rego600::send(uint8_t *data) {
//	outputStream::write(data);
}
