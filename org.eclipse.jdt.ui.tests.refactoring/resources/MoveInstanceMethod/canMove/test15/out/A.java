package p;

public class A {
	int fMagic;

	public static void main(String[] args) {
		Second s= new Second("Bingo");
		A a= new A();
		a.fMagic= 17;
		a.print(s);
	}

	public void print(Second s) {
		s.print(this);
	}
}

class Second {
	String fName;
	public Second(String name) {
		fName= name;
	}
	public String toString() {
		return fName;
	}
	/**
	 * @param a
	 */
	public void print(A a) {
		System.out.println(this + ": " + a.fMagic);
	}
}
