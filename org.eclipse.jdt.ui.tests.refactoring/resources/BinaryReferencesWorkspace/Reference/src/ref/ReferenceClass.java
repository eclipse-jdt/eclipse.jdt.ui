package ref;

import source.BaseClass;

public class ReferenceClass {
	public static void main(String[] args) {
		new BaseClass(1).referencedMethod();
		BaseClass.referencedStaticMethod();
		new BaseClass(1).referencedVirtualMethod();
		new SubClass(1).referencedVirtualMethod();
		new BaseClass(1).paintColor(null);
	}
}
