package p;

public class A {
	int fMagic;

	public static void main(String[] args) {
		Second s= new Second("Bingo");
		A a= new A();
		a.fMagic= 17;
		a.print(s);
	}

	/**
	 * Print
	 * @param s
	 */
	public void print(Second s) {
		System.out.println(s + ": " + fMagic);
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
}
