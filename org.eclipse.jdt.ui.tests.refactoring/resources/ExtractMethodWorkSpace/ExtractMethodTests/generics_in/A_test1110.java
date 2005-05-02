package generics_in;

import java.util.List;

// Tests capture bindings
public class A_test1110 {
	public void foo() {
		List<?> param= null;
		/*[*/consume(param);/*]*/
	}
	public void consume(List<?> param) {
		
	}
}
