package trycatch_in;

import java.net.MalformedURLException;

public class TestThisConstructorCall {
	public TestThisConstructorCall(int i) throws MalformedURLException {
	}
	public TestThisConstructorCall() {
		this(10);
	}
}
