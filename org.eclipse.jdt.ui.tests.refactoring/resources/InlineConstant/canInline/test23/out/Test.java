//6, 26 -> 6, 26  replaceAll == false, removeDeclaration == false
package p;

class Test {
	Runnable getExecutor() {
		return new Runnable() {
			public void run() { }
		};
	}
}

class Runnables {
	public static final Runnable DO_NOTHING= new Runnable() {
		public void run() { }
	};
}
