package p;

public class Names_in {
	private int fN;
	public Names_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		Names_in	n= /*[*/new Names_in(16)/*]*/;

		System.out.println("Value = " + n.get());
	}
}
