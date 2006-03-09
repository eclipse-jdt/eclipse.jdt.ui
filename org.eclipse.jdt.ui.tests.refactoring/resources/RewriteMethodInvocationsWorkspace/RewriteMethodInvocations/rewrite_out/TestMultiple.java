package rewrite_out;
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
		tm.getException().printStackTrace();
	}
	
	void user() {
		getException().printStackTrace();
		getException().printStackTrace();
	}
}
