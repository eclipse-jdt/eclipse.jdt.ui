package simple_in;

public class TestCatchClause {
	public int foo() {
		int i= 0;
		switch(i) {
			case 10:
				return /*]*/bar()/*[*/;
			case 20:
				return bar();
		}
		return 0;
	}
	int bar() {
		return 10;
	}
}
