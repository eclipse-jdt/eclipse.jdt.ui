package duplicates_in;

// don't extract second occurence of
// 2 since it is in a inner class
public class A_test962 {
	private Object object;
	public A_test962() {
	  this.object = new Object();
	  System.out.println(/*[*/this.object/*]*/);
	}
}
