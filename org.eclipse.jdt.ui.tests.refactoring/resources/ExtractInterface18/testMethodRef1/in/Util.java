package p;

interface SAM1 {
	X apply(X x);
}

interface SAM2 {
	X apply(X x);
}

interface SAM3 {
	X apply(X x);
}

class Util {
	void f_via_method_ref(X x1) {
		SAM1 sam1 = X::methodN;
		sam1.apply(x1).methodN();
	}

	void f_via_lambda(X x2) {
		SAM2 sam2 = x -> x.methodN();
		sam2.apply(x2).methodN();
	}

	void f_via_anonymous_class(X x3) {
		SAM3 sam3 = new SAM3() {
			@Override
			public X apply(X x) {
				return x.methodN();
			}
		};
		sam3.apply(x3).methodN();
	}
}