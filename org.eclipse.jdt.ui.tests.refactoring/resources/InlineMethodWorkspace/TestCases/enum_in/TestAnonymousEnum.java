package enum_in;

public enum TestAnonymousEnum {
	A {
		void foo() {
			/*]*/bar()/*[*/;
		}
		void bar() {
			System.out.println("Hello Eclipse");
		}
	};
}
