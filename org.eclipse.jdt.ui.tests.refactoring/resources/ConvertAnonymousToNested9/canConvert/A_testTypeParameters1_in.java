package p;
//private, final
class A {
	public interface Callable<T> {
		T call() throws Exception;
	}

	Callable<String> c = new Callable<>() {
		@Override
		public String call() throws Exception {
			return "abc"; //$NON-NLS-1$
		}
	};
}