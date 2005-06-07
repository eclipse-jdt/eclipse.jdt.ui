package p;

import java.util.List;

public class CtorTypeArgBounds<T, L extends List<T>> {
	public static <T, L extends List<T>> CtorTypeArgBounds<T, L> createCtorTypeArgBounds(L list) {
		return new CtorTypeArgBounds<T, L>(list);
	}

	private L _attribute;

	/*[*/private CtorTypeArgBounds/*]*/(L list) {
		_attribute = list;
	}
}
