//selection: 17, 21, 17, 40
//name: is -> ints
package simple;

/**
 * @see Javadoc1#doSomething(int[])
 * @see #doSomething(int[] is)
 * 
 * @see Javadoc1#go(float, int[])
 * @see #go(float ship, int[] ints)
 */
public class Javadoc1 {
	public void run() {
		go(3.0f, new int[] {1, 2, 3});
	}
	public void go(float ship, int[] ints) {
		doSomething(ints);
	}
	static void doSomething(int[] is) {
	}
}