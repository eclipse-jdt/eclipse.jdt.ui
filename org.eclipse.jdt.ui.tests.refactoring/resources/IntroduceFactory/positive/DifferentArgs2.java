package p;

public class DifferentArgs2_in {
	public static DifferentArgs2_in createDifferentArgs2_in(int N) {
		return new DifferentArgs2_in(N);
	}
	private int fN;
	public DifferentArgs2_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		DifferentArgs2_in	da= createDifferentArgs2_in(16);

		System.out.println("Value = " + da.get());
	}
	public void bar(String[] args) {
		int					size= 24;
		DifferentArgs2_in	da= createDifferentArgs2_in(size);

		System.out.println("Value = " + da.get());
	}
}
