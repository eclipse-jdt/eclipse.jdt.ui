package generics_out;

import java.util.List;

// Tests capture bindings
public class A_test1110 {
	public void foo() {
		List<?> param= null;
		extracted(param);
	}
	protected void extracted(List<?> param) {
		/*[*/consume(param);/*]*/
	}
	public void consume(List<?> param) {
		
	}
}
