package cast_in;

public class TestReturnValue3 {	
	Integer foo() {
		return 1;
	}

	void x() {
		int a= /*]*/foo()/*[*/.intValue();
	}
}
