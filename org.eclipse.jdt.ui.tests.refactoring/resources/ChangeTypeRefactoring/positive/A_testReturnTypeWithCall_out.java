import java.util.*;

class A_testReturnTypeWithCall_in {
	public AbstractList foo(){
		return new ArrayList();
	}
	public void bar(){
		List l = this.foo();	
	}
}
