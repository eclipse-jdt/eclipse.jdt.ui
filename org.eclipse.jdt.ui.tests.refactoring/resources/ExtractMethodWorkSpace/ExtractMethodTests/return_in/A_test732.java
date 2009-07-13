package return_in;

import java.util.ArrayList;
import java.util.List;

public class A_test732 {
	public void foo(int x) {
		List<A_test732> list = new ArrayList<A_test732>();
		for (A_test732 t : list) {
		}
		
		int a = 0;
		for (int i = 0; i < 3; i++) {
			/*[*/g(a++);/*]*/
		}
	}

	private void g(int i) {}
}

