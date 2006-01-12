package p0;

public class Foo {
	
	// test visibility adjustment of intermediary type
	// because of existing references
	
	public void bar() { // <- create im in Bar.
		
	}
	
	{
		Bar.bar(this); 
	}

}
