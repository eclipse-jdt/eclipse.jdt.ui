package p;

public class QualifiedName {
	public static QualifiedName createQualifiedName() {
		return new QualifiedName();
	}

	void foo() {
		createQualifiedName();
	}
}
