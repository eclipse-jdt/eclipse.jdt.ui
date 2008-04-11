package ref;

import source.BaseClass;
import source.Color;
import source.sub.InSubPack;

public class SubClass extends BaseClass {
	public SubClass(int count) {
		super(count + 1);
		fProtected= 42;
	}

	protected void baseMethod() {
		super.baseMethod();
		referencedVirtualMethod();
		new InSubPack();
	}
	
	protected int compareTo(BaseClass other) {
		return +1;
	}
	
	@Override
	public void paintColor(Color color) {
		// don't paint
	}
}
