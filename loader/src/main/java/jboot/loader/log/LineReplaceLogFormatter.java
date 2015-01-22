package jboot.loader.boot.log;

import java.util.logging.LogRecord;

public class LineReplaceLogFormatter extends LogFormatter {

	private String lastFormatted = "";

	@Override
	public synchronized String format(LogRecord record) {
		StringBuffer sb = new StringBuffer();
		String strRecord = super.format(record);
		sb.append(strRecord.substring(0, strRecord.length()-strLINE_SEPARATOR.length()));
		if (sb.length() < lastFormatted.length()) {
			for (int i=0;i<lastFormatted.length()-sb.length();i++) {
				sb.append(' ');
			}
		}
		sb.append("\r");
		lastFormatted = sb.toString();
		return lastFormatted;
	}
}
