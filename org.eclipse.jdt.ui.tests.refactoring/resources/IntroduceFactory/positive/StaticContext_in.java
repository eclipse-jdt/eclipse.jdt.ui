package p;

public class StaticContext_in {
	private int fN;
	public StaticContext_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public static void main(String[] args) {
		StaticContext_in	sc= /*[*/new StaticContext_in(16)/*]*/;

		System.out.println("Value = " + sc.get());
	}
}
