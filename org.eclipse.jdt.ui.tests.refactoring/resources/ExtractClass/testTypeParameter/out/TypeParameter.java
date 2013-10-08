package p;
class TypeParameter<T>{
	public static class FooParameter<T> {
		public T param;
		public FooParameter() {
		}
	}

	FooParameter data = new FooParameter();
}