package se.aceone.mesh;

public class Logger implements Protocol {
	private final String name;
	private final String key;
	private final String type;

	public Logger(String name) {
		this(name, null);
	}

	public Logger(String name, String key) {
		this(null, name, key);
	}

	public Logger(String type, String name, String key) {
		this.type = type;
		this.name = name;
		this.key = key;
	}

	public void info(String message) {
		writeLog(INFO, message);
	}

	public void warning(String message) {
		writeLog(WARN, message);
	}

	public void error(String message) {
		writeLog(ERROR, message);
	}

	public void log(int level, String message) {
		writeLog(level, message);
	}

	
	
	private StringBuilder writeLog(int logLevel, String message) {
		StringBuilder sb = new StringBuilder();
		if (logLevel == INFO) {
			sb.append("INFO:    ");
		} else if (logLevel == WARN) {
			sb.append("WARNING: ");
		} else if (logLevel == ERROR) {
			sb.append("ERROR:   ");
		}
		StringBuilder sbName = new StringBuilder();
		if (type != null) {
			sbName.append(type);
			sbName.append(": ");
		}
		sbName.append(name);
		if (key != null) {
			sbName.append("(");
			sbName.append(key);
			sbName.append(")");
		}
		for(int i = sbName.length(); i < 36;i++){
			sbName.append(' ');
		}
		sb.append(sbName);
		sb.append(" --> ");
		sb.append(message);
		System.out.println(sb.toString());
		return sb;
	}
	
	

}
