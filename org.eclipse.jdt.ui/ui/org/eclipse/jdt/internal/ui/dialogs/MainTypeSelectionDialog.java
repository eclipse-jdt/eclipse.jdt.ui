package org.eclipse.jdt.internal.ui.dialogs;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
 
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;

/**
 * A dialog to select a type from a list of types. The dialog allows
 * multiple selections.
 */
public class MainTypeSelectionDialog extends TwoPaneElementSelector {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	private final static String PREFIX= "type_selector.";
	private final static String NO_MAPPING_PREFIX= PREFIX+"no_mapping.";
		
	
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
		super(shell, "", null, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS), 
			new PackageRenderer(),
			ignoreCase, true); 
		fRunnableContext= context;
		Assert.isNotNull(fRunnableContext);
		fScope= scope;
		Assert.isNotNull(fScope);
		fStyle= style;
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
		setInitialSelection("A");
		return super.open();
	}
	
}