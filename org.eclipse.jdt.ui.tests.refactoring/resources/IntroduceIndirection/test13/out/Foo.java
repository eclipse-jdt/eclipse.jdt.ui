package p;

import p.Foo.Inner.MoreInner;

public class Foo {
	
	static class Inner {
		
		static class MoreInner {

			/**
			 * @param foo
			 */
			public static void bar(Foo foo) {
				foo.foo();
			}
			
		}
		
	}
	
	void foo() {	// <- create an intermediary in Inner. 
		
	}
	
	{
		MoreInner.bar(this);
	}

}
