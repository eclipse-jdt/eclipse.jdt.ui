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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportReferencesCollector;

public class ImportRemover {
	/*
	 * Potential optimization in getImportsToRemove():
	 * Run ImportReferencesCollector on fRemovedNodes first.
	 * Only visit rest of AST when removed type refs found.
	 */
	private Set/*<String>*/ fAddedImports;
	private List/*<ASTNode>*/ fRemovedNodes;
	private final CompilationUnit fRoot;
	private final IJavaProject fProject;

	public ImportRemover(IJavaProject project, CompilationUnit root) {
		fProject= project;
		fRoot= root;
		fAddedImports= new HashSet();
		fRemovedNodes= new ArrayList();
	}

	public void registerAddedImport(String typeName) {
		int dot= typeName.indexOf('.');
		if (dot == -1)
			fAddedImports.add(typeName);
		else
			fAddedImports.add(typeName.substring(dot + 1));
	}
	
	public void registerRemovedNode(ASTNode removed) {
		fRemovedNodes.add(removed);
	}

	public boolean hasRemovedNodes() {
		return fRemovedNodes.size() != 0;
	}
	
	public ITypeBinding[] getImportsToRemove() {
		ArrayList/*<SimpleName>*/ simpleNames= new ArrayList();
		fRoot.accept(new ImportReferencesCollector(fProject, null, simpleNames, null));
		List/*<SimpleName>*/ removedTypeRefs= new ArrayList();
		List/*<SimpleName>*/ notRemovedTypeRefs= new ArrayList();
		divideTypeRefs(simpleNames, removedTypeRefs, notRemovedTypeRefs);
		if (removedTypeRefs.size() == 0)
			return new ITypeBinding[0];
		
		HashMap/*<String, ITypeBinding>*/ potentialRemoves= getPotentialRemoves(removedTypeRefs);
		for (Iterator iter= notRemovedTypeRefs.iterator(); iter.hasNext();) {
			SimpleName name= (SimpleName) iter.next();
			potentialRemoves.remove(name.getIdentifier());
		}
		
		Collection importsToRemove= potentialRemoves.values();
		return (ITypeBinding[]) importsToRemove.toArray(new ITypeBinding[importsToRemove.size()]);
	}

	private HashMap getPotentialRemoves(List removedTypeRefs) {
		HashMap/*<String, ITypeBinding>*/ potentialRemoves= new HashMap();
		for (Iterator iter= removedTypeRefs.iterator(); iter.hasNext();) {
			SimpleName name= (SimpleName) iter.next();
			if (fAddedImports.contains(name.getIdentifier()))
				continue; // don't remove added imports
			IBinding binding= name.resolveBinding();
			if (binding != null && binding instanceof ITypeBinding)
				potentialRemoves.put(name.getIdentifier(), binding); // only remove refs with type bindings
		}
		return potentialRemoves;
	}

	private void divideTypeRefs(List/*<SimpleName>*/ simpleNames, List/*<SimpleName>*/ removedTypeRefs, List/*<SimpleName>*/ notRemovedTypeRefs) {
		int[] removedStartsEnds= new int[2 * fRemovedNodes.size()];
		for (int i= 0; i < fRemovedNodes.size(); i++) {
			ASTNode node= (ASTNode) fRemovedNodes.get(i);
			int start= node.getStartPosition();
			removedStartsEnds[2 * i]= start;
			removedStartsEnds[2 * i + 1]= start + node.getLength();
		}
		for (Iterator iter= simpleNames.iterator(); iter.hasNext();) {
			SimpleName ref= (SimpleName) iter.next();
			if (isInRemoved(ref, removedStartsEnds))
				removedTypeRefs.add(ref);
			else
				notRemovedTypeRefs.add(ref);
		}
	}

	private boolean isInRemoved(SimpleName ref, int[] removedStartsEnds) {
		int start= ref.getStartPosition();
		int end= start + ref.getLength();
		for (int i= 0; i < removedStartsEnds.length; i+= 2) {
			if (start >= removedStartsEnds[i] && end <= removedStartsEnds[i+1])
				return true;
		}
		return false;
	}
	
}
