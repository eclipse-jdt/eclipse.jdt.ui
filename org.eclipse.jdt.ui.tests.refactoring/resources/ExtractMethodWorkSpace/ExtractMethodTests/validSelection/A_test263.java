package validSelection;

public class A_test263 {
	public boolean fBoolean;
	public void foo() {
		/*]*/foo()/*[*/;
		
		do 
			foo();
		while(fBoolean);		
	}
}