package p;
public class QualifiedIncrements {
        private int test;
        private void foo() {
                QualifiedIncrements qi = new QualifiedIncrements();
                qi.test++;
                new QualifiedIncrements().test++;
                next().test++;
                (qi).test++;
                new QualifiedIncrements().test= 1;
        }
		private QualifiedIncrements next() {
			return new QualifiedIncrements();
		}
}
