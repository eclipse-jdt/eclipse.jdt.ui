package p;

public class DifferentArgs3_in {
	private int fN;
	public DifferentArgs3_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		int				size;
		DifferentArgs3_in	da= /*[*/new DifferentArgs3_in(size=16)/*]*/;

		System.out.println("Value = " + da.get());
	}
	public void bar(String[] args) {
		DifferentArgs3_in	da= new DifferentArgs3_in(24);

		System.out.println("Value = " + da.get());
	}
}
