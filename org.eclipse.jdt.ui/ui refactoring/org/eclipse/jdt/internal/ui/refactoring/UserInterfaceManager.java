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
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

public class UserInterfaceManager {
	
	private Map fMap= new HashMap();
	
	private static class Tuple {
		private Class starter;
		private Class wizard;
		public Tuple(Class s, Class w) {
			starter= s;
			wizard= w;
		}
	}
	
	protected void put(Class processor, Class starter, Class wizard) {
		fMap.put(processor, new Tuple(starter, wizard));
	}

	
	public UserInterfaceStarter getStarter(Refactoring refactoring) {
		RefactoringProcessor processor= (RefactoringProcessor)refactoring.getAdapter(RefactoringProcessor.class);
		if (processor == null)
			return null;
		Tuple tuple= (Tuple)fMap.get(processor.getClass());
		if (tuple == null)
			return null;
		try {
			UserInterfaceStarter starter= (UserInterfaceStarter)tuple.starter.newInstance();
			RefactoringWizard wizard= (RefactoringWizard)tuple.wizard.newInstance();
			wizard.initialize(refactoring);
			starter.initialize(wizard);
			return starter;
		} catch (InstantiationException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}
}
