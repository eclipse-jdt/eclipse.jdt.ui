import java.util.*;

class A_testParameterDeclWithOverride_in {
	static class X {
		public void foo(AbstractCollection v1){
			Collection c = v1;
		}
	}
	static class Y extends X {
		public void foo(AbstractCollection v2){
			v2 = new ArrayList();
		}
	}
}
