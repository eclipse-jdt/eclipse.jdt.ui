package locals_out;

public class A_test569 {
	public void foo() {
		String[] args[]= null;
		
		args = extracted();
		
		args[0]= args[1];
	}

	protected String[][] extracted() {
		String[][] args;
		/*[*/args= new String[1][4];/*]*/
		return args;
	}
}

