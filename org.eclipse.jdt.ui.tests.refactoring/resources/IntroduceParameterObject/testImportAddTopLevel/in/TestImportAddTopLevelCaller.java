package p.sub;

import java.security.Permission;

public class TestImportAddTopLevelCaller {
	public void foo(){
		new p.TestImportAddTopLevel().foo(new Permission[0], 99);
	}
}
