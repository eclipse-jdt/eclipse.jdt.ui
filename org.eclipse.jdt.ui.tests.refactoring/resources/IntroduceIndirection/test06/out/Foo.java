package p;

public class Foo {
	
	// Test correct "thisification".

	class X extends Bar {
		
		{
			Foo.bar(this);
		}
	}

	/* (non-Javadoc)
	 * @see p.Bar#getDisplay()
	 */
	public static void bar(Bar target) {
		target.getDisplay();
	}
}
