//selection: 10, 21, 11, 40
//name: is -> ints
package simple;

public class Formatting1 {
	public void method1() {
		method2();
	}
	public void method2() {
		doSomething(new int[]{1, 2, //newline
				3/*important comment*/});
	}
	private void doSomething(int[] is) {
	}
}