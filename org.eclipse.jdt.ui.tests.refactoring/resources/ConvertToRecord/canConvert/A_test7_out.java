package p;
interface Blah {
	void printSomething();
}
/**
 *  Class A
 */
public record A(int a, String b) implements Blah {
	static {
		c= 4;
	}
	private static int c;
	public static int getC() {
		return c;
	}
	@Override
	public void printSomething() {
		System.out.println("here");
	}
}