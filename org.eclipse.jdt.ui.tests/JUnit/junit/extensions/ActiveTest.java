package junit.extensions;

import junit.framework.*;

/**
 * A Decorator that runs a test in a separate thread.
 *
 */
public class ActiveTest extends TestDecorator {

	public ActiveTest(Test test) {
		super(test);
	}
	public void run(TestResult result) {
		final TestResult finalResult= result;
		Thread t= new Thread() {
			public void run() {
				fTest.run(finalResult);
			}
		};
		t.start();
	}
	public String toString() {
		return super.toString()+"(active)";
	}
}