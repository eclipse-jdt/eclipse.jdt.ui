import java.util.*;

class A_testParameterNameWithOverride_in {
	static class X {
		public void foo(Collection v1){
			Collection c = v1;
		}
	}
	static class Y extends X {
		public void foo(Collection v2){
			v2 = new ArrayList();
		}
	}
}
