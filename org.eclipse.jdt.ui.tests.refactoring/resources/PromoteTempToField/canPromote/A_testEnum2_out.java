//10, 21, 10, 21
package p;

class C {
	public static final Member fM= Member.SECOND;
	enum Member {
		FIRST, SECOND;
	}
	void use() {
		Member m2= fM;
	}
}