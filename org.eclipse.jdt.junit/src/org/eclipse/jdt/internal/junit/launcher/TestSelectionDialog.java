/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

 
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;

/**
 * A dialog to select a test class or a test suite from a list of types.
 */
public class TestSelectionDialog extends TwoPaneElementSelector {

	private IJavaProject fProject;
	private IType[] fTypes;
	
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
	public TestSelectionDialog(Shell shell, IJavaProject project)
	{
		super(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS), 
			new PackageRenderer());

		fProject= project;
	}
	
	public TestSelectionDialog(Shell shell, IType[] types) {
		super(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS), 
				new PackageRenderer());
		fTypes= types;
	}

	/**
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		//WorkbenchHelp.setHelp(newShell, new Object[] { IJavaHelpContextIds.MAINTYPE_SELECTION_DIALOG });
	}

	/*
	 * @see Window#open()
	 */
	public int open() {
		if (fTypes == null) {
			fTypes= new IType[0];
			try {
				fTypes= TestSearchEngine.findTests(new Object[]{fProject});
			} catch (InterruptedException e) {
				return CANCEL;
			} catch (InvocationTargetException e) {
				JUnitPlugin.log(e.getTargetException());
				return CANCEL;
			}
		}
		
		setElements(fTypes);
		return super.open();
	}
	
}
