package enums_out;

public enum A_test1152 {
	A {
		public void foo() {
			extracted();
		}

		protected void extracted() {
			/*[*/foo();/*]*/
		}
	};
}
