package p;

public class Foo {
	
	// Test name clash with existing arguments
	
	void foo(String target, String target1) {
		
	}
	
	{
		foo(null, null); // <- invoke here
	}

}
