package cast_in;

public class TestNoCast {
	String fName;
	String getName() {
		return fName;
	}
	void foo(TestNoCast o) {
		System.out.println(/*]*/o.getName()/*[*/);
	}
}
