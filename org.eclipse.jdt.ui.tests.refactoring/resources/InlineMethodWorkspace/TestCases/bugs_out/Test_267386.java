package bugs_out;

public class InlineAutoboxed {
	int foo(Integer i) {
		return i.intValue();
	}

	int bar() {
		return ((Integer) 42).intValue();
	}
}
