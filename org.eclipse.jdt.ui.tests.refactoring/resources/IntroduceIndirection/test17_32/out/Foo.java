package p;

public class Foo<T> {
	
	/**
	 * @param foo
	 */
	public static <T> void foo(Foo<T> foo) {
		foo.foo();
	}

	// Test qualification with outer type
	void foo() {
		
		Bar bar= new Bar() {
			{
				Foo.foo(Foo.this); // <--- invoke here
			}
		};
	}
	
}

class Bar {
	
}
