package p;
interface TestMethodTypeParamArgs<T> {
    public static class FooParameter<T> {
		public T t;
		public FooParameter(T t) {
			this.t = t;
		}
	}

	void foo(FooParameter parameterObject);
}