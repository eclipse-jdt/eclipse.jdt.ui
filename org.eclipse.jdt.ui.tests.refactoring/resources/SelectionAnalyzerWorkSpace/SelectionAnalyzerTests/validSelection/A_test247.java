package validSelection;

public class A_test247 {
	public void foo() {
		for (int i= 10; i < 10; i++)/*[*/
			for (int z= 10; z < 10; z++)
				foo();
		/*]*/foo();	
	}
}