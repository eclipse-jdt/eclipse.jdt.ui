package p;

public class Foo {
	
	static class Inner {
		
		static class MoreInner {
			
		}
		
	}
	
	void foo() {	// <- create an intermediary in Inner. 
		
	}
	
	{
		foo();
	}

}
