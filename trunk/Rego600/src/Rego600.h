#ifndef Rego600_h
#define Rego600_h

#include <inttypes.h>

#define REGO_600 0x81

// 1 - address 5char 16bit number Read from front panel (keyboard+leds) {reg09FF+xx}
#define PANEL_R 0x00
// 2 - address + data 1char confirm Write to front panel (keyboard+leds) {reg 09FF+xx}
#define PANEL_W 0x01
// 1 - address 5char 16bit number Read from system register (heat curve, temperatures, devices) {reg 1345+xx}
#define REGISTER_R 0x02
// 2 - address + data 1char confirm Write into system register (heat curve, temperatures, devices) {reg 1345+xx}
#define REGISTER_W 0x03
// 1 - address 5char 16bit number Read from timer registers {reg 1B45+xx}
#define TIMER_R 0x04
// 2 - address + data 1char confirm Write into timer registers {reg 1B45+xx}
#define TIMER_W 0x05
// 1 - address 5char 16bit number Read from register 1B61 {reg 1B61+xx}
#define REGISTER_HI_R 0x06
// 2 - address + data 1char confirm Write into register 1B61 {1B61+xx}
#define REGISTER_HI_W 0x07
// 1 - display line 42char text line Read from display {0AC7+15h*xx}
#define DISPLAY_R 0x20
// 0 42char text line Read last error line [4100/00]
#define ERROR_LAST 0x40
// 0 42char text line Read previous error line (prev from last reading) [4100/01]
#define ERROR_PREVIUS 0x42
// 0 5char 16bit number Read rego version {constant 0258 600 ?Rego 600?}

#define VERSION 0x7F

// Rego636-... Register
// Sensor values
#define RADIATOR_RETURN_GT1 0x020B // Radiator return[GT1]
#define OUTDOOR_TEMP_GT2 0x020C // Outdoor [GT2]
#define HOT_WATER_GT3 0x020D // Hot water [GT3]
#define FORWARD_GT4 0x020E // Forward [GT4]
#define ROOM_GT5 0x020F // Room [GT5]
#define COMPRESSOR_GT6 0x0210 // Compressor [GT6]
#define HEAT_FLUID_OUT_GT8 0x0211 // Heat fluid out [GT8]
#define HEAT_FLUID_IN_GT9 0x0212 // Heat fluid in [GT9]
#define TRANSFER_FLUID_IN_GT10 0x0213 // Cold fluid in [GT10]
#define TRANSFER_FLUID_OUT_GT11 0x0214 // Cold fluid out [GT11]
#define HOT_WATER_EXTERNAL_GT3X 0x0215 // External hot water [GT3x]

// Device values
#define GROUND_LOOP_PUMP_P3 0x01FF // Ground loop pump [P3]
#define COMPRESSOR 0x0200 // Compressor
#define ADDITIONAL_HEAT_STEP_1 0x0201 // Additional heat 3kW
#define ADDITIONAL_HEAT_STEP_2 0x0202 // Additional heat 6kW
#define RADIATOR_PUMP_P1 0x0205 // Radiator pump [P1]
#define HEAT_CARRIER_PUMP_P2 0x0206 // Heat carrier pump [P2]
#define THREE_WAY_VALVE 0x0207 // Tree-way valve [VXV]
#define ALARM 0x0208 // Alarm

// Control data
#define ADDED_HEAT_POWER 0x006C // Add heat power in %
#define GT4_TARGET_VALUE 0x006D // GT4 Target value
#define GT1_TARGET_VALUE 0x006E // GT1 Target value
#define GT1_ON_VALUE 0x006F // GT1 On value
#define GT1_OFF_VALUE 0x0070 // GT1 Off value
#define GT3_TARGET_VALUE 0x002B // GT3 Target value
#define GT3_ON_VALUE 0x0073 // GT3 On value
#define GT3_OFF_VALUE 0x0074 // GT3 Off value

// Settings
#define HEAT_CURVE 0x0000 // Heat curve
#define HEAT_CURVE_FINE 0x0001 // Heat curve fine adj.
#define HEAT_CURVE_COUPLING_DIFF 0x0002 // Heat curve coupling diff.
#define ADJ_CURVE_AT_N35_DGR_OUT 0x0008 // Adj. curve at -35C out
#define ADJ_CURVE_AT_N30_DGR_OUT 0x000A // Adj. curve at -30C out
#define ADJ_CURVE_AT_N25_DGR_OUT 0x000C // Adj. curve at -25C out
#define ADJ_CURVE_AT_N20_DGR_OUT 0x000E // Adj. curve at -20C out
#define ADJ_CURVE_AT_N15_DGR_OUT 0x0010 // Adj. curve at -15C out
#define ADJ_CURVE_AT_N10_DGR_OUT 0x0012 // Adj. curve at -10C out
#define ADJ_CURVE_AT_N5_DGR_OUT 0x0014 // Adj. curve at -5C out
#define ADJ_CURVE_AT_0_DGR_OUT 0x0016 // Adj. curve at 0C out
#define ADJ_CURVE_AT_5_DGR_OUT 0x0018 // Adj. curve at 5C out
#define ADJ_CURVE_AT_10_DGR_OUT 0x001A // Adj. curve at 10C out
#define ADJ_CURVE_AT_15_DGR_OUT 0x001C // Adj. curve at 15C out
#define ADJ_CURVE_AT_20_DGR_OUT 0x001E // Adj. curve at 20C out
#define INDOOR_TEMP_SETTING 0x0021 // Indoor temp setting
#define CURVE_INFL_IN_TEMP 0x0022 // Curve infl. by in-temp.


class Rego600 {
private:
	uint8_t request[9];
	char shortResult[5];
	char longResult[42];
	char textResult[20];

	uint8_t *buildRequest(uint8_t address, uint8_t command, uint16_t reg, int value);

	int result2int(char *result);

	char *result2text(char *result);

	void sendRead(uint8_t *request, char *result);

	void read(char *result);

	void send(uint8_t *request);

public:
	int getRegisterValue(uint16_t reg);

	double getRegisterValueTemperature(uint16_t reg);

	int getValue(uint8_t address, uint8_t command, uint16_t reg);

	char *getDisplayLine(uint8_t line);

	char *getText(uint8_t address, uint8_t command, uint16_t reg);

	char *getLongData(uint8_t address, uint8_t command, uint16_t reg);

};

#endif
