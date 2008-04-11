package ref;

import source.BaseClass;
import source.Color;
import static source.Color.*;

public class ReferenceClass {
	public static void main(String[] args) {
		new BaseClass(1).referencedMethod();
		BaseClass.referencedStaticMethod();
		new BaseClass(1).referencedVirtualMethod();
		new SubClass(1).referencedVirtualMethod();
		BaseClass baseClass = new BaseClass(1);
		baseClass.paintColor(true ? Color.RED : GREEN);
		System.out.println(baseClass.fPublic);
	}
}
