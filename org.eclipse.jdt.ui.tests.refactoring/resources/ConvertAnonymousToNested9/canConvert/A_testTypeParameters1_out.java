package p;
//private, final
class A {
	private final class CallableImplementation implements Callable<String> {
		@Override
		public String call() throws Exception {
			return "abc"; //$NON-NLS-1$
		}
	}

	public interface Callable<T> {
		T call() throws Exception;
	}

	Callable<String> c = new CallableImplementation();
}