package bugs_in;

public class Test_94426 {
	/**
	 * @see Other#foo()
	 */
	public void toInline() {
		System.out.println("foo");
	}
	
	void m() {
		toInline();
	}
}
