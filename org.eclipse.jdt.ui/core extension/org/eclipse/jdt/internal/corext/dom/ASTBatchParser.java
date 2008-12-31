/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.IBinding;

/**
 * Creates AST from a set of compilation units. Uses the
 * batch parser. Splits the set of compilation units in subsets
 * such that it is unlikely that a out of memory exception will occur.
 *
 * @since 3.4
 */
public class ASTBatchParser {

	private static final int MAX_AT_ONCE;
	static {
		long maxMemory= Runtime.getRuntime().maxMemory();
		int ratio= (int) Math.round((double) maxMemory / (64 * 0x100000));
		switch (ratio) {
			case 0:
				MAX_AT_ONCE= 25;
				break;
			case 1:
				MAX_AT_ONCE= 100;
				break;
			case 2:
				MAX_AT_ONCE= 200;
				break;
			case 3:
				MAX_AT_ONCE= 300;
				break;
			case 4:
				MAX_AT_ONCE= 400;
				break;
			default:
				MAX_AT_ONCE= 500;
				break;
		}
	}

	/**
	 * Creates ASTs for each compilation unit in <code>units</code>.
	 * <p>
	 * <code>ASTRequestor.acceptAST</code> is called in no particular order to
	 * pass the compilation unit and the corresponding AST to <code>requestor</code>.
	 * </p>
	 * <p>
	 * The <code>bindingKeys</code> parameter specifies bindings keys
	 * ({@link IBinding#getKey()}) that are to be looked up.
	 * </p>
	 *
	 * @param compilationUnits the compilation units to create ASTs for
	 * @param bindingKeys the binding keys to create bindings for
	 * @param requestor the AST requestor that collects abstract syntax trees and bindings
	 * @param monitor the progress monitor used to report progress and request cancelation,
	 *   or <code>null</code> if none
	 * @see ASTParser#createASTs(ICompilationUnit[], String[], ASTRequestor, IProgressMonitor)
	 */
	public final void createASTs(ICompilationUnit[] compilationUnits, String[] bindingKeys, ASTRequestor requestor, IProgressMonitor monitor) {
		if (compilationUnits.length == 0)
			return;

		if (monitor == null)
			monitor= new NullProgressMonitor();

		monitor.beginTask("", compilationUnits.length); //$NON-NLS-1$
		try {

			ICompilationUnit[][] splited= splitByProject(compilationUnits);
			for (int i= 0; i < splited.length; i++) {
				ICompilationUnit[] units= splited[i];

				if (units.length <= MAX_AT_ONCE) {
					createParser(units[0].getJavaProject()).createASTs(units, bindingKeys, requestor, new SubProgressMonitor(monitor, units.length));
				} else {
					List list= Arrays.asList(units);
					int end= 0;
					int cursor= 0;
					while (cursor < units.length) {
						end= Math.min(end + MAX_AT_ONCE, units.length);
						List toParse= list.subList(cursor, end);

						createParser(units[0].getJavaProject()).createASTs((ICompilationUnit[]) toParse.toArray(new ICompilationUnit[toParse.size()]), bindingKeys, requestor,
								new SubProgressMonitor(monitor, toParse.size()));
						cursor= end;
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates a new parser which can be used to create ASTs
	 * for compilation units in <code>project</code>
	 * <p>
	 * Subclasses may override
	 * </p>
	 *
	 * @param project the project for which ASTs are been generated
	 * @return an AST parser capable of creating ASTs of compilation units in project
	 */
	protected ASTParser createParser(IJavaProject project) {
		ASTParser result= ASTParser.newParser(AST.JLS3);
		result.setResolveBindings(true);
		result.setProject(project);

		return result;
	}

	private static ICompilationUnit[][] splitByProject(ICompilationUnit[] units) {
		if (hasOnlyOneProject(units))
			return new ICompilationUnit[][] { units };

		Hashtable projectTable= new Hashtable();

		for (int i= 0; i < units.length; i++) {
			ICompilationUnit unit= units[i];
			ArrayList list= (ArrayList) projectTable.get(unit.getJavaProject());
			if (list == null) {
				list= new ArrayList();
				projectTable.put(unit.getJavaProject(), list);
			}
			list.add(unit);
		}

		Collection values= projectTable.values();

		ICompilationUnit[][] result= new ICompilationUnit[values.size()][];
		int i= 0;
		for (Iterator iterator= values.iterator(); iterator.hasNext();) {
			ArrayList cus= (ArrayList) iterator.next();
			result[i]= (ICompilationUnit[]) cus.toArray(new ICompilationUnit[cus.size()]);
			i++;
		}

		return result;
	}

	private static boolean hasOnlyOneProject(ICompilationUnit[] units) {
		IJavaProject javaProject= units[0].getJavaProject();
		for (int i= 1; i < units.length; i++) {
			if (!javaProject.equals(units[i].getJavaProject()))
				return false;
		}

		return true;
	}
}