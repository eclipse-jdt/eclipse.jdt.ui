package invalidSelection;

public class A_test042 {
	public void foo() {
		for (int i= 0; i < 10; /*]*/i++/*[*/)
			foo();
	}	
}