package try9_out;

import java.io.Closeable;
import java.io.IOException;

public class A_test201 {
	void foo() throws IOException {
		Resource resource1 = new Resource("resource1");
		try (resource1) {
			System.out.println(/*]*/extracted(resource1)/*[*/.toString());
		}
	}

	protected Resource extracted(Resource resource1) {
		return resource1;
	}
}

class Resource implements Closeable {
	public Resource(String string) throws IOException {}
	@Override
	public void close() throws IOException {}
}