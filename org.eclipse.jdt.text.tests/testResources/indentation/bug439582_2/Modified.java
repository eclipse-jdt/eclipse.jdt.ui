package indentbug;

public class Bug2 {
	static final Object o = new SB()
			.addAll("ABC")
			.addAll("DEF")
			.build();
}

class SB {
	public SB addAll(Object o) {
		return this;
	}

	public SB build() {
		return this;
	}
}