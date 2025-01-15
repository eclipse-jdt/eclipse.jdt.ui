package p;

import java.util.Set;

public class A {
	public class B {
		public int id;
	}

	private class C {
		public B original() {
			return new B();
		}
	}

	public void foo(B enumConstant, Set<B> unenumerated) {
		C field = new C();
		int intValue = field.original().id;
		if (enumConstant.id == intValue) {
			unenumerated.remove(enumConstant);
		}
	}
}
