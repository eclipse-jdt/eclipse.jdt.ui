package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.DecoratingLabelProvider;

import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.OverrideIndicatorLabelDecorator;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Decorator prepared for the switch to use lightweight label decorators:
 * uncomment the lbale decorator entries in plugin.xml and change
 * USE_LIGHTWEIGHT to true.
 * Certain views don't want problems or override indicators, so they signal this
 * in the constructor. So on each getImage the corrsponding decorators are
 * turned off and on again.
  */
public class DecoratingJavaLabelProvider extends DecoratingLabelProvider {

	private static final boolean USE_LIGHTWEIGHT= false;
	
	private static final String PROBLEM_DECORATOR_ID= "org.eclipse.jdt.ui.problem.decorator";
	private static final String OVERRIDE_DECORATOR_ID= "org.eclipse.jdt.ui.override.decorator";
	
	private boolean fUseErrorTick;
	private boolean fUseOverride;

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
		fUseErrorTick= errorTick;
		fUseOverride= override;
		if (!USE_LIGHTWEIGHT) {
			if (errorTick) {
				labelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
			}
			if (override) {
				labelProvider.addLabelDecorator(new OverrideIndicatorLabelDecorator(null));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (USE_LIGHTWEIGHT) {
			IDecoratorManager manager= PlatformUI.getWorkbench().getDecoratorManager();
			
		 	boolean disableErrorTick= manager.getEnabled(PROBLEM_DECORATOR_ID) && !fUseErrorTick;
			boolean disableOverride= manager.getEnabled(OVERRIDE_DECORATOR_ID) && !fUseOverride;
			try {
				if (disableErrorTick) {
					manager.setEnabled(PROBLEM_DECORATOR_ID, false);
				}
				if (disableOverride) {
					manager.setEnabled(OVERRIDE_DECORATOR_ID, false);
				}
				Image image= super.getImage(element);
				if (disableErrorTick) {
					manager.setEnabled(PROBLEM_DECORATOR_ID, true);
				}
				if (disableOverride) {
					manager.setEnabled(OVERRIDE_DECORATOR_ID, true);
				}
				return image;
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (USE_LIGHTWEIGHT) {
			IDecoratorManager manager= PlatformUI.getWorkbench().getDecoratorManager();
			
			boolean disableErrorTick= manager.getEnabled(PROBLEM_DECORATOR_ID) && !fUseErrorTick;
			boolean disableOverride= manager.getEnabled(OVERRIDE_DECORATOR_ID) && !fUseOverride;
			try {
				if (disableErrorTick) {
					manager.setEnabled(PROBLEM_DECORATOR_ID, false);
				}
				if (disableOverride) {
					manager.setEnabled(OVERRIDE_DECORATOR_ID, false);
				}
				String text= super.getText(element);
				if (disableErrorTick) {
					manager.setEnabled(PROBLEM_DECORATOR_ID, true);
				}
				if (disableOverride) {
					manager.setEnabled(OVERRIDE_DECORATOR_ID, true);
				}
				return text;
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return super.getText(element);
	}

}
