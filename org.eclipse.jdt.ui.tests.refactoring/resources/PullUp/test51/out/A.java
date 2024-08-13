package pkg1;

import java.util.List;

public class A implements B.Foo {

	@Override
	public void b() {
		List<Object> l = null;
	}
}
