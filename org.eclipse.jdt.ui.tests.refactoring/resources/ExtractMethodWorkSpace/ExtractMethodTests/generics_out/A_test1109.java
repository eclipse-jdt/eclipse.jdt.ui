package generics_out;

import java.util.List;

// Tests capture bindings
public class A_test1109 {
	public void foo() {
		consume(extracted());
	}
	protected List<?> extracted() {
		return /*[*/produce()/*]*/;
	}
	public List<?> produce() {
		return null;
	}
	public void consume(List<?> param) {
		
	}
}
