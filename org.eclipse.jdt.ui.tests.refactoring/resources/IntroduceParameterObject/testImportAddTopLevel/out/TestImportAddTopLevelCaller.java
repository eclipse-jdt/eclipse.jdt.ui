package p.sub;

import java.security.Permission;

import p.parameters.TestImportAddTopLevelParameter;

public class TestImportAddTopLevelCaller {
	public void foo(){
		new p.TestImportAddTopLevel().foo(new TestImportAddTopLevelParameter(new Permission[0], 99));
	}
}
