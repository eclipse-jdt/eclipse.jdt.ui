package p;

public class QualifiedName {
	void foo() {
		new p./*[*/QualifiedName/*]*/();
	}
}
