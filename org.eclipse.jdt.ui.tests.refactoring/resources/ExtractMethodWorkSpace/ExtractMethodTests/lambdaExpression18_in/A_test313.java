package lambdaExpression18_in;

@FunctionalInterface
public interface J {
	void foo();
}

class X1 {
	String m1() {
		/*[*/J j2= () -> {
			return;
		};/*]*/
		return "";	
	}
}