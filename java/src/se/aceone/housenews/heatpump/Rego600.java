package se.aceone.housenews.heatpump;

/**
 * Rego600 heat pump controler interface
 * See http://rago600.sourceforge.net/ for more info.
 * Thanks Jindrich Fucik for diging this out. 
 * If You are using this java code I think You have to drop Jindrich a post card.  
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Rego600 {

	static final byte REGO_600 = (byte) 0x81;

	// 1 - address 5char 16bit number Read from front panel (keyboard+leds) {reg09FF+xx}
	private static final byte PANEL_R = (byte) 0x00;
	// 2 - address + data 1char confirm Write to front panel (keyboard+leds) {reg 09FF+xx}
	private static final byte PANEL_W = (byte) 0x01;
	// 1 - address 5char 16bit number Read from system register (heat curve, temperatures, devices) {reg 1345+xx}
	private static final byte REGISTER_R = (byte) 0x02;
	// 2 - address + data 1char confirm Write into system register (heat curve, temperatures, devices) {reg 1345+xx}
	private static final byte REGISTER_W = (byte) 0x03;
	// 1 - address 5char 16bit number Read from timer registers {reg 1B45+xx}
	private static final byte TIMER_R = (byte) 0x04;
	// 2 - address + data 1char confirm Write into timer registers {reg 1B45+xx}
	private static final byte TIMER_W = (byte) 0x05;
	// 1 - address 5char 16bit number Read from register 1B61 {reg 1B61+xx}
	private static final byte REGISTER_HI_R = (byte) 0x06;
	// 2 - address + data 1char confirm Write into register 1B61 {1B61+xx}
	private static final byte REGISTER_HI_W = (byte) 0x07;
	// 1 - display line 42char text line Read from display {0AC7+15h*xx}
	private static final byte DISPLAY_R = (byte) 0x20;
	// 0 42char text line Read last error line [4100/00]
	private static final byte ERROR_LAST = (byte) 0x40;
	// 0 42char text line Read previous error line (prev from last reading) [4100/01]
	private static final byte ERROR_PREVIUS = (byte) 0x42;
	// 0 5char 16bit number Read rego version {constant 0258 = 600 ?Rego 600?}
	public static final byte VERSION = (byte) 0x7F;

	// Rego636-... Register
	// Sensor values
	public static final int RADIATOR_RETURN_GT1 = 0x020B; // Radiator return[GT1]
	public static final int OUTDOOR_TEMP_GT2 = 0x020C; // Outdoor [GT2]
	public static final int HOT_WATER_GT3 = 0x020D; // Hot water [GT3]
	public static final int FORWARD_GT4 = 0x020E; // Forward [GT4]
	public static final int ROOM_GT5 = 0x020F; // Room [GT5]
	public static final int COMPRESSOR_GT6 = 0x0210; // Compressor [GT6]
	public static final int HEAT_FLUID_OUT_GT8 = 0x0211; // Heat fluid out [GT8]
	public static final int HEAT_FLUID_IN_GT9 = 0x0212; // Heat fluid in [GT9]
	public static final int TRANSFER_FLUID_IN_GT10 = 0x0213; // Cold fluid in [GT10]
	public static final int TRANSFER_FLUID_OUT_GT11 = 0x0214; // Cold fluid out [GT11]
	public static final int HOT_WATER_EXTERNAL_GT3X = 0x0215; // External hot water [GT3x]

	// Device values
	public static final int GROUND_LOOP_PUMP_P3 = 0x01FF; // Ground loop pump [P3]
	public static final int COMPRESSOR = 0x0200; // Compressor
	public static final int ADDITIONAL_HEAT_STEP_1 = 0x0201; // Additional heat 3kW
	public static final int ADDITIONAL_HEAT_STEP_2 = 0x0202; // Additional heat 6kW
	public static final int RADIATOR_PUMP_P1 = 0x0205; // Radiator pump [P1]
	public static final int HEAT_CARRIER_PUMP_P2 = 0x0206; // Heat carrier pump [P2]
	public static final int THREE_WAY_VALVE = 0x0207; // Tree-way valve [VXV]
	public static final int ALARM = 0x0208; // Alarm

	// Control data
	public static final int ADDED_HEAT_POWER = 0x006C; // Add heat power in %
	public static final int GT4_TARGET_VALUE = 0x006D; // GT4 Target value
	public static final int GT1_TARGET_VALUE = 0x006E; // GT1 Target value
	public static final int GT1_ON_VALUE = 0x006F; // GT1 On value
	public static final int GT1_OFF_VALUE = 0x0070; // GT1 Off value
	public static final int GT3_TARGET_VALUE = 0x002B; // GT3 Target value
	public static final int GT3_ON_VALUE = 0x0073; // GT3 On value
	public static final int GT3_OFF_VALUE = 0x0074; // GT3 Off value

	// Settings
	public static final int HEAT_CURVE = 0x0000; // Heat curve
	public static final int HEAT_CURVE_FINE = 0x0001; // Heat curve fine adj.
	public static final int HEAT_CURVE_COUPLING_DIFF = 0x0002; // Heat curve coupling diff.
	public static final int ADJ_CURVE_AT_N35_DGR_OUT = 0x0008; // Adj. curve at -35° out
	public static final int ADJ_CURVE_AT_N30_DGR_OUT = 0x000A; // Adj. curve at -30° out
	public static final int ADJ_CURVE_AT_N25_DGR_OUT = 0x000C; // Adj. curve at -25° out
	public static final int ADJ_CURVE_AT_N20_DGR_OUT = 0x000E; // Adj. curve at -20° out
	public static final int ADJ_CURVE_AT_N15_DGR_OUT = 0x0010; // Adj. curve at -15° out
	public static final int ADJ_CURVE_AT_N10_DGR_OUT = 0x0012; // Adj. curve at -10° out
	public static final int ADJ_CURVE_AT_N5_DGR_OUT = 0x0014; // Adj. curve at -5° out
	public static final int ADJ_CURVE_AT_0_DGR_OUT = 0x0016; // Adj. curve at 0° out
	public static final int ADJ_CURVE_AT_5_DGR_OUT = 0x0018; // Adj. curve at 5° out
	public static final int ADJ_CURVE_AT_10_DGR_OUT = 0x001A; // Adj. curve at 10° out
	public static final int ADJ_CURVE_AT_15_DGR_OUT = 0x001C; // Adj. curve at 15° out
	public static final int ADJ_CURVE_AT_20_DGR_OUT = 0x001E; // Adj. curve at 20° out
	public static final int INDOOR_TEMP_SETTING = 0x0021; // Indoor temp setting
	public static final int CURVE_INFL_IN_TEMP = 0x0022; // Curve infl. by in-temp.

	private static final String DATE_FORMAT = "yyMMdd HH:mm:ss";

	private OutputStream outputStream;
	private InputStream inputStream;

	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

	public Rego600(OutputStream os, InputStream is) {
		outputStream = os;
		inputStream = is;
	}

	public String getDisplayLine(int line) throws IOException, DataException {
		return getText(REGO_600, DISPLAY_R, line);
	}

	public Alarm getLastAlarm() throws IOException, DataException {
		return getAlarm(ERROR_LAST);
	}

	public Alarm getPreviusAlarm() throws IOException, DataException {
		return getAlarm(ERROR_PREVIUS);
	}

	public Alarm getAlarm(byte command) throws IOException, DataException {
		byte[] data = getLongData(REGO_600, command, 0);
		if (data[0] == 255) {
			return null;
		}
		Date time = null;
		try {
			time = sdf.parse(new String(data, 1, 15));
		} catch (ParseException e) {
			// return null;
		}
		return new Alarm(data[0], time, data);
	}

	public String getText(byte address, byte command, int register) throws IOException, DataException {
		return result2text(getLongData(address, command, register));
	}

	byte[] getLongData(byte address, byte command, int register) throws IOException, DataException {
		byte[] request = buildRequest(address, command, register, 00);
		int length = 42;
		return sendRead(request, length);
	}

	private byte[] sendRead(byte[] request, int length) throws IOException, DataException {
		int i = 0;
		while (true) {
			i++;
			try {
				send(request);
				return read(length);
			} catch (DataException e) {
				if (i == 5) {
					throw e;
				}
			}
		}
	}

	public int getPanelValue(int register) throws IOException, DataException {
		return getValue(REGO_600, PANEL_R, register);
	}

	public int getRegisterValue(int register) throws IOException, DataException {
		return getValue(REGO_600, REGISTER_R, register);
	}

	public double getRegisterValueTemperature(int register) throws IOException, DataException {
		if (register >= RADIATOR_RETURN_GT1 && register <= HOT_WATER_EXTERNAL_GT3X) {
			register -= 2;
		}
		double d = getValue(REGO_600, REGISTER_R, register) * .1;
		BigDecimal bd = new BigDecimal(d);
		bd = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	public int getValue(byte address, byte command, int register) throws IOException, DataException {
		byte[] request = buildRequest(address, command, register, 00);
		int length = 5;
		byte[] read = sendRead(request, length);
		return result2int(read);
	}

	private byte[] buildRequest(byte address, byte command, int register, int value) {
		byte[] request = { address, command, 00, 00, 00, 00, 00, 00, 00 };
		byte sum = 0;

		for (int poc = 0; poc <= 2; poc++) {
			byte b = (byte) (register & 0x7F);
			request[4 - poc] = b;
			sum = (byte) (sum ^ b);
			register = register >> 7;
		}
		for (int poc = 0; poc <= 2; poc++) {
			byte b = (byte) (value & 0x7F);
			request[7 - poc] = b;
			sum = (byte) (sum ^ b);
			value = value >> 7;
		}
		request[8] = sum;
		return request;
	}

	// # convert received values (array) to temperature
	private int result2int(byte[] result) {
		int num = result[1] * 16384 + result[2] * 128 + result[3];
		if (result[1] >= 2) {
			num -= (65536);
		}
		return num;
	}

	// # convert 42 bytes array to text
	private String result2text(byte[] result) {
		StringBuffer text = new StringBuffer();
		for (int i = 1; i <= 20; i++) {
			int c = (result[(i * 2)] << 4) + result[1 + (i * 2)];
			text.append((char) c);
		}
		return text.toString();
	}

	private byte[] read(int length) throws DataException, IOException {
		byte[] result = new byte[length];
		int i = 0;
		while (inputStream.available() < length) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			if (i >= 5) {
				throw new DataException("No data recived");
			}
			i++;
		}
		inputStream.read(result);
		if (result[0] != 01) {
			throw new DataException("Bad header " + result[0] + " length: " + result.length);
		}
		int checksum = 0;
		for (int j = 1; j < result.length - 1; j++) {
			checksum ^= result[j];
		}

		// if (result[result.length - 1] != checksum) {
		// throw new DataException("Bad checksum " + result[result.length - 1] +
		// " != " + checksum);
		// }

		return result;
	}

	private void send(byte[] data) throws IOException {
		outputStream.write(data);
	}

	public static void main(String[] args) throws IOException, DataException {
		// try {
		// System.out.println(new Alarm((byte) 2, new Date(), null));
		// Rego600 rego600 = new Rego600("COM8");
		// try {
		// double temp = rego600.getRegisterValueTemperature(HEAT_CURVE);
		// System.out.println("Heat curve 0x0000=" + temp);
		// } catch (DataException e) {
		// System.out.println(e.getMessage());
		// }
		// try {
		// double temp = rego600.getRegisterValueTemperature(RADIATOR_RETURN_GT1);
		// System.out.println("Radiator return 0x020B=" + temp);
		// } catch (DataException e) {
		// System.out.println(e.getMessage());
		// }
		//
		// Alarm alarm = rego600.getLastAlarm();
		// System.out.println(alarm);
		// while ((alarm = rego600.getPreviusAlarm()) != null) {
		// System.out.println(alarm);
		// }
		//
		// rego600.close();
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// } finally {
		// System.exit(0);
		// }
	}

	public static String translateRegister(int register) {
		switch (register) {
		case RADIATOR_RETURN_GT1:
			return "Radiator return [GT1]";
		case OUTDOOR_TEMP_GT2:
			return "Outdoor [GT2]";
		case HOT_WATER_GT3:
			return "Hot water [GT3]";
		case FORWARD_GT4:
			return "Forward [GT4]";
		case ROOM_GT5:
			return "Room [GT5]";
		case COMPRESSOR_GT6:
			return "Compressor [GT6]";
		case HEAT_FLUID_OUT_GT8:
			return "Heat fluid out [GT8]";
		case HEAT_FLUID_IN_GT9:
			return "Heat fluid in [GT9]";
		case TRANSFER_FLUID_IN_GT10:
			return "Cold fluid in[GT10]";
		case TRANSFER_FLUID_OUT_GT11:
			return "Cold fluid out[GT11]";
		case HOT_WATER_EXTERNAL_GT3X:
			return "External hotwater [GT3x]";

			// Device values
		case GROUND_LOOP_PUMP_P3:
			return "Ground loop pump [P3]";
		case COMPRESSOR:
			return "Compresor";
		case ADDITIONAL_HEAT_STEP_1:
			return "Additional heat 3kW";
		case ADDITIONAL_HEAT_STEP_2:
			return "Additional heat 6kW";
		case RADIATOR_PUMP_P1:
			return "Radiator pump [P1]";
		case HEAT_CARRIER_PUMP_P2:
			return "Heat carrier pump [P2]";
		case THREE_WAY_VALVE:
			return "Tree-way valve [VXV]";
		case ALARM:
			return "Alarm";

			// Control data
		case ADDED_HEAT_POWER:
			return "Add heat power in %";
		case GT4_TARGET_VALUE:
			return "GT4 Target value";
		case GT1_TARGET_VALUE:
			return "GT1 Target value";
		case GT1_ON_VALUE:
			return "GT1 On value";
		case GT1_OFF_VALUE:
			return "GT1 Off value";
		case GT3_TARGET_VALUE:
			return "GT3 Target value";
		case GT3_ON_VALUE:
			return "GT3 On value";
		case GT3_OFF_VALUE:
			return "GT3 Off value";

			// Settings
		case HEAT_CURVE:
			return "Heat curve";
		case HEAT_CURVE_FINE:
			return "Heat curve fine adj.";
		case HEAT_CURVE_COUPLING_DIFF:
			return "Heat curve coupling diff.";
		case ADJ_CURVE_AT_N35_DGR_OUT:
			return "Adj. curve at -35° out";
		case ADJ_CURVE_AT_N30_DGR_OUT:
			return "Adj. curve at -30° out";
		case ADJ_CURVE_AT_N25_DGR_OUT:
			return "Adj. curve at -25° out";
		case ADJ_CURVE_AT_N20_DGR_OUT:
			return "Adj. curve at -20° out";
		case ADJ_CURVE_AT_N15_DGR_OUT:
			return "Adj. curve at -15° out";
		case ADJ_CURVE_AT_N10_DGR_OUT:
			return "Adj. curve at -10° out";
		case ADJ_CURVE_AT_N5_DGR_OUT:
			return "Adj. curve at -5° out";
		case ADJ_CURVE_AT_0_DGR_OUT:
			return "Adj. curve at 0° out";
		case ADJ_CURVE_AT_5_DGR_OUT:
			return "Adj. curve at 5° out";
		case ADJ_CURVE_AT_10_DGR_OUT:
			return "Adj. curve at 10° out";
		case ADJ_CURVE_AT_15_DGR_OUT:
			return "Adj. curve at 15° out";
		case ADJ_CURVE_AT_20_DGR_OUT:
			return "Adj. curve at 20° out";
		case INDOOR_TEMP_SETTING:
			return "Indoor temp setting";
		case CURVE_INFL_IN_TEMP:
			return "Curve infl. by in-temp.";
		}
		return null;
	}
}
