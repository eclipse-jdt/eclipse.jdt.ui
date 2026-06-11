package invalid;

class A1 {
	private A1() {
	}

	public static A1 /*]*/create/*[*/() { // inline this method
		return new A1();
	}
}


public class TestPrivateConstructor {
	public static void main(String[] args) {
		A1 a1 = A1.create();
	}
}
