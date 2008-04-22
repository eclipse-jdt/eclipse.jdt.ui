package p;
class B {
	A participant;

	boolean participates(A a) {
		return (participant.id == a.id);
	}
}