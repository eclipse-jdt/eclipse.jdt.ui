package receiver_in;

public class TestImplicitReceiverField {
	String field;
	public void foo() {
		/*]*/bar();/*[*/
	}
	public void bar() {
		field= "Eclipse";
	}
}
