package validSelection;

public class A_test252 {
	public boolean b() {
		return true;
	}
	
	public void foo() {
		while(b())
			while(b())/*[*/
				foo();
		/*]*/foo();		
	}	
}