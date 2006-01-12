package p;

public class Foo extends Bar {
	
	void foo() {
		
	}
	
	void myFoo() throws Exception {
		foo();				// <-- invoke here
		new Bar().foo();
	}

}
