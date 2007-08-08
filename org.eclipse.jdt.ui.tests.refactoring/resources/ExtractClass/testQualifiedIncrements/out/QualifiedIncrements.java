package p;
public class QualifiedIncrements {
        private QualifiedIncrementsParameter parameterObject = new QualifiedIncrementsParameter();
		private void foo() {
                QualifiedIncrements qi = new QualifiedIncrements();
                qi.parameterObject.setTest(qi.parameterObject.getTest() + 1);
                new QualifiedIncrements().parameterObject.setTest(new QualifiedIncrements().parameterObject.getTest() + 1);
                next().parameterObject.setTest(next().parameterObject.getTest() + 1);
                (qi).parameterObject.setTest((qi).parameterObject.getTest() + 1);
                new QualifiedIncrements().parameterObject.setTest(1);
        }
		private QualifiedIncrements next() {
			return new QualifiedIncrements();
		}
}
