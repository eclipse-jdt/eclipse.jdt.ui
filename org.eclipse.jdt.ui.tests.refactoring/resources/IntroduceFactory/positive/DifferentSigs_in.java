package p;

public class DifferentSigs_in {
	private int fN;
	public DifferentSigs_in() {
		this(10);
	}
	public DifferentSigs_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		DifferentSigs_in	ds= /*[*/new DifferentSigs_in(16)/*]*/;

		System.out.println("Value = " + ds.get());
	}
	public void bar(String[] args) {
		DifferentSigs_in	ds= new DifferentSigs_in();

		System.out.println("Value = " + ds.get());
	}
}
