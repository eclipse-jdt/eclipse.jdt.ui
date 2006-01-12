package p;

public class Foo {
	
	// Test correct "thisification".

	class X extends Bar {
		
		{
			getDisplay();
		}
	}
}
