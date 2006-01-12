package p;

public class Foo extends Bar {
	
	// Test correct "thisification".

	void foo() {
		
		X x= new X() {
			
			{
				getDisplay(); // <- invoke here
			}
		};
	}
}

class X {
	
}
