package invalid;

class A1 {
	private enum Day {
		MONDAY
	}

	int /*]*/getVal/*[*/() { // inline this method
		return Day.MONDAY.ordinal();
	}
}


public class TestPrivateEnum {
	public static void main(String[] args) {
		System.out.println(new A1().getVal()); // inline this call
	}
}
