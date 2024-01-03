package package1;

public class Example {

	public static long toRefactor(long ending) {
		long i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		long j = Example.toRefactor(Long.MAX_VALUE);
	}
}