package p;

public class DifferentArgs1_in {
	private int fN;
	public DifferentArgs1_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		DifferentArgs1_in	da= /*[*/new DifferentArgs1_in(16)/*]*/;

		System.out.println("Value = " + da.get());
	}
	public void bar(String[] args) {
		DifferentArgs1_in	da= new DifferentArgs1_in(24);

		System.out.println("Value = " + da.get());
	}
}
