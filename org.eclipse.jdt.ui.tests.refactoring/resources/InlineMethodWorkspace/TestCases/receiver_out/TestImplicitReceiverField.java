package receiver_out;

public class TestImplicitReceiverField {
	String field;
	public void foo() {
		/*]*/field= "Eclipse";/*[*/
	}
	public void bar() {
		field= "Eclipse";
	}
}
