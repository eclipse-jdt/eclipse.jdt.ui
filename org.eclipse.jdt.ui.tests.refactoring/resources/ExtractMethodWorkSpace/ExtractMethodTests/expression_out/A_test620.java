package expression_out;

import java.util.List;

public class A_test620 {
	public void foo() {
		B b= new B();
		Object o= extracted(b);
	}

	protected List[] extracted(B b) {
		return /*[*/b.foo()/*]*/;
	}
}
