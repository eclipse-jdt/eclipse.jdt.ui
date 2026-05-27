// 10, 24 -> 10, 36   AllowLoadtime == false
package p;
record User(String token) {
}

class A {
	User user = new User("x");

	void test() {
		String token= user.token();
		String a = use(token);
		String b = use(token);
	}

	String use(String s) {
		return s;
	}
}
