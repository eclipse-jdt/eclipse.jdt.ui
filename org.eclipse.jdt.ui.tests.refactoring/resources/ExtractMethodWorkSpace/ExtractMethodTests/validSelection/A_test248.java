package validSelection;

public class A_test248 {
	public void foo() {
		for (int i= 10; i < 10; i++)
			foo();
			
		for (int z= 10; z < 10; z++)
			foo();
		
		foo();
		
		int i= 0;
		while (i < 10)
			foo();
			
		while (i < 10)
			foo();
			
		foo();
		
		do 
			foo();
		while (i < 10);	
		
		do 
			foo();
		while (i < 10);		
	
		foo();
		
		switch (1) {
			case 0:
				foo();
				break;
			case 1:
				foo();
				break;
			default:
				foo();		
		}
		
		switch (1) {
			case 0:
				foo();
				break;
			case 1:
				foo();
				break;
			default:
				foo();		
		}

		foo();
		
	}
}