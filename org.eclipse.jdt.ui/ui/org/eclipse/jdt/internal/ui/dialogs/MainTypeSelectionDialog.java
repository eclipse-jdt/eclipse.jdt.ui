/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;
 
import java.util.List;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.operation.IRunnableContext;import org.eclipse.jface.util.Assert;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;

/**
 * A dialog to select a type from a list of types. The dialog allows
 * multiple selections.
 */
public class MainTypeSelectionDialog extends TwoPaneElementSelector {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	private static class PackageRenderer extends JavaElementLabelProvider {
		public PackageRenderer() {
			super(JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION |
				JavaElementLabelProvider.SHOW_CONTAINER |
				JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);	
		}

		public Image getImage(Object element) {
			return super.getImage(((IType)element).getPackageFragment());
		}
		
		public String getText(Object element) {
			return super.getText(((IType)element).getPackageFragment());
		}
	}
	
	public MainTypeSelectionDialog(Shell shell, IRunnableContext context, IJavaSearchScope scope, int style, boolean ignoreCase) {
		super(shell, "", //$NON-NLS-1$
			null, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS), 
			new PackageRenderer(), ignoreCase, true);

		Assert.isNotNull(context);
		Assert.isNotNull(scope);

		fRunnableContext= context;
		fScope= scope;
		fStyle= style;
	}
	
	/**
	 * @see Windows#configureShell
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, new Object[] { IJavaHelpContextIds.MAINTYPE_SELECTION_DIALOG });
	}

	/*
	 * @private
	 */
	public int open() {
		MainMethodSearchEngine engine= new MainMethodSearchEngine();
		List typesFound= engine.searchMethod(fRunnableContext, fScope, fStyle);

		if (typesFound.size() == 0)
			return CANCEL;
		
		IType[] types= (IType[])typesFound.toArray(new IType[typesFound.size()]);
		setElements(types);
		setInitialSelection("A"); //$NON-NLS-1$
		return super.open();
	}
	
}