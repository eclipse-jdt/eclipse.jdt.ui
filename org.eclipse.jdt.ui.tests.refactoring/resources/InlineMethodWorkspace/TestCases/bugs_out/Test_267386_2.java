package bugs_out;

public class InlineAutoboxed_2 {
	int bar() {
		return ((Integer) 42).intValue();
	}
}
