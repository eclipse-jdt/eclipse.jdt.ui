package p;

interface SAM1 {
	I apply(X x);
}

interface SAM2 {
	I apply(I x);
}

interface SAM3 {
	I apply(I x);
}

class Util {
	void f_via_method_ref(X x1) {
		SAM1 sam1 = X::methodN;
		sam1.apply(x1).methodN();
	}

	void f_via_lambda(I x2) {
		SAM2 sam2 = x -> x.methodN();
		sam2.apply(x2).methodN();
	}

	void f_via_anonymous_class(I x3) {
		SAM3 sam3 = new SAM3() {
			@Override
			public I apply(I x) {
				return x.methodN();
			}
		};
		sam3.apply(x3).methodN();
	}
}