package p;

import java.util.List;

public class CtorTypeArgBounds<T, L extends List<T>> {
	private L _attribute;

	/*[*/CtorTypeArgBounds/*]*/(L list) {
		_attribute = list;
	}
}
