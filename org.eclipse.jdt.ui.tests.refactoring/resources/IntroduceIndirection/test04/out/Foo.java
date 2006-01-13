package p;

public class Foo {
	
	/* (non-Javadoc)
	 * @see p.Foo#foo()
	 */
	public static void bar(Foo foo) {
		foo.foo();
	}

	// Test qualification with outer type
	void foo() {
		
		Bar bar= new Bar() {
			{
				Foo.bar(Foo.this); // <--- invoke here
			}
		};
	}
	
}

class Bar {
	
}
