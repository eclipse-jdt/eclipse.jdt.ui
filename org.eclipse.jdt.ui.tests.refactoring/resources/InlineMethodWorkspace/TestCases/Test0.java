public class Test0 {
	private static class Inner extends Test0 {
		String getMessage() { 
			return "overridden";
		}
	}		
	String getMessage() {
		return "original";
	}
	void printMessage() {
		System.out.println(getMessage());
	}
	public static void main(String[] args) {
		new Inner().printMessage();
	}
}