package validSelection;

public class A_test255 {
	public boolean b() {
		return true;
	}
	
	public void foo() {
		/*]*/foo();/*[*/
		
		while(b())
			foo();	
	}	
}