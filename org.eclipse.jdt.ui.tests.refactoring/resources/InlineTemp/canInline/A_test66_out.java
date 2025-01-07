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
		if (enumConstant.id == field.original().id) {
			unenumerated.remove(enumConstant);
		}
	}
}
