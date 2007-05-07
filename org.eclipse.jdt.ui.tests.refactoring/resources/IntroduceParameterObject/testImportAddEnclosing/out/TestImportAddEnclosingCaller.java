package p;

import java.security.Permission;

import p.TestImportAddEnclosing.FooParameter;

public class TestImportAddEnclosingCaller {
	public void foo(){
		new TestImportAddEnclosing().foo(new FooParameter(new Permission[0], 7));
	}
}
