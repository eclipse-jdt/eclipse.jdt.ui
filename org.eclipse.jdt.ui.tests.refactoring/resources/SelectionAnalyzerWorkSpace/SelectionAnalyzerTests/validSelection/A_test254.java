package validSelection;

public class A_test254 {
	public boolean b() {
		return true;
	}
	
	public void foo() {
		while(b())
			foo();
			
		/*]*/foo();/*[*/		
	}	
}