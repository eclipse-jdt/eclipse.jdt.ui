public class Test {
	public void foo() {
		foo();
		/* comment */int i= 0;/*[*/
		// comment
		/** comment */
		foo();
	}
}
