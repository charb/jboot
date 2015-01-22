package jboot.loader.boot.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

	protected final static String strDATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SSS";
	protected final static String strLINE_SEPARATOR = System.getProperty("line.separator");

	private Date date = new Date();
	private SimpleDateFormat simpleDateFormat;

	@Override
	public synchronized String format(LogRecord record) {
		StringBuffer sb = new StringBuffer();
		date.setTime(record.getMillis());
		if (simpleDateFormat == null) {
			simpleDateFormat = new SimpleDateFormat(strDATE_FORMAT_STRING);
		}
		sb.append(simpleDateFormat.format(date));
		sb.append(" ");
		sb.append(record.getLevel().getLocalizedName());
		sb.append(" ");
		sb.append(record.getLoggerName());
		sb.append(" ");
		String message = formatMessage(record);
		sb.append(message);
		sb.append(strLINE_SEPARATOR);
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
			}
		}
		return sb.toString();
	}
}
