package signature;

import java.io.File;

public class TestGenericTypes<A, B extends String> {
	class Inner <C extends String, D> {
	}
	
	void foo() {
		class Local<C extends File, D> {
		}
	}
}
