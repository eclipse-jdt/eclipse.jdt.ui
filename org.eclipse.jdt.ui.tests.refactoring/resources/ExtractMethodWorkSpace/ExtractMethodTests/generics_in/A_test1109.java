package generics_in;

import java.util.List;

// Tests capture bindings
public class A_test1109 {
	public void foo() {
		consume(/*[*/produce()/*]*/);
	}
	public List<?> produce() {
		return null;
	}
	public void consume(List<?> param) {
		
	}
}
