public class C {
	public boolean flag;
	public void foo() {
		while (flag)    
			foo();
			
		while (flag) {
			foo();
		}
		
		for (;flag;)
			foo();
			
		for (;flag;) {
			foo();
		}
		
		final int i= 10;
		
		int x= i;
		
		do {
			foo();
		} while (flag);
		
		try {
			foo();
		} catch (Exception e) {
		}
	}
}

