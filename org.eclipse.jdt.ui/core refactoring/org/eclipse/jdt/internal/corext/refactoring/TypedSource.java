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
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

/**
 * A tuple used to keep source of an element and its type.
 * @see IJavaElement
 * @see org.eclipse.jdt.core.ISourceReference
 */
public class TypedSource {
	
	private final String fSource;
	private final int fType;

	private TypedSource(String source, int type){
		Assert.isNotNull(source);
		Assert.isTrue(canCreateForType(type));
		fSource= source;
		fType= type;				  
	}
	
	public static TypedSource create(String source, int type) {
		if (source == null || ! canCreateForType(type))
			return null;
		return new TypedSource(source, type);
	}

	public String getSource() {
		return fSource;
	}

	public int getType() {
		return fType;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (! (other instanceof TypedSource))
			return false;
		
		TypedSource ts= (TypedSource)other;
		return ts.getSource().equals(getSource()) && ts.getType() == getType();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getSource().hashCode() ^ (97 * getType());
	}

	private static boolean canCreateForType(int type){
		return 		type == IJavaElement.FIELD 
				|| 	type == IJavaElement.TYPE
				|| 	type == IJavaElement.IMPORT_CONTAINER
				|| 	type == IJavaElement.IMPORT_DECLARATION
				|| 	type == IJavaElement.INITIALIZER
				|| 	type == IJavaElement.METHOD
				|| 	type == IJavaElement.PACKAGE_DECLARATION;
	}
	
	
	public static void sortByType(TypedSource[] typedSources){
		Arrays.sort(typedSources, createTypeComparator());
	}

	public static Comparator createTypeComparator() {
		return new Comparator(){
			public int compare(Object arg0, Object arg1) {
				return ((TypedSource)arg0).getType() - ((TypedSource)arg1).getType();
			}
		};
	}
	public static TypedSource[] createTypedSources(IJavaElement[] javaElements) throws CoreException {
		//Map<ICompilationUnit, List<IJavaElement>>
		Map grouped= ReorgUtils.groupByCompilationUnit(Arrays.asList(javaElements));
		List result= new ArrayList(javaElements.length);
		for (Iterator iter= grouped.keySet().iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit) iter.next();
			ASTParser p= ASTParser.newParser(AST.LEVEL_2_0);
			p.setSource(cu);
			CompilationUnit cuNode= (CompilationUnit) p.createAST(null);
			for (Iterator iterator= ((List) grouped.get(cu)).iterator(); iterator.hasNext();) {
				TypedSource[] ts= createTypedSources((IJavaElement) iterator.next(), cu, cuNode);
				if (ts != null)
					result.addAll(Arrays.asList(ts));				
			}
		}
		return (TypedSource[]) result.toArray(new TypedSource[result.size()]);		
	}

	private static TypedSource[] createTypedSources(IJavaElement elem, ICompilationUnit cu, CompilationUnit cuNode) throws CoreException {
		if (! ReorgUtils.isInsideCompilationUnit(elem))
			return null;
		//import containers are different because you cannot paste them directly (there's no AST node for them)
		if (elem.getElementType() == IJavaElement.IMPORT_CONTAINER) 
			return createTypedSourcesForImportContainer(cu, cuNode, (IImportContainer)elem);
		//fields need to be handled differently because of the multi-declaration case
		else if (elem.getElementType() == IJavaElement.FIELD) 
			return new TypedSource[] {create(getFieldSource((IField)elem, cu, cuNode), elem.getElementType())};
		return new TypedSource[] {create(getSourceOfDeclararationNode(elem, cu, cuNode), elem.getElementType())};
	}

	private static TypedSource[] createTypedSourcesForImportContainer(ICompilationUnit cu, CompilationUnit cuNode, IImportContainer container) throws JavaModelException, CoreException {
		IJavaElement[] imports= container.getChildren();
		List result= new ArrayList(imports.length);
		for (int i= 0; i < imports.length; i++) {
			result.addAll(Arrays.asList(createTypedSources(imports[i], cu, cuNode)));
		}
		return (TypedSource[]) result.toArray(new TypedSource[result.size()]);
	}

	private static String getFieldSource(IField field, ICompilationUnit cu, CompilationUnit cuNode) throws CoreException {
		VariableDeclarationFragment declarationFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, cuNode);
		FieldDeclaration declaration= ASTNodeSearchUtil.getFieldDeclarationNode(field, cuNode);
		if (declaration.fragments().size() == 1)
			return getSourceOfDeclararationNode(field, cu, cuNode);
		StringBuffer buff= new StringBuffer();
		IBuffer buffer= cu.getBuffer();
		int firstFragmentOffset= ((ASTNode)declaration.fragments().get(0)).getStartPosition();
		buff.append(buffer.getText(declaration.getStartPosition(), firstFragmentOffset - declaration.getStartPosition()));
		buff.append(buffer.getText(declarationFragment.getStartPosition(), declarationFragment.getLength()));
		buff.append(";"); //$NON-NLS-1$
		return buff.toString();
	}

	private static String getSourceOfDeclararationNode(IJavaElement elem, ICompilationUnit cu, CompilationUnit cuNode) throws JavaModelException, CoreException {
		Assert.isTrue(elem.getElementType() != IJavaElement.IMPORT_CONTAINER);
		ASTNode[] nodes= ASTNodeSearchUtil.getDeclarationNodes(elem, cuNode);
		if (nodes != null && nodes.length == 1) {
			return trimIndent(cu.getBuffer().getText(nodes[0].getStartPosition(), nodes[0].getLength()));
		} else 
			return ""; //$NON-NLS-1$
	}

	private static String trimIndent(String source) {
		return Strings.trimIndentation(source, CodeFormatterUtil.getTabWidth(), false);
	}
}
