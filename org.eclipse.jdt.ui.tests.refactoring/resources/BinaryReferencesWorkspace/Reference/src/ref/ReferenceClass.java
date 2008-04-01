package ref;

import source.BaseClass;

public class ReferenceClass extends BaseClass {
	public ReferenceClass(int count) {
		super(count + 1);
	}

	protected void baseMethod() {
		super.baseMethod();
	}
	
	protected int compareTo(BaseClass other) {
		return +1;
	}
}
