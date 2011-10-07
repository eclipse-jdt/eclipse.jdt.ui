package destination_in;

import java.util.concurrent.Callable;

public class A_test1055 {
	public class Inner {
		public void extracted() {
			
		}
		public int foo() {
			return new Callable<Integer>() {
				public Integer call() {
					return /*[*/2 + 3/*]*/;
				}
			}.call();
		}
	}
}