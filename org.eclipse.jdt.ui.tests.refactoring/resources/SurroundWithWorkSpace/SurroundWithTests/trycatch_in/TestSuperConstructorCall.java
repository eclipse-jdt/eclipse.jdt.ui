package trycatch_in;

import java.net.MalformedURLException;

public class TestSuperConstructorCall {
	static class A {
		public A(int i) throws MalformedURLException {
		}
	}
	
	static class B extends A {
		public B() {
			super(10);
		}
	}
}
