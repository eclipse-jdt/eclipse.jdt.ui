package invalidSelection;

public class A_test091 {
	public void foo() {
		for (int i= 0; i < 10; i++)
			/*]*/continue/*[*/;
	}	
}