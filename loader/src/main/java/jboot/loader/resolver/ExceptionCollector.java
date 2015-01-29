package jboot.loader.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionCollector {
	private static Logger resolverLog = Logger.getLogger("jboot.resolver");
	private List<Throwable> exceptions;

	public ExceptionCollector() {
		this.exceptions = new ArrayList<Throwable>();
	}

	public void add(Throwable throwable) {
		this.exceptions.add(throwable);
	}

	public List<Throwable> getExceptions() {
		return this.exceptions;
	}

	public void merge(ExceptionCollector collector) {
		this.exceptions.addAll(collector.getExceptions());
	}

	public void log() {
		for (Throwable throwable : exceptions) {
			resolverLog.log(Level.INFO, "", throwable);
		}
	}
}
