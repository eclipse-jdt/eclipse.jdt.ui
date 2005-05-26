package p;

class Generic<G> {
	void /*target*/take(G g) {}
}

class Impl extends Generic<Integer> {
	void /*ripple*/take(Integer g) {}
}

class Impl2 extends Generic<String> {
	void /*ripple*/take(String g) {}
}
