//selection: 17, 21, 17, 40
//name: is -> ints
package simple;

/**
 * @see Javadoc1#doSomething(int[])
 * @see #doSomething(int[] is)
 * 
 * @see Javadoc1#go(float)
 * @see #go(float ship)
 */
public class Javadoc1 {
	public void run() {
		go(3.0f);
	}
	public void go(float ship) {
		doSomething(new int[] {1, 2, 3});
	}
	static void doSomething(int[] is) {
	}
}