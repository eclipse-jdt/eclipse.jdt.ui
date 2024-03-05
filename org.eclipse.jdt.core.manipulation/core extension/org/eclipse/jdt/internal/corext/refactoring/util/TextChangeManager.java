/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;


/**
 * A <code>TextChangeManager</code> manages associations between <code>ICompilationUnit</code>
 * or <code>IFile</code> and <code>TextChange</code> objects.
 */
public class TextChangeManager {

	private Map<ICompilationUnit, TextChange> fMap= new HashMap<>(10);

	private final boolean fKeepExecutedTextEdits;

	public TextChangeManager() {
		this(false);
	}

	public TextChangeManager(boolean keepExecutedTextEdits) {
		fKeepExecutedTextEdits= keepExecutedTextEdits;
	}

	/**
	 * Adds an association between the given compilation unit and the passed
	 * change to this manager.
	 *
	 * @param cu the compilation unit (key)
	 * @param change the change associated with the compilation unit
	 */
	public void manage(ICompilationUnit cu, TextChange change) {
		fMap.put(cu, change);
	}

	/**
	 * Returns the <code>TextChange</code> associated with the given compilation unit.
	 * If the manager does not already manage an association it creates a one.
	 *
	 * @param cu the compilation unit for which the text buffer change is requested
	 * @return the text change associated with the given compilation unit.
	 */
	public TextChange get(ICompilationUnit cu) {
		TextChange result= fMap.get(cu);
		if (result == null) {
			result= new CompilationUnitChange(cu.getElementName(), cu);
			result.setKeepPreviewEdits(fKeepExecutedTextEdits);
			fMap.put(cu, result);
		}
		return result;
	}

	/**
	 * Removes the <code>TextChange</code> managed under the given key
	 * <code>unit</code>.
	 *
	 * @param unit the key determining the <code>TextChange</code> to be removed.
	 * @return the removed <code>TextChange</code>.
	 */
	public TextChange remove(ICompilationUnit unit) {
		return fMap.remove(unit);
	}

	/**
	 * Returns all text changes managed by this instance.
	 *
	 * @return all text changes managed by this instance
	 */
	public TextChange[] getAllChanges(){
		Set<ICompilationUnit> cuSet= fMap.keySet();
		ICompilationUnit[] cus= cuSet.toArray(new ICompilationUnit[cuSet.size()]);
		// sort by cu name:
		Arrays.sort(cus, (o1, o2) -> {
			String name1= o1.getElementName();
			String name2= o2.getElementName();
			return name1.compareTo(name2);
		});

		TextChange[] textChanges= new TextChange[cus.length];
		for (int i= 0; i < cus.length; i++) {
			textChanges[i]= fMap.get(cus[i]);
		}
		return textChanges;
	}

	/**
	 * Returns all compilation units managed by this instance.
	 *
	 * @return all compilation units managed by this instance
	 */
	public ICompilationUnit[] getAllCompilationUnits(){
		return fMap.keySet().toArray(new ICompilationUnit[fMap.size()]);
	}

	/**
	 * Clears all associations between resources and text changes.
	 */
	public void clear() {
		fMap.clear();
	}

	/**
	 * Returns if any text changes are managed for the specified compilation unit.
	 *
	 * @param cu the compilation unit
	 * @return <code>true</code> if any text changes are managed for the specified compilation unit and <code>false</code> otherwise
	 */
	public boolean containsChangesIn(ICompilationUnit cu){
		return fMap.containsKey(cu);
	}
}

