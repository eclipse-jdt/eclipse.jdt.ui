package p;

public class Names_in {
	public static Names_in createThing(int N) {
		return new Names_in(N);
	}
	private int fN;
	private Names_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		Names_in	n= createThing(16);

		System.out.println("Value = " + n.get());
	}
}
