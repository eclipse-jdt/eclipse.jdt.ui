//selection: 10, 21, 11, 40
//name: is -> ints
package simple;

public class Formatting1 {
	public void method1() {
		method2(new int[]{1, 2, //newline
						3/*important comment*/});
	}
	public void method2(int[] ints) {
		doSomething(ints);
	}
	private void doSomething(int[] is) {
	}
}