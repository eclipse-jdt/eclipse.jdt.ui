// 10, 24 -> 10, 36   AllowLoadtime == false
package p;
record User(String token) {
}

class A {
	User user = new User("x");

	void test() {
		String a = use(user.token());
		String b = use(user.token());
	}

	String use(String s) {
		return s;
	}
}
