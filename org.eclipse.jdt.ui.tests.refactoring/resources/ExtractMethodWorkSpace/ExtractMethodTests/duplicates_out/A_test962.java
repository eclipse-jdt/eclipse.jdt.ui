package duplicates_out;

public class A_test962 {
	private Object object;
	public A_test962() {
	  this.object = new Object();
	  System.out.println(extracted());
	}
	protected Object extracted() {
		return /*[*/this.object/*]*/;
	}
}
