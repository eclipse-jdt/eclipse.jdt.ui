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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.util.CompilationUnitSorter;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferenceCache;

/**
 * Orders members is a compilation unit. A working copy must be passed.
 */
public class SortMembersOperation implements IWorkspaceRunnable {


	/**
	 * Default comparator for body declarations.
	 */
	public class DefaultJavaElementComparator implements Comparator {

		private Collator fCollator;
		private MembersOrderPreferenceCache fMemberOrderCache;

		public DefaultJavaElementComparator() {
			this.fCollator= Collator.getInstance();
			fMemberOrderCache= JavaPlugin.getDefault().getMemberOrderPreferenceCache();
		}

		private int category(BodyDeclaration bodyDeclaration) {
			switch (bodyDeclaration.getNodeType()) {
				case ASTNode.METHOD_DECLARATION:
					{
						MethodDeclaration method= (MethodDeclaration) bodyDeclaration;
						if (method.isConstructor()) {
							return getMemberCategory(MembersOrderPreferenceCache.CONSTRUCTORS_INDEX);
						}
						int flags= method.getModifiers();
						if (Modifier.isStatic(flags))
							return getMemberCategory(MembersOrderPreferenceCache.STATIC_METHODS_INDEX);
						else
							return getMemberCategory(MembersOrderPreferenceCache.METHOD_INDEX);
					}
				case ASTNode.FIELD_DECLARATION :
					{
						int flags= ((FieldDeclaration) bodyDeclaration).getModifiers();
						if (Modifier.isStatic(flags))
							return getMemberCategory(MembersOrderPreferenceCache.STATIC_FIELDS_INDEX);
						else
							return getMemberCategory(MembersOrderPreferenceCache.FIELDS_INDEX);
					}
				case ASTNode.INITIALIZER :
					{
						int flags= ((Initializer) bodyDeclaration).getModifiers();
						if (Modifier.isStatic(flags))
							return getMemberCategory(MembersOrderPreferenceCache.STATIC_INIT_INDEX);
						else
							return getMemberCategory(MembersOrderPreferenceCache.INIT_INDEX);
					}
				case ASTNode.TYPE_DECLARATION :
					return getMemberCategory(MembersOrderPreferenceCache.TYPE_INDEX);
			}
			return 0; // should never happen
		}

		private int getMemberCategory(int kind) {
			return fMemberOrderCache.getCategoryIndex(kind);
		}

