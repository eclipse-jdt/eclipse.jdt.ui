/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.NewImportRewrite;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ConvertForLoopProposal;
import org.eclipse.jdt.internal.ui.text.correction.ConvertIterableLoopProposal;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Fix which introduce new language constructs to pre Java50 code.
 * Requires a compiler level setting of 5.0+
 * Supported:
 * 		Add missing @Override annotation
 * 		Add missing @Deprecated annotation
 * 		Convert for loop to enhanced for loop
 */
public class Java50Fix extends LinkedFix {
	
	private static final String FOR_LOOP_ELEMENT_IDENTIFIER= "element"; //$NON-NLS-1$

	private static class ForLoopConverterGenerator extends GenericVisitor {

		private final List fForConverters;
		private final Hashtable fUsedNames;
		private final CompilationUnit fCompilationUnit;
		
		public ForLoopConverterGenerator(List forConverters, CompilationUnit compilationUnit) {
			fForConverters= forConverters;
			fCompilationUnit= compilationUnit;
			fUsedNames= new Hashtable();
		}
		
		public boolean visit(ForStatement node) {
			List usedVaribles= getUsedVariableNames(node);
			usedVaribles.addAll(fUsedNames.values());
			String[] used= (String[])usedVaribles.toArray(new String[usedVaribles.size()]);

			String identifierName= FOR_LOOP_ELEMENT_IDENTIFIER;
			int count= 0;
			for (int i= 0; i < used.length; i++) {
				if (used[i].equals(identifierName)) {
					identifierName= FOR_LOOP_ELEMENT_IDENTIFIER + count;
					count++;
					i= 0;
				}
			}
			
			ConvertForLoopProposal forAdapter= new ConvertForLoopProposal(fCompilationUnit, node, identifierName);
			if (forAdapter.satisfiesPreconditions()) {
				fForConverters.add(forAdapter);
				fUsedNames.put(node, identifierName);
			} else {
				ConvertIterableLoopProposal iterableAdapter= new ConvertIterableLoopProposal(fCompilationUnit, node, identifierName);
				if (iterableAdapter.isApplicable()) {
					fForConverters.add(iterableAdapter);
					fUsedNames.put(node, identifierName);
				}
			}
			return super.visit(node);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#endVisit(org.eclipse.jdt.core.dom.ForStatement)
		 */
		public void endVisit(ForStatement node) {
			fUsedNames.remove(node);
			super.endVisit(node);
		}

		private List getUsedVariableNames(ASTNode node) {
			CompilationUnit root= (CompilationUnit)node.getRoot();
			IBinding[] varsBefore= (new ScopeAnalyzer(root)).getDeclarationsInScope(node.getStartPosition(),
				ScopeAnalyzer.VARIABLES);
			IBinding[] varsAfter= (new ScopeAnalyzer(root)).getDeclarationsAfter(node.getStartPosition()
				+ node.getLength(), ScopeAnalyzer.VARIABLES);

			List names= new ArrayList();
			for (int i= 0; i < varsBefore.length; i++) {
				names.add(varsBefore[i].getName());
			}
			for (int i= 0; i < varsAfter.length; i++) {
				names.add(varsAfter[i].getName());
			}
			return names;
		}
	}
	
	private static class AnnotationRewriteOperation implements IFixRewriteOperation {
		private final BodyDeclaration fBodyDeclaration;
		private final String[] fAnnotations;

		public AnnotationRewriteOperation(BodyDeclaration bodyDeclaration, String[] annotations) {
			fBodyDeclaration= bodyDeclaration;
			fAnnotations= annotations;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite, org.eclipse.jdt.core.dom.CompilationUnit, java.util.List)
		 */
		public void rewriteAST(ASTRewrite rewrite, NewImportRewrite importRewrite, CompilationUnit compilationUnit, List textEditGroups) throws CoreException {
			addAnnotation(fBodyDeclaration, fAnnotations, compilationUnit.getAST(), rewrite, textEditGroups);
		}
		
		private void addAnnotation(BodyDeclaration declaration, String[] annotationNames, AST ast, ASTRewrite rewrite, List textEditGroups) {
			ListRewrite listRewrite= rewrite.getListRewrite(declaration, declaration.getModifiersProperty());

			for (int i= 0; i < annotationNames.length; i++) {
				Annotation newAnnotation= ast.newMarkerAnnotation();
				newAnnotation.setTypeName(ast.newSimpleName(annotationNames[i]));
				TextEditGroup group= new TextEditGroup(MessageFormat.format(FixMessages.Java50Fix_AddMissingAnnotation_description, new Object[] {annotationNames[i]}));
				textEditGroups.add(group);
				listRewrite.insertFirst(newAnnotation, group);	
			}
		}
	}
	
	public static Java50Fix createAddOverrideAnnotationFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		return createFix(compilationUnit, problem, true, false);
	}
	
	public static Java50Fix createAddDeprectatedAnnotation(CompilationUnit compilationUnit, IProblemLocation problem) {
		return createFix(compilationUnit, problem, false, true);
	}
	
