package invalidSelection;

public class A_test072 {
	public int foo() {
		int i= foo();
		switch (i) {
			case 1/*]*/:
				foo()/*[*/;
			case 2:
				foo();
				foo();
			case 3:
				foo();
			default:
				foo();	
		}
		return i;
	}
}