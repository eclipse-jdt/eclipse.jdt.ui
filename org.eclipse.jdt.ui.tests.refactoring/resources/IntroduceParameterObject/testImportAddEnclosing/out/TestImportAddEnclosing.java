package p;

import java.security.Permission;

public class TestImportAddEnclosing {
	public static class FooParameter {
		public Permission[] permissions;
		public int b;
		public FooParameter(Permission[] permissions, int b) {
			this.permissions = permissions;
			this.b = b;
		}
	}

	public void foo(FooParameter parameterObject){
		
	}
}
