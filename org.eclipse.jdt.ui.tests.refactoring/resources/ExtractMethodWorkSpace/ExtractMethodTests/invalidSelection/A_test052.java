package invalidSelection;

public class A_test052 {
	public boolean b() {
		return true;
	}
	
	public void foo() {
		while(b())/*[*/
			while(b())
				foo();
		foo()/*[*/;		
	}	
}