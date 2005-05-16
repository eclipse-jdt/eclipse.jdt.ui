//selection: 13, 19, 13, 31
//name: object -> arg
package simple;

import java.util.List;

public class Wildcard2 {
	private List<? super Integer> field= null;
	private void use() {
		foo();
	}
	private void foo() {
		Object n= field.get(0);
	}
}
