package p;

public class MultiUnit1A_in {
	public static MultiUnit1A_in createMultiUnit1A_in(int N) {
		return new MultiUnit1A_in(N);
	}
	private int fN;
	private MultiUnit1A_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo1(String[] args) {
		MultiUnit1A_in	mu= createMultiUnit1A_in(16);

		System.out.println("Value = " + mu.get());
	}
	public void foo2(String[] args) {
		MultiUnit1A_in	mu= createMultiUnit1A_in(24);

		System.out.println("Value = " + mu.get());
	}
}
