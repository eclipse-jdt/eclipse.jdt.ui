package locals_out;

public class A_test566 {
	public void foo() {
		String args[]= null;
		extracted(args);
	}

	protected void extracted(String[] args) {
		/*[*/for (int i = 0; i < args.length; i++) {
			args[i]= "";
		}/*]*/
	}
}

