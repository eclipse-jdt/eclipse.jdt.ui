package bugs_out;

public class Test_98856 {
	class Inner {
		String field;
	}

	Inner inner;
	String string;

	static void foo() {
		Test_98856 t = new Test_98856();
		t.inner.field = "Eclipse";
		t.string = "Eclipse";
	}

	void bar() {
		inner.field = "Eclipse";
		string = "Eclipse";
	}
}
