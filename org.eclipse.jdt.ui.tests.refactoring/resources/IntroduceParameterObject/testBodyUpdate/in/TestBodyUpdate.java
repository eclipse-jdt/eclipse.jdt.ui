package p;

import java.security.Permission;

class s {
	int i(){return 5;}
}

public class TestBodyUpdate extends s{
	Permission p;
	public void foo(Permission p, String s, int i){
		i=i();
		s=new s().i()+"";
		i+=super.i();
		this.p=p;
	}
	
}
