package p;

class C {
	static int m() {
		return 7;
	}
}

public class B extends C {
	
	public void foo() {
		System.out.println(m());
	}
}