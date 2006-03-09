package rewrite_in;
/* params: 
getException().printStackTrace();
 */

public class TestMultiple {
	/**
	 * @deprecated use getException().printStackTrace()
	 */
	void /*]*/m/*[*/() {
		System.out.println("No trace");
	}
	
	Exception getException() {
		return new Exception();
	}
	
	public static void main(String[] args) {
		TestMultiple tm= new TestMultiple();
		tm.m();
	}
	
	void user() {
		m();
		m();
	}
}
