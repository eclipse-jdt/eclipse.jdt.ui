package receiver_in;

public class TestQualifiedAnonymousReceiver {
	int x = 10;

	void superClassMethod() {
		System.out.println(x);
	}

	static void callingMethod(TestQualifiedAnonymousReceiver instance) {
		/*]*/instance.methodToBeInlined();/*[*/
	}

	void methodToBeInlined() {
		int k = 0;
		new Object() {
			void execute() {
				TestQualifiedAnonymousReceiver.this.superClassMethod();
			}
		}.execute();
	}
}
