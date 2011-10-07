package bugs_in;

public class InlineAutoboxed_2 {
	/*]*/int foo(Integer i)/*[*/{
		return i.intValue();
	}

	int bar() {
		return foo(42);
	}
}
