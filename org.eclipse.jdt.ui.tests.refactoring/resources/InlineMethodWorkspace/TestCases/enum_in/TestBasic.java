package enum_in;

public enum TestBasic {
	A, B;
	
	void foo() {
		/*]*/bar()/*[*/;
	}
	void bar() {
		System.out.println("Hello Eclipse");
	}
}
