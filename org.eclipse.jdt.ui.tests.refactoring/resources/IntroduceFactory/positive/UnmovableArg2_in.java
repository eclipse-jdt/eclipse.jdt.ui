package p;

public class UnmovableArg2_in {
	private int fN;
	public UnmovableArg2_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		int				size=16;
		UnmovableArg2_in	ua= /*[*/new UnmovableArg2_in(size)/*]*/;

		System.out.println("Value = " + ua.get());
	}
}
