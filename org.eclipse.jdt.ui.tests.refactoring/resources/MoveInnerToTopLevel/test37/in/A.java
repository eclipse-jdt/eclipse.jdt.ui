package p;

public class A {
	
	private static final class SomeInner {
		private SomeInner() {}
		private SomeInner(String unused) {}
		private void usedMethod() {}
		private void unusedMethod() {}
	}

	void usage() {
		SomeInner c= new SomeInner();
		c.usedMethod();
	}
}