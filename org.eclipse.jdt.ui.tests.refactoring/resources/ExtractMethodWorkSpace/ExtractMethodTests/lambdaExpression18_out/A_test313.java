package lambdaExpression18_in;

@FunctionalInterface
public interface J {
	void foo();
}

class X1 {
	String m1() {
		extracted();
		return "";	
	}

	private void extracted() {
		/*[*/J j2= () -> {
			return;
		};/*]*/
	}
}