import java.util.*;

class A_testParameterTypeWithOverride_in {
	static class X {
		public void foo(AbstractList v1){
			Collection c = v1;
		}
	}
	static class Y extends X {
		public void foo(AbstractList v2){
			v2 = new ArrayList();
		}
	}
}
