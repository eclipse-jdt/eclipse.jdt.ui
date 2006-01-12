package p;

public class Foo {
	
	class Inner {
		
	}
	
	void foo() {	// <- create an intermediary in Inner. fails because inner is not static.
		
	}

}
