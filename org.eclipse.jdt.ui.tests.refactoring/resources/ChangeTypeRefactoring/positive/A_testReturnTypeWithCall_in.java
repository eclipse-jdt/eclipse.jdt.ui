import java.util.*;

class A_testReturnTypeWithCall_in {
	public ArrayList foo(){
		return new ArrayList();
	}
	public void bar(){
		List l = this.foo();	
	}
}
