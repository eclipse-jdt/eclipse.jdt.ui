package p;

/**
 * Local variables: Prefix "pm", Suffix "_pm"
 * Parameters: Prefix "lv", Suffix "_lv"
 *
 */
public class SomeOtherClass {
	
	public void foo1(SomeOtherClass pmSomeOtherClass) {
		SomeOtherClass lvSomeOtherClass;
		SomeOtherClass lvSomeOtherClass_lv;
		SomeOtherClass someOtherClass_lv;
		SomeOtherClass pmSomeClass_pm; // do not rename this!
	}
	
	public void foo2(SomeClass pmSomeClass_pm, SomeClass lvSomeClass) { // don't rename the second param.
	}
	
	public void foo3(SomeOtherClass someOtherClass_pm) {
	}
	
}
