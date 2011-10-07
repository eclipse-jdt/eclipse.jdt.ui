package cast_in;

public class TestReturnValue4 {	
	Integer foo() {
		return 1 + 1;
	}

	void x() {
		int a= /*]*/foo()/*[*/.intValue();
	}
}
