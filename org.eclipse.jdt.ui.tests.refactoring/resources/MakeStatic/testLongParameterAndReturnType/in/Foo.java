package package1;

public class Example {

	public long toRefactor(long ending) {
		long i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		long j = instance.toRefactor(Long.MAX_VALUE);
	}
}