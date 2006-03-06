package classes;

public class Target2 {
	/**
	 * @deprecated inline implementation
	 */
	public static int logMessage(String message) {
		return new Target().logMessage(message);
	}
}
