package cast_in;

public class TestReturnValue5 {	
	Integer foo() {
		return 1 + 1;
	}

	void x() {
		Integer a= /*]*/foo()/*[*/;
	}
}
