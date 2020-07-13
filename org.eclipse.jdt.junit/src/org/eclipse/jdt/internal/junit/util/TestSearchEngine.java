/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.util;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.launcher.ITestKind;


/**
 * Custom Search engine for suite() methods
 * @see CoreTestSearchEngine
 */
public class TestSearchEngine extends CoreTestSearchEngine {

	public static IType[] findTests(IRunnableContext context, final IJavaElement element, final ITestKind testKind) throws InvocationTargetException, InterruptedException {
		final Set<IType> result= new HashSet<>();

		IRunnableWithProgress runnable= progressMonitor -> {
			try {
				testKind.getFinder().findTestsInContainer(element, result, progressMonitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
		context.run(true, true, runnable);
		return result.toArray(new IType[result.size()]);
	}

}
