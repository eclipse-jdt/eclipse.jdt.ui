package p;

public class Foo<T> {
	
	// Test qualification with outer type
	void foo() {
		
		Bar bar= new Bar() {
			{
				foo(); // <--- invoke here
			}
		};
	}
	
}

class Bar {
	
}
