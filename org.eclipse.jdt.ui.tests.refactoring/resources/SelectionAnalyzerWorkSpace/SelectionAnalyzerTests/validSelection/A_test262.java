package validSelection;

public class A_test262 {
	public boolean fBoolean;
	public void foo() {
		do 
			foo();
		while(fBoolean);
		
		/*]*/foo()/*[*/;
	}
}