	private static Java50Fix createFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean addOverrideAnnotation, boolean addDepricatedAnnotation) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;
		
		if (!addOverrideAnnotation && !addDepricatedAnnotation)
			return null;
		
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode == null)
			return null;
		
		ASTNode declaringNode= getDeclaringNode(selectedNode);
		if (!(declaringNode instanceof BodyDeclaration)) 
			return null;
		
		List/*<String>*/ annotations= new ArrayList();
		
		String name= addAnnotations(problem, addOverrideAnnotation, addDepricatedAnnotation, annotations);
		
		if (annotations.isEmpty())
			return null;
		
		BodyDeclaration declaration= (BodyDeclaration) declaringNode;
		
		AnnotationRewriteOperation operation= new AnnotationRewriteOperation(declaration, (String[])annotations.toArray(new String[annotations.size()]));
		
		return new Java50Fix(name, compilationUnit, new IFixRewriteOperation[] {operation});
	}
	
	public static Java50Fix createConvertForLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertForLoopProposal loopConverter= new ConvertForLoopProposal(compilationUnit, loop, FOR_LOOP_ELEMENT_IDENTIFIER);
		if (!loopConverter.satisfiesPreconditions())
			return null;
		
		return new Java50Fix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
	}
	
	public static Java50Fix createConvertIterableLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertIterableLoopProposal loopConverter= new ConvertIterableLoopProposal(compilationUnit, loop, FOR_LOOP_ELEMENT_IDENTIFIER);
		if (!loopConverter.isApplicable())
			return null;

		return new Java50Fix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, 
			boolean addOverrideAnnotation, 
			boolean addDeprecatedAnnotation, 
			boolean convertToEnhancedForLoop) {
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;
		
		if (!addOverrideAnnotation && !addDeprecatedAnnotation && !convertToEnhancedForLoop)
			return null;

		List/*<IFixRewriteOperation>*/ operations= new ArrayList();
		
		if (addOverrideAnnotation || addDeprecatedAnnotation) {
			IProblem[] problems= compilationUnit.getProblems();
			for (int i= 0; i < problems.length; i++) {
				IProblemLocation problem= getProblemLocation(problems[i]);
				
				if (isMissingDeprecated(problem) || isMissingOverride(problem)) {				
					
					ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
					if (selectedNode != null) { 
					
						ASTNode declaringNode= getDeclaringNode(selectedNode);
						if (declaringNode instanceof BodyDeclaration) {
						
							List/*<String>*/ annotations= new ArrayList();
							
							addAnnotations(problem, addOverrideAnnotation, addDeprecatedAnnotation, annotations);
							
							if (!annotations.isEmpty()) {
								BodyDeclaration declaration= (BodyDeclaration) declaringNode;
								AnnotationRewriteOperation annotationAdapter= new AnnotationRewriteOperation(declaration, (String[])annotations.toArray(new String[annotations.size()]));
								operations.add(annotationAdapter);
							}
						}
					}
				}
			}
		}
		
		if (convertToEnhancedForLoop) {
			ForLoopConverterGenerator forLoopFinder= new ForLoopConverterGenerator(operations, compilationUnit);
			compilationUnit.accept(forLoopFinder);
		}
		
		if (operations.size() == 0)
			return null;
		
		IFixRewriteOperation[] operationsArray= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new Java50Fix("", compilationUnit, operationsArray); //$NON-NLS-1$
	}

	private static String addAnnotations(IProblemLocation problem, boolean addOverrideAnnotation, boolean addDepricatedAnnotation, List annotations) {
		String name= ""; //$NON-NLS-1$
		if (addOverrideAnnotation && isMissingOverride(problem)) {
			annotations.add("Override"); //$NON-NLS-1$
			name= FixMessages.Java50Fix_AddOverride_description;
		}
		
		if (addDepricatedAnnotation && isMissingDeprecated(problem)) {
			annotations.add("Deprecated"); //$NON-NLS-1$
			name= FixMessages.Java50Fix_AddDeprecated_description;
		}
		
		if (annotations.size() == 2) {
			name= FixMessages.Java50Fix_AddMissingAnnotations_description;
		}
		return name;
	}

	private static boolean isMissingOverride(IProblemLocation problem) {
		return problem.getProblemId() == IProblem.MissingOverrideAnnotation;
	}

	private static boolean isMissingDeprecated(IProblemLocation problem) {
		return problem.getProblemId() == IProblem.FieldMissingDeprecatedAnnotation ||
		problem.getProblemId() == IProblem.MethodMissingDeprecatedAnnotation ||
		problem.getProblemId() == IProblem.TypeMissingDeprecatedAnnotation;
	}
	
	private static ASTNode getDeclaringNode(ASTNode selectedNode) {
		ASTNode declaringNode= null;		
		if (selectedNode instanceof MethodDeclaration) {
			declaringNode= selectedNode;
		} else if (selectedNode instanceof SimpleName) {
			StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
			if (locationInParent == MethodDeclaration.NAME_PROPERTY || locationInParent == TypeDeclaration.NAME_PROPERTY) {
				declaringNode= selectedNode.getParent();
			} else if (locationInParent == VariableDeclarationFragment.NAME_PROPERTY) {
				declaringNode= selectedNode.getParent().getParent();
			}
		}
		return declaringNode;
	}
	
	private static IProblemLocation getProblemLocation(IProblem problem) {
		int offset= problem.getSourceStart();
		int length= problem.getSourceEnd() - offset + 1;
		return new ProblemLocation(offset, length, problem.getID(), problem.getArguments(), problem.isError());
	}
	
	private Java50Fix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewrites) {
		super(name, compilationUnit, fixRewrites);
	}
	
}
