package p;

public class JavadocRef_in {

	private JavadocRef_in() {
	}

	/**
	 * See {@link #createJavadocRef_in()}.
	 * @param args
	 * @see JavadocRef_in#createJavadocRef_in()
	 */
	public static void main(String[] args) {
		JavadocRef_in ref= createJavadocRef_in();
	}

	public static JavadocRef_in createJavadocRef_in() {
		return new JavadocRef_in();
	}
}
