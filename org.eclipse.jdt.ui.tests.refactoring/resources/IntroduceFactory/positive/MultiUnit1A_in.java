package p;

public class MultiUnit1A_in {
	private int fN;
	public MultiUnit1A_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo1(String[] args) {
		MultiUnit1A_in	mu= /*[*/new MultiUnit1A_in(16)/*]*/;

		System.out.println("Value = " + mu.get());
	}
	public void foo2(String[] args) {
		MultiUnit1A_in	mu= new MultiUnit1A_in(24);

		System.out.println("Value = " + mu.get());
	}
}
