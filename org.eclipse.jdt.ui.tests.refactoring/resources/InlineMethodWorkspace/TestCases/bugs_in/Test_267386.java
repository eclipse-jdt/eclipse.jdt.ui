package bugs_in;

public class InlineAutoboxed {
	int foo(Integer i) {
		return i.intValue();
	}

	int bar() {
		return /*]*/foo/*[*/(42);
	}
}
