package util.exceptions;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/*public class NoDataFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoDataFoundException() {
		
	}

	public NoDataFoundException(String message) {
		super(fineTuneMessage(message));
	}
	
	private static String fineTuneMessage(String message) {
		if (message == null || message.isBlank()) return "Resource not found.";
		String[] parts = message.split(":", 2);
		return parts.length > 1 ? parts[1].trim() : message.trim();
	}
}*/

@ResponseStatus(HttpStatus.NOT_FOUND) // opcija A: direktno mapiranje
public class NoDataFoundException extends RuntimeException {
    public NoDataFoundException(String message) {
        super(message);
    }
}
