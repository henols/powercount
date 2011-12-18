package se.aceone.housenews.heatpump;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Alarm {
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
	private final byte type;
	private final Date time;

	private final byte[] data;

	public Alarm(byte type, Date time, byte[] data) {
		this.type = type;
		this.time = time;
		this.data = data;
	}

	public Date getTime() {
		return time;
	}

	@Override
	public String toString() {
		return /*sdf.format(getTime())+ " "+*/getText();
	}
	
	public byte getType() {
		return type;
	}

	
	
	public String getText() {
		switch (getType()) {
		case 0:
			return "Sensor radiator return (GT1)";
		case 1:
			return "Outdoor sensor (GT2)";
		case 2:
			return "Sensor hot water (GT3)";
		case 3:
			return "Mixing valve sensor (GT4)";
		case 4:
			return "Room sensor (GT5)";
		case 5:
			return "Sensor compressor (GT6)";
		case 6:
			return "Sensor heat tran fluid out (GT8)";
		case 7:
			return "Sensor heat tran fluid in (GT9)";
		case 8:
			return "Sensor cold tran fluid in (GT10)";
		case 9:
			return "Sensor cold tran fluid in (GT11)";
		case 10:
			return "Compresor circuit switch";
		case 11:
			return "Electrical cassette";
		case 12:
			return "HTF C=pump switch (MB2)";
		case 13:
			return "Low pressure switch (LP)";
		case 14:
			return "High pressure switch (HP)";
		case 15:
			return "High return HP (GT9)";
		case 16:
			return "HTF out max (GT8)";
		case 17:
			return "HTF in under limit (GT10)";
		case 18:
			return "HTF out under limit (GT11)";
		case 19:
			return "Compressor superhear (GT6)";
		case 20:
			return "3-phase incorrect order";
		case 21:
			return "Power failure";
		case 22:
			return "Varmetr. delta hoch";
		}
		return "Unknown alarm (" + getType() + ")";
	}
}
