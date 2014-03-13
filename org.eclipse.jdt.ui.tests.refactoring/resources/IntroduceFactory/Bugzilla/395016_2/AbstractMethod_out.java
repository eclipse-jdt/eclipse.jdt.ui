package p;
abstract class AbstractMethod {
	public static AbstractMethod createAbstractMethod() {
		return new AbstractMethod() {
			/* (non-Javadoc)
			 * @see p.AbstractMethod#foo()
			 */
			@Override
			void foo() {
				// TODO Auto-generated method stub
				
			}
		};
	}

	private /*[*/AbstractMethod/*]*/() {
	}

	abstract void foo();
}