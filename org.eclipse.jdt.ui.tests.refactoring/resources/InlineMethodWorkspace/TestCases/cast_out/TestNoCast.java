package cast_out;

public class TestNoCast {
	String fName;
	String getName() {
		return fName;
	}
	void foo(TestNoCast o) {
		System.out.println(o.fName);
	}
}
