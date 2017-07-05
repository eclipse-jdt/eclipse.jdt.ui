package p;

import java.io.Closeable;
import java.io.IOException;

public class A {
	void foo() throws IOException {
		Resource resource1 = new Resource("resource1");
		try (resource1) {
			System.out.println(/*]*/resource1/*[*/.toString());
		}
	}
}

class Resource implements Closeable {
	public Resource(String string) throws IOException {}
	@Override
	public void close() throws IOException {}
}