package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.viewers.DecoratingLabelProvider;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.OverrideIndicatorLabelDecorator;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;

/**
  */
public class DecoratingJavaLabelProvider extends DecoratingLabelProvider {

	/**
	 * Decorating label provider for Java. Combines a JavaUILabelProvider
	 * with problem and override indicuator with the workbench decorator (label
	 * decorator extension point).
	 */
	public DecoratingJavaLabelProvider(JavaUILabelProvider labelProvider) {
		this(labelProvider, true, true);
	}

	/**
	 * Decorating label provider for Java. Combines a JavaUILabelProvider
	 * (if enabled with problem and override indicator) with the workbench
	 * decorator (label decorator extension point).
	 */
	public DecoratingJavaLabelProvider(JavaUILabelProvider labelProvider, boolean errorTick, boolean override) {
		super(labelProvider, PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
		if (errorTick) {
			labelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
		}
		if (override) {
			labelProvider.addLabelDecorator(new OverrideIndicatorLabelDecorator(null));
		}
	}

}
