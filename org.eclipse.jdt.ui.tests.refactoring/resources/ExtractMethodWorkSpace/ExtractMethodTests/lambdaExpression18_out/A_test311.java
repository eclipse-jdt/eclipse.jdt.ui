package lambdaExpression18_in;

@FunctionalInterface
public interface J {
	void foo();
}

class X1 {
	J j1= () -> {
		extracted(); 
	};

	private void extracted() {
		/*[*/ return; /*]*/
	}
}