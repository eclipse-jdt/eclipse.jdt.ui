package classes;

public class Target {
	/**
	 * @param message the string to log
	 * @return the length of the logged message
	 * @deprecated inline implementation
	 */
	public int logMessage(String message) {
		System.out.println(message);
		return message.length();
	}
}
