package simple_out;

public class TestCatchClause {
	public int foo() {
		int i= 0;
		switch(i) {
			case 10:
				return 10;
			case 20:
				return bar();
		}
		return 0;
	}
	int bar() {
		return 10;
	}
}
