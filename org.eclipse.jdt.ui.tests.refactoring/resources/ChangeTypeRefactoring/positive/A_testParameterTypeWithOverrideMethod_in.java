class A_testParameterTypeWithOverrideMethod_in {
	static class E {
		public void bar(String s1, Float g, String s) {
			g.foo();
		}
	}

	static class F extends E {
		@Override
		public void bar(String s1, Float g, String s) {
		}

		public void bar(String s1, Object i, String s) {
		}
	}
}
