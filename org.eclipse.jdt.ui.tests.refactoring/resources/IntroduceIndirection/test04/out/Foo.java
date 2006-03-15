package p;

public class Foo {
	
	/**
	 * @param foo
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
