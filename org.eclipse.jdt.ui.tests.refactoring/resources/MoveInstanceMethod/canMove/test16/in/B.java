package p;

public class B {
	private int count;

	public void f() {
		Inner s= new Inner();
		print(s);
	}

	/**
	 * Bla bla
	 * @param s
	 */
	public void print(Inner s) {
		count++;
		System.out.println(s);
	}	

	public class Inner {
	}
}
