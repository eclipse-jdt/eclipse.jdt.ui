package receiver_out;

public class TestQualifiedAnonymousReceiver {
	int x = 10;

	void superClassMethod() {
		System.out.println(x);
	}

	static void callingMethod(TestQualifiedAnonymousReceiver instance) {
		/*]*/int k = 0;
		new Object() {
			void execute() {
				instance.superClassMethod();
			}
		}.execute();/*[*/
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
