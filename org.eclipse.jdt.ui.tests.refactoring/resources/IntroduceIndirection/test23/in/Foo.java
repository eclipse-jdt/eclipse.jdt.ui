package p;

public class Foo {
	
	Bar.FooBar bb;
	
	static class Bar {
		
		static class FooBar {
			
			static void foo() {		// <--- invoke here
				
			}
		}
	}
	
	Bar.FooBar getFooBar() {
		return null;
	}
	
	{
		Bar.FooBar b= new Bar.FooBar();
		
		// not ok:
		b.foo();
		getFooBar().foo();
		this.bb.foo();
		bb.foo();
		
		// ok:
		Foo.Bar.FooBar.foo();
		p.Foo.Bar.FooBar.foo();
		Bar.FooBar.foo();
	}

}
