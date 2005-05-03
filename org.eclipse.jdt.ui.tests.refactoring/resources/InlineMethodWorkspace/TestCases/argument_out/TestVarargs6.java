package argument_out;

import java.io.Serializable;

public class TestVarargs6 {

	private static class Warnings {};
	
	public void varargs(Serializable... args) {
		for (Serializable arg : args) {
			System.out.println(arg);
		}
	}
	
	public void singleArgumentTransfer() {
		Warnings[] args= { new Warnings(), new Warnings() };
		Serializable[] args1 = {args};
		/*]*/for (Serializable arg : args1) {
			System.out.println(arg);
		}/*[*/
	}	
}
