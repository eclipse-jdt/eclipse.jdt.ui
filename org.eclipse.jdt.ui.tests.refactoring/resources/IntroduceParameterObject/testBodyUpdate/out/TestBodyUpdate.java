package p;

import java.security.Permission;

class s {
	int i(){return 5;}
}

public class TestBodyUpdate extends s{
	Permission p;
	public static class FooParameter {
		public Permission p;
		public String s;
		public int i;
		public FooParameter(Permission p, String s, int i) {
			this.p = p;
			this.s = s;
			this.i = i;
		}
	}
	public void foo(FooParameter parameterObject){
		int i = parameterObject.i;
		String s = parameterObject.s;
		i=i();
		s=new s().i()+"";
		i+=super.i();
		this.p=parameterObject.p;
	}
	
}
