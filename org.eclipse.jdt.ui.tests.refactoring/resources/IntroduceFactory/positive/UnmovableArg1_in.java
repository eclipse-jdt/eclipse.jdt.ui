package p;

public class UnmovableArg1_in {
	private int fN;
	public UnmovableArg1_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		int				size;
		UnmovableArg1_in	ua= /*[*/new UnmovableArg1_in(size=16)/*]*/;

		System.out.println("Value = " + ua.get());
	}
}
