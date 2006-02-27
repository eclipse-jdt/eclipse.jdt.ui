package p;
public class A {
    private static final class Greeter implements Runnable {
		public void run() {
		    System.out.println("Hello World")
		}
	}

	void m() {
        Runnable greeter= new Greeter();
        greeter.run();
    
}