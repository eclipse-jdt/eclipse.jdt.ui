/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class SelectionExpression extends CompositeExpression {

	private IConfigurationElement fElement;
	private String fAdaptable;
	private int fMode;
	private int fSize;
	
	public static final String NAME= "selection"; //$NON-NLS-1$
	private static final String ADAPTABLE= "adaptable"; //$NON-NLS-1$
	private static final String SIZE= "count"; //$NON-NLS-1$
	
	private static final int ANY_NUMBER=	5;
	private static final int EXACT=			4;
	private static final int ONE_OR_MORE=	3;
	private static final int NONE_OR_ONE= 	2;
	private static final int NONE= 			1;
	private static final int UNKNOWN= 		0;
	
	
	public SelectionExpression(IConfigurationElement element) {
		fElement= element;
		fAdaptable= element.getAttribute(ADAPTABLE);
		String size = element.getAttribute(SIZE);
		if (size == null)
			size = "*"; //$NON-NLS-1$
		if (size.equals("*")) //$NON-NLS-1$
			fMode= ANY_NUMBER;
		else if (size.equals("?")) //$NON-NLS-1$
			fMode= NONE_OR_ONE;
		else if (size.equals("!")) //$NON-NLS-1$
			fMode= NONE;
		else if (size.equals("+")) //$NON-NLS-1$
			fMode= ONE_OR_MORE;
		else {
			try {
				fSize= Integer.parseInt(size);
				fMode= EXACT;
			} catch (NumberFormatException e) {
				fMode= UNKNOWN;
			}
		}
	}

	public int evaluate(Object element) throws CoreException {
		Class clazz= null;
		if (fAdaptable != null) {
			IPluginDescriptor pd= fElement.getDeclaringExtension().getDeclaringPluginDescriptor();
			ClassLoader loader= pd.getPluginClassLoader();
			try {
				clazz= loader.loadClass(fAdaptable);
			} catch (ClassNotFoundException e) {
				throw new CoreException(new Status(
					IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, 
					"Class specified in adaptable attribute not found",
					e));
			}
		}
		Object[] elements= (Object[])element;
		if (!checkSize(elements))
			return ITestResult.FALSE;
		int result= ITestResult.TRUE;
		for (int i= 0; i < elements.length; i++) {
			Object o= elements[i];
			if (clazz != null && o instanceof IAdaptable) {
				o= ((IAdaptable)o).getAdapter(clazz);
				if (o == null)
					return ITestResult.FALSE;
			}
			result= TestResult.and(result, evaluateAnd(o));
		}
		return result;
	}

	private boolean checkSize(Object[] elements) {
		int size= elements.length;
		switch (fMode) {
			case UNKNOWN:
				return false;
			case NONE:
				return size == 0;
			case ONE_OR_MORE:
				return size >= 1;
			case EXACT:
				return fSize == size;
			case ANY_NUMBER:
				return true;	
		}
		return false;
	}
}
