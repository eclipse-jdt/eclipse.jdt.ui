package p;

interface SAM1 {
	X apply(I x1);
}

interface SAM2 {
	X apply(X x1);
}

interface SAM3 {
	X apply(I x1);
}

interface SAM4 {
	X apply(I x1);
}

interface SAM5 {
	X apply(I x1);
}

interface SAM6 {
	X apply(I x1);
}

interface SAM7 {
	X apply(I x1);
}

class Base {
	I method1(I x) {
		return new X("created in Base::method1");
	}

	X method2(I x) {
		return new X("created in Base::method2");
	}

	I method3(I x2) {
		return x2;
	}
}

public class Util extends Base {
	X method1(I x) {
		return new X("created in method1");
	}

	X method2(I x) {
		return new X("created in method2");
	}

	String static_method(I x1) {
		SAM1 sam1 = X::staticMethod;
		return sam1.apply(x1).m2();
	}

	String instance_method_via_type(X x2) {
		SAM2 sam2 = p.X::instanceMethod;
		return sam2.apply(x2).m2();
	}

	String constructor_ref(I x3) {
		SAM3 sam3 = X::new;
		return sam3.apply(x3).m2();
	}

	String this_method1(I x4) {
		SAM4 sam4 = this::method1;
		return sam4.apply(x4).m2();
	}

	String super_method2(I x5) {
		SAM5 sam5 = super::method2;
		return sam5.apply(x5).m2();
	}

	String instance_method2(X x6) {
		SAM6 sam6 = x6::instanceMethod2;
		return sam6.apply(x6).m2();
	}

	String instance_method2_via_field(X x7) {
		SAM7 sam7 = x7.field::instanceMethod2;
		return sam7.apply(x7).m2();
	}

	public static void check(String expected, String actual) {
		if (!expected.equals(actual)) {
			System.out.println(expected + " != " + actual);
		} else {
			System.out.println("OK: " + expected);
		}
	}

	public static void main(String[] args) {
		Util util = new Util();
		check("created in X::staticMethod", util.static_method(new X("static_method")));
		check("created in X::instanceMethod", util.instance_method_via_type(new X("instance_method_via_type")));
		check("created in X::new", util.constructor_ref(new X("constructor_ref")));
		check("created in method1", util.this_method1(new X("this_method1")));
		check("created in Base::method2", util.super_method2(new X("super_method2")));
		check("created in X::instanceMethod2", util.instance_method2(new X("instance_method2")));
		check("created in X::instanceMethod2", util.instance_method2_via_field(new X("instance_method2_via_field")));
	}
}
