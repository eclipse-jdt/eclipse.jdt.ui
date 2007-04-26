package p;

import java.security.Permission;

public class TestImportAddEnclosingCaller {
	public void foo(){
		new TestImportAddEnclosing().foo(new TestImportAddEnclosing.FooParameter(new Permission[0], 7));
	}
}
