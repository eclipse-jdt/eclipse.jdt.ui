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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ASTNodeDeleteUtil;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

/**
 * A tuple used to keep source of an element and its type.
 * @see IJavaElement
 * @see ISourceReference
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
		Map grouped= ReorgUtils.groupByCompilationUnit(Arrays.asList(javaElements));
		List result= new ArrayList(javaElements.length);
		for (Iterator iter= grouped.keySet().iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit) iter.next();
			List elems= (List) grouped.get(cu);
			CompilationUnit cuNode= AST.parseCompilationUnit(cu, false);
			for (Iterator iterator= elems.iterator(); iterator.hasNext();) {
				TypedSource[] ts= create((IJavaElement) iterator.next(), cu, cuNode);
				if (ts != null)
					result.addAll(Arrays.asList(ts));				
			}
		}
		return (TypedSource[]) result.toArray(new TypedSource[result.size()]);		
	}

	private static TypedSource[] create(IJavaElement elem, ICompilationUnit cu, CompilationUnit cuNode) throws CoreException {
		if (! ReorgUtils.isInsideCompilationUnit(elem))
			return null;
		//import containers are different because you cannot paste them directly (there's no AST node for them)
		if (elem.getElementType() != IJavaElement.IMPORT_CONTAINER)
			return new TypedSource[] {create(getSource(elem, cu, cuNode), elem.getElementType())};

		IImportContainer container= (IImportContainer)elem;
		IJavaElement[] imports= container.getChildren();
		List result= new ArrayList(imports.length);
		for (int i= 0; i < imports.length; i++) {
			result.addAll(Arrays.asList(create(imports[i], cu, cuNode)));
		}
		return (TypedSource[]) result.toArray(new TypedSource[result.size()]);
	}

	private static String getSource(IJavaElement elem, ICompilationUnit cu, CompilationUnit cuNode) throws CoreException {
		//fields need to be handled differently because of the multi-declaration case
		if (elem.getElementType() == IJavaElement.FIELD)
			return getFieldSource((IField) elem, cu, cuNode);
		return getSourceOfDeleteEdit(elem, cu, cuNode);		
	}

	private static String getFieldSource(IField field, ICompilationUnit cu, CompilationUnit cuNode) throws CoreException {
		VariableDeclarationFragment declarationFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, cuNode);
		FieldDeclaration declaration= ASTNodeSearchUtil.getFieldDeclarationNode(field, cuNode);
		if (declaration.fragments().size() == 1)
			return getSourceOfDeleteEdit(field, cu, cuNode);
		StringBuffer buff= new StringBuffer();
		IBuffer buffer= cu.getBuffer();
		//get(0) is ok - checked before
		int firstFragmentOffset= ((ASTNode)declaration.fragments().get(0)).getStartPosition();
		buff.append(buffer.getText(declaration.getStartPosition(), firstFragmentOffset - declaration.getStartPosition()));
		buff.append(buffer.getText(declarationFragment.getStartPosition(), declarationFragment.getLength()));
		buff.append(";");
		buff.append(StubUtility.getLineDelimiterUsed(field));
		return buff.toString();
	}

	private static String getSourceOfDeleteEdit(IJavaElement elem, ICompilationUnit cu, CompilationUnit cuNode) throws JavaModelException, CoreException {
		ASTRewrite rewrite= new ASTRewrite(cuNode);
		try {
			ASTNodeDeleteUtil.markAsDeleted(new IJavaElement[] {elem}, cuNode, rewrite);
			TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
			TextEdit resultingEdits= new MultiTextEdit();
			rewrite.rewriteNode(textBuffer, resultingEdits);
			TextChange change= new CompilationUnitChange("", cu);
			change.addTextEdit("", resultingEdits);
			String content= textBuffer.getContent(resultingEdits.getTextRange().getOffset(), resultingEdits.getTextRange().getLength());
			return Strings.trimIndentation(content, CodeFormatterUtil.getTabWidth(), false);
		} finally{
			rewrite.removeModifications();
		}
	}
}
