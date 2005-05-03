package argument_in;

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
		/*]*/varargs(args);/*[*/
	}	
}
