//selection: 13, 19, 13, 31
//name: object -> arg
package simple;

import java.util.List;

public class Wildcard2 {
	private List<? super Integer> field= null;
	private void use() {
		foo(field.get(0));
	}
	private void foo(Object arg) {
		Object n= arg;
	}
}
