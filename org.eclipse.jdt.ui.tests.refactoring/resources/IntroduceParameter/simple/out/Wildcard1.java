//selection: 13, 19, 13, 31
//name: number -> number
package simple;

import java.util.List;

public class Wildcard1 {
	private List<? extends Number> field= null;
	private void use() {
		foo(field.get(0));
	}
	private void foo(Number number) {
		Number n= number;
	}
}
