package p1;

public class TT {
	public static TT createTT() {
		return new TT();
	}
	public void bletch() {
		createTT();
	}
	public void bar() {
		createTT();
	}
}
