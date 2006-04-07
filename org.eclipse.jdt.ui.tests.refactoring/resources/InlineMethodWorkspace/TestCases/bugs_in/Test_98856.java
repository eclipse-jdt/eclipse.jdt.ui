package bugs_in;

public class Test_98856 {
	class Inner {
		String field;
	}

	Inner inner;
	String string;

	static void foo() {
		Test_98856 t = new Test_98856();
		t./*]*/bar()/*[*/;
	}

	void bar() {
		inner.field = "Eclipse";
		string = "Eclipse";
	}
}
