package p;

public class StaticInstance_in {
	private int fN;
	public StaticInstance_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		StaticInstance_in	si= /*[*/new StaticInstance_in(16)/*]*/;

		System.out.println("Value = " + si.get());
	}
	public static void bar(String[] args) {
		StaticInstance_in	si= new StaticInstance_in(16);

		System.out.println("Value = " + si.get());
	}
}
