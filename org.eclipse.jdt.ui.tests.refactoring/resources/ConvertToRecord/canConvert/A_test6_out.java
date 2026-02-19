package p;
// Class A
public record A(int a, String b) {
	static {
		c= 4;
	}
	private static int c;
	public static int getC() {
		return c;
	}
}