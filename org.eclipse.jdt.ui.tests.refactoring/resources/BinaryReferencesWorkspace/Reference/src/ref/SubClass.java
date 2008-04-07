package ref;

import source.BaseClass;
import source.Color;

public class SubClass extends BaseClass {
	public SubClass(int count) {
		super(count + 1);
	}

	protected void baseMethod() {
		super.baseMethod();
		referencedVirtualMethod();
	}
	
	protected int compareTo(BaseClass other) {
		return +1;
	}
	
	@Override
	public void paintColor(Color color) {
		// don't paint
	}
}