		/**
		 * This comparator follows the contract defined in CompilationUnitSorter.sort.
		 * @see Comparator#compare(java.lang.Object, java.lang.Object)
		 * @see CompilationUnitSorter#sort(org.eclipse.jdt.core.ICompilationUnit, int[], java.util.Comparator, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public int compare(Object e1, Object e2) {
			BodyDeclaration bodyDeclaration1= (BodyDeclaration) e1;
			BodyDeclaration bodyDeclaration2= (BodyDeclaration) e2;
			int cat1= category(bodyDeclaration1);
			int cat2= category(bodyDeclaration2);

			if (cat1 != cat2) {
				return cat1 - cat2;
			}

			switch (bodyDeclaration1.getNodeType()) {
				case ASTNode.METHOD_DECLARATION :
					{
						MethodDeclaration method1= (MethodDeclaration) bodyDeclaration1;
						MethodDeclaration method2= (MethodDeclaration) bodyDeclaration2;

						if (fMemberOrderCache.isSortByVisibility()) {
							int vis= fMemberOrderCache.getVisibilityIndex(method1.getModifiers()) - fMemberOrderCache.getVisibilityIndex(method2.getModifiers());
							if (vis != 0) {
								return vis;
							}
						}
						
						String name1= method1.getName().getIdentifier();
						String name2= method2.getName().getIdentifier();

						// method declarations (constructors) are sorted by name
						int cmp= this.fCollator.compare(name1, name2);
						if (cmp != 0) {
							return cmp;
						}

						// if names equal, sort by parameter types
						List parameters1= method1.parameters();
						List parameters2= method2.parameters();
						int length1= parameters1.size();
						int length2= parameters2.size();

						int len= Math.min(length1, length2);
						for (int i= 0; i < len; i++) {
							SingleVariableDeclaration param1= (SingleVariableDeclaration) parameters1.get(i);
							SingleVariableDeclaration param2= (SingleVariableDeclaration) parameters2.get(i);
							cmp= this.fCollator.compare(buildSignature(param1.getType()), buildSignature(param2.getType()));
							if (cmp != 0) {
								return cmp;
							}
						}
						if (length1 != length2) {
							return length1 - length2;
						}
						// preserve relative order
						int value1= ((Integer) bodyDeclaration1.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
						int value2= ((Integer) bodyDeclaration2.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
						return value1 - value2;
					}
				case ASTNode.FIELD_DECLARATION :
					{
						FieldDeclaration field1= (FieldDeclaration) bodyDeclaration1;
						FieldDeclaration field2= (FieldDeclaration) bodyDeclaration2;

						String name1= ((VariableDeclarationFragment) field1.fragments().get(0)).getName().getIdentifier();
						String name2= ((VariableDeclarationFragment) field2.fragments().get(0)).getName().getIdentifier();

						// field declarations are sorted by name
						int cmp= this.fCollator.compare(name1, name2);
						if (cmp != 0) {
							return cmp;
						}
						// preserve relative order
						int value1= ((Integer) bodyDeclaration1.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
						int value2= ((Integer) bodyDeclaration2.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
						return value1 - value2;
					}
				case ASTNode.INITIALIZER :
					{
						// preserve relative order
						int value1= ((Integer) bodyDeclaration1.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
						int value2= ((Integer) bodyDeclaration2.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
						return value1 - value2;
					}
				case ASTNode.TYPE_DECLARATION :
					{
						TypeDeclaration type1= (TypeDeclaration) bodyDeclaration1;
						TypeDeclaration type2= (TypeDeclaration) bodyDeclaration2;

						String name1= type1.getName().getIdentifier();
						String name2= type2.getName().getIdentifier();

						// typedeclarations are sorted by name
						int cmp= this.fCollator.compare(name1, name2);
						if (cmp != 0) {
							return cmp;
						}

						// preserve relative order
						int value1= ((Integer) bodyDeclaration1.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
						int value2= ((Integer) bodyDeclaration2.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
						return value1 - value2;
					}
			}
			return 0;
		}

		private String buildSignature(Type type) {
			switch (type.getNodeType()) {
				case ASTNode.PRIMITIVE_TYPE :
					PrimitiveType.Code code= ((PrimitiveType) type).getPrimitiveTypeCode();
					return code.toString();
				case ASTNode.ARRAY_TYPE :
					ArrayType arrayType= (ArrayType) type;
					StringBuffer buffer= new StringBuffer();
					buffer.append(buildSignature(arrayType.getElementType()));
					int dimensions= arrayType.getDimensions();
					for (int j= 0; j < dimensions; j++) {
						buffer.append("[]"); //$NON-NLS-1$
					}
					return buffer.toString();
				case ASTNode.SIMPLE_TYPE :
					SimpleType simpleType= (SimpleType) type;
					return buildSignature(simpleType.getName());
			}
			return null; // should never happen
		}

		private String buildSignature(Name name) {
			if (name.isSimpleName()) {
				return ((SimpleName) name).getIdentifier();
			} else {
				QualifiedName qualifiedName= (QualifiedName) name;
				return buildSignature(qualifiedName.getQualifier()) + '.' + buildSignature(qualifiedName.getName());
			}
		}
	}


	private ICompilationUnit fCompilationUnit;
	private int[] fPositions;
	
	/**
	 * Creates the operation.
	 * @param cu The working copy of a compilation unit.
	 * @param positions Positions to track or <code>null</code> if no positions
	 * should be tracked.
	 */
	public SortMembersOperation(ICompilationUnit cu, int[] positions) {
		fCompilationUnit= cu;
		fPositions= positions;
	}

	/**
	 * Runs the operation.
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		CompilationUnitSorter.sort(fCompilationUnit, fPositions, new DefaultJavaElementComparator(), 0, monitor);
	}

}
