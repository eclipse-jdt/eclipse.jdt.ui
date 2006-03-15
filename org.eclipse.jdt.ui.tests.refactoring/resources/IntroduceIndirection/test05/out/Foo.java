package p;

public class Foo extends Bar {
	
	// Test correct "thisification".

	/**
	 * @param bar
	 */
	public static void bar(Bar bar) {
		bar.getDisplay();
	}

	void foo() {
		
		X x= new X() {
			
			{
				Foo.bar(Foo.this); // <- invoke here
			}
		};
	}
}

class X {
	
}
