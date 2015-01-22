package jboot.repository.client.info.model;

import java.io.PrintStream;
import java.io.PrintWriter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class RepositoryException {
	@XmlElement
	private String message;
	@XmlElement
	private String stackTrace;

	public RepositoryException() {
	}

	public RepositoryException(String message, String stackTrace) {
		this.message = message;
		this.stackTrace = stackTrace;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	@SuppressWarnings("serial")
	public void throwException() throws Exception {
		throw new Exception(message != null ? message : "unknown_message") {
			@Override
			public void printStackTrace(PrintStream s) {
				s.println(stackTrace != null ? stackTrace : "unknown_stack_trace");
			}

			@Override
			public void printStackTrace(PrintWriter p) {
				p.println(stackTrace != null ? stackTrace : "unknown_stack_trace");
			}
		};
	}
}
