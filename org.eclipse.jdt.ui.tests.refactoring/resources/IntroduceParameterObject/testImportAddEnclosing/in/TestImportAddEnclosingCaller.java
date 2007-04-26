package p;

import java.security.Permission;

public class TestImportAddEnclosingCaller {
	public void foo(){
		new TestImportAddEnclosing().foo(new Permission[0], 7);
	}
}
