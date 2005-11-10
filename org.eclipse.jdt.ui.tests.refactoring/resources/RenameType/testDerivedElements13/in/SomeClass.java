package p;

/**
 * Local variables: Prefix "pm", Suffix "_pm"
 * Parameters: Prefix "lv", Suffix "_lv"
 *
 */
public class SomeClass {
	
	public void foo1(SomeClass pmSomeClass) {
		SomeClass lvSomeClass;
		SomeClass lvSomeClass_lv;
		SomeClass someClass_lv;
		SomeClass pmSomeClass_pm; // do not rename this!
	}
	
	public void foo2(SomeClass pmSomeClass_pm, SomeClass lvSomeClass) { // don't rename the second param.
	}
	
	public void foo3(SomeClass someClass_pm) {
	}
	
}
