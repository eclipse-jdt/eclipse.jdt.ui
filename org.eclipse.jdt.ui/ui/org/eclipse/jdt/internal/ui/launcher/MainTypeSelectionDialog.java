/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.launcher;
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.TwoPaneElementSelector;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;

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
			super(JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_POST_QUALIFIED | JavaElementLabelProvider.SHOW_ROOT);	
		}

		public Image getImage(Object element) {
			return super.getImage(((IType)element).getPackageFragment());
		}
		
		public String getText(Object element) {
			return super.getText(((IType)element).getPackageFragment());
		}
	}
	
	/**
	 * Constructor.
	 */
	public MainTypeSelectionDialog(Shell shell, IRunnableContext context,
		IJavaSearchScope scope, int style)
	{
		super(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS), 
			new PackageRenderer());

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
	 * @see Window#create()
	 */
	public void create() {	
		if (getFilter() == null)
			setFilter("A"); //$NON-NLS-1$
			
		super.create();				
	}

	/*
	 * @see Window#open()
	 */
	public int open() {
		MainMethodSearchEngine engine= new MainMethodSearchEngine();
		IType[] types;
		try {
			types= engine.searchMainMethods(fRunnableContext, fScope, fStyle);
		} catch (InterruptedException e) {
			return CANCEL;
		} catch (InvocationTargetException e) {
			//XX: to do
			ExceptionHandler.handle(e, "Error", e.getMessage());
			return CANCEL;
		}
		
		setElements(types);
		return super.open();
	}
	
}