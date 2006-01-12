package p;

public class SeaLevel implements Eiger, Moench {

	// Test fatal error open hierarchy
	{
		Eiger a= new SeaLevel();
		Moench b= new SeaLevel();
		SeaLevel c= new SeaLevel();
		
		a.foo();
		b.foo();
		c.foo(); //<- invoke 
	}

	public void foo() {
		
	}
}
