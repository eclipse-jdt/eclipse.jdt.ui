package p;

public class Foo {
	
	// Test error, method already exists
	
	void foo(p.Bar bar, String foo)
	
	{
		new Bar().foo(null); // <- invoke here with same name and target type Foo
	}

}
