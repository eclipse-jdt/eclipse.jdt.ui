/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditUtil;

import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessorUtil;

public class InlineMethodFixCore implements IProposableFix {

	private final String fName;
	private final ICompilationUnit fCompilationUnit;
	private final List<InlineMethodRefactoring> fRefactoringList;
	private final List<MethodInvocation> fMethodInvocations;

	private InlineMethodFixCore(String name, CompilationUnit compilationUnit,
			InlineMethodRefactoring refactoring, MethodInvocation methodInvocation) {
		this.fName= name;
		this.fCompilationUnit= (ICompilationUnit)compilationUnit.getJavaElement();
		this.fRefactoringList= List.of(refactoring);
		this.fMethodInvocations= List.of(methodInvocation);
	}

	private InlineMethodFixCore(String name, CompilationUnit compilationUnit, List<InlineMethodRefactoring> refactorings,
			List<MethodInvocation> methodInvocations) {
		this.fName= name;
		this.fCompilationUnit= (ICompilationUnit)compilationUnit.getJavaElement();
		this.fRefactoringList= refactorings;
		this.fMethodInvocations= methodInvocations;
	}

	public static InlineMethodFixCore create(String name, CompilationUnit compilationUnit, MethodInvocation methodInvocation) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(cu, compilationUnit,
				methodInvocation.getStartPosition(), methodInvocation.getLength());
		try {
			RefactoringStatus status= refactoring.checkAllConditions(new NullProgressMonitor());
			if (!status.isOK()) {
				return null;
			}
		} catch (OperationCanceledException | CoreException e) {
			return null;
		}
		InlineMethodFixCore fix= new InlineMethodFixCore(name, compilationUnit, refactoring, methodInvocation);
		return fix;
	}

	public static InlineMethodFixCore create(String name, CompilationUnit compilationUnit, List<MethodInvocation> methodInvocations) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		List<InlineMethodRefactoring> refactorings= new ArrayList<>();
		for (MethodInvocation methodInvocation : methodInvocations) {
			InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(cu, compilationUnit,
					methodInvocation.getStartPosition(), methodInvocation.getLength());
			try {
				RefactoringStatus status= refactoring.checkAllConditions(new NullProgressMonitor());
				if (!status.isOK()) {
					return null;
				}
			} catch (OperationCanceledException | CoreException e) {
				return null;
			}
			refactorings.add(refactoring);
		}
		return new InlineMethodFixCore(name, compilationUnit, refactorings, methodInvocations);
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		Map<MethodDeclaration, List<MethodInvocation>> callMap= new LinkedHashMap<>();
		final Set<MethodInvocation> needsParentheses= new HashSet<>();
		for (MethodInvocation methodInvocation : fMethodInvocations) {
			MethodDeclaration methodDeclaration= ASTNodes.getFirstAncestorOrNull(methodInvocation, MethodDeclaration.class);
			if (!callMap.containsKey(methodDeclaration)) {
				callMap.put(methodDeclaration, new ArrayList<>());
			}
			List<MethodInvocation> methodInvocationList= callMap.get(methodDeclaration);
			methodInvocationList.add(methodInvocation);
		}
		// pare down the list of method invocations to be when multiple invocations exist in a single method declaration and
		// those invocations will end up bringing in a local variable declaration when inlined
		for (MethodDeclaration methodDeclaration : callMap.keySet()) {
			List<MethodInvocation> methodInvocations= callMap.get(methodDeclaration);
			if (methodInvocations.size() > 1) {
				List<MethodInvocation> methodInvocationsCopy= new ArrayList<>(methodInvocations);
				for (MethodInvocation methodInvocation : methodInvocationsCopy) {
					try {
						if (!usesLocals(methodInvocation)) {
							methodInvocations.remove(methodInvocation);
						}
					} catch (CoreException e) {
						return null;
					}
				}
			} else {
				methodInvocations.clear();
			}
		}
		// now we have set of multiple invocations that will bring in local variable declarations in the same method declaration
		// so we need to check if any calls exist in outer blocks of other calls in which case we need to
		// add parentheses to the outer block call so that there won't be any local variable collisions
		for (MethodDeclaration methodDeclaration : callMap.keySet()) {
			List<MethodInvocation> methodInvocations= callMap.get(methodDeclaration);
			Map<Block, MethodInvocation> blockMap= new LinkedHashMap<>();
			if (methodInvocations.size() > 1) {
				for (int i= 0; i < methodInvocations.size(); ++i) {
					Block block= ASTNodes.getFirstAncestorOrNull(methodInvocations.get(i), Block.class);
					if (block == null) {
						return null;
					}
					if (blockMap.containsKey(block)) {
						needsParentheses.add(blockMap.get(block));
					}
					blockMap.put(block,  methodInvocations.get(i));
				}
				Block[] blockArray= blockMap.keySet().toArray(new Block[0]);
				for (int i= blockArray.length - 1; i >= 0; --i) {
					ASTNode node= blockArray[i];
					while (node instanceof Block) {
						node= ASTNodes.getFirstAncestorOrNull(node, Block.class, BodyDeclaration.class, LambdaExpression.class);
						if (blockMap.containsKey(node)) {
							// only need parentheses if outer block is registered by earlier call
							for (int j= 0; j < i; ++j) {
								if (blockArray[j] == node) {
									needsParentheses.add(blockMap.get(node));
									break;
								}
							}
						}
					}
				}
			}
		}
		// Take all TextEdit changes and form a single conglomerate edit.  For each edit, we need to
		// modify local variable names to prevent collision between multiple edits in the same
		// method declaration.  As well, we need to check for overlaps as multiple TextEdits could
		// require adding new import statements and these will likely overlap.  In the case of overlap,
		// we need to do a special merge so all imports end up in .
		CompositeChange change= (CompositeChange)fRefactoringList.get(0).createChange(progressMonitor);
		CompilationUnitChange compilationUnitChange= new CompilationUnitChange(fName, fCompilationUnit);
		Change[] changes= change.getChildren();
		int index= 1;
		MethodDeclaration lastMethodDeclaration= null;
		TextEdit parentEdit= null;
		if (changes.length == 1 && changes[0] instanceof TextChange textChange) {
			parentEdit= textChange.getEdit();
			if (needsParentheses.contains(fMethodInvocations.get(0))) {
				MethodInvocation methodInvocation= fMethodInvocations.get(0);
				MethodDeclaration methodDeclaration= ASTNodes.getFirstAncestorOrNull(methodInvocation, MethodDeclaration.class);
				parentEdit= modifyEdit(index, parentEdit, methodInvocation);
				lastMethodDeclaration= methodDeclaration;
				if (parentEdit == null) {
					return null;
				}
			}
		} else {
			return null;
		}
		for (int i= 1; i < fRefactoringList.size(); ++i) {
			++index;
			change= (CompositeChange)fRefactoringList.get(i).createChange(progressMonitor);
			changes= change.getChildren();
			if (changes.length == 1 && changes[0] instanceof TextChange textChange2) {
				TextEdit textEdit= textChange2.getEdit();
				if (needsParentheses.contains(fMethodInvocations.get(i))) {
					MethodInvocation methodInvocation= fMethodInvocations.get(i);
					MethodDeclaration methodDeclaration= ASTNodes.getFirstAncestorOrNull(methodInvocation, MethodDeclaration.class);
					if (methodDeclaration != lastMethodDeclaration) {
						index= 1;
					}
					textEdit= modifyEdit(index, textEdit, methodInvocation);
					lastMethodDeclaration= methodDeclaration;
					if (textEdit == null) {
						return null;
					}
				}
				if (TextEditUtil.overlaps(parentEdit, textEdit)) {
					parentEdit= fixAndMerge(parentEdit, textEdit);
					if (parentEdit == null) {
						return null;
					}
				} else {
					parentEdit= TextEditUtil.merge(parentEdit, textEdit);
				}
			} else {
				return null;
			}
		}
		compilationUnitChange.setEdit(parentEdit);
		return compilationUnitChange;
	}

	/**
	 * Modify an inlining edit to change local variable names so we can avoid conflict with other
	 * inlining edits that pick local variable names based on the insertion point in the original
	 * code and do not know about other inlining edits that will occur later.
	 *
	 * @param index - index to use to form new local variable names
	 * @param textEdit - TextEdit to modify
	 * @param methodInvocation - MethodInvocation we are inlining with modified text edit
	 *
	 * @return modified TextEdit to use
	 */
	private TextEdit modifyEdit(int index, TextEdit textEdit, MethodInvocation methodInvocation) {
		CompilationUnit root= (CompilationUnit)methodInvocation.getRoot();
		MethodDeclaration toInline= getMethodDeclaration(methodInvocation);

		Collection<String> usedVariableNames= new ScopeAnalyzer(root).getUsedVariableNames(methodInvocation.getStartPosition(), methodInvocation.getLength());
		Set<String> localNames= new HashSet<>();
		try {
			localNames= getLocalVarNames(toInline);
		} catch (AbortSearchException e) {
			return null;
		}
		Map<String, String> localVarMap= new HashMap<>();
		for (String localName : localNames) {
			String newName= localName;
			int i= 1;
			while (usedVariableNames.contains(newName)) {
				newName= localName + i++;
			}
			// we now have the local variable name that refactoring will choose to rename to
			String modifiedName= newName;
			// now modify it since we have to ensure we don't have a collision with another inlining in
			// the same method declaration
			do {
				if (Character.isDigit(newName.charAt(newName.length() - 1))) {
					newName= newName + "_" + index; //$NON-NLS-1$
				} else {
					newName= newName + index;
				}
			} while (usedVariableNames.contains(newName));
			localVarMap.put(modifiedName, newName);
		}
		if (textEdit instanceof ReplaceEdit replaceEdit) {
			String text=  replaceEdit.getText();
			for (String name : localVarMap.keySet()) {
				if (text.contains(name)) {
					text= text.replaceAll("([\\W]+)" + name + "([\\W]+)", //$NON-NLS-1$ //$NON-NLS-2$
							"$1" + localVarMap.get(name) + "$2"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			textEdit= new ReplaceEdit(replaceEdit.getOffset(), replaceEdit.getLength(), text);
		} else if (textEdit instanceof MultiTextEdit multiTextEdit) {
			MultiTextEdit newEdit= new MultiTextEdit();
			TextEdit[] childEdits= textEdit.getChildren();
			textEdit= newEdit;
			TextEdit parentEdit= newEdit;
			for (TextEdit childEdit : childEdits) {
				if (childEdit instanceof InsertEdit insertEdit) {
					String text= insertEdit.getText();
					for (String name : localVarMap.keySet()) {
						if (text.contains(name)) {
							text= text.replaceAll("([\\W]+)" + name + "([\\W]+)", //$NON-NLS-1$ //$NON-NLS-2$
									"$1" + localVarMap.get(name) + "$2"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					TextEdit newChildEdit= new InsertEdit(insertEdit.getOffset(), text);
					parentEdit.addChild(newChildEdit);
				} else if (childEdit instanceof MultiTextEdit childMultiTextEdit) {
					TextEdit newChildEdit= modifyEdit(index, childEdit, methodInvocation);
					parentEdit.addChild(newChildEdit);
				} else {
					parentEdit.addChild(childEdit.copy());
				}
			}
		}
		return textEdit;
	}

	/**
	 * When we have import statements brought in, there can be an overlap if two such method invocations
	 * are inlined and they bring in an import (e.g. a static class field reference).  So, we must do a sort of
	 * manual merge where we take the imports from the second TextEdit and add them if needed to the first edit.
	 * Once moved, we delete the overlap from the second edit and then do a regular merge as we have
	 * removed the overlap issue.  The method uses recursion when there are MultiTextEdits that have
	 * overlaps with MultiTextEdits.
	 *
	 * @param textEdit1 - first TextEdit we are building
	 * @param textEdit2 - second TextEdit with an overlap
	 * @return new merged TextEdit
	 */
	private TextEdit fixAndMerge(TextEdit textEdit1, TextEdit textEdit2) {
		MultiTextEdit edit1= (MultiTextEdit)textEdit1;
		MultiTextEdit edit2= (MultiTextEdit)textEdit2;

		MultiTextEdit newEdit1= new MultiTextEdit();
		MultiTextEdit parentEdit1= newEdit1;
		MultiTextEdit parentEdit2= edit2;

		TextEdit[] children1= edit1.getChildren();
		TextEdit[] children2= edit2.copy().getChildren();

		int removeCount= 0;
		for (int i= 0; i < children1.length; ++i) {
			TextEdit childEdit1= children1[i];
			boolean foundOverLap= false;
			for (int j= 0; j < children2.length; ++j) {
				TextEdit childEdit2= children2[j];
				if (TextEditUtil.overlaps(childEdit1, childEdit2)) {
					foundOverLap= true;
					if (childEdit1 instanceof InsertEdit insertEdit1 && childEdit2 instanceof InsertEdit insertEdit2) {
						String text1= insertEdit1.getText();
						String text2= insertEdit2.getText();
						if (text2.contains("import") && !text1.contains(text2)) { //$NON-NLS-1$
							text1 += "\n" + text2; //$NON-NLS-1$
						}
						childEdit1= new InsertEdit(insertEdit1.getOffset(), text1);
						parentEdit2.removeChild(i - removeCount++);
					} else if (childEdit1 instanceof DeleteEdit && childEdit2 instanceof DeleteEdit) {
						parentEdit2.removeChild(i - removeCount++);
						childEdit1= childEdit1.copy();
					} else if (childEdit1 instanceof MultiTextEdit && childEdit2 instanceof MultiTextEdit) {
						childEdit1= fixAndMerge(childEdit1, parentEdit2.getChildren()[i - removeCount]);
					} else {
						continue;
					}
					parentEdit1.addChild(childEdit1);
					break;
				}
			}
			if (!foundOverLap) {
				parentEdit1.addChild(childEdit1.copy());
			}
		}

		if (edit2.hasChildren()) {
			if (TextEditUtil.overlaps(newEdit1, edit2)) {
				return null;
			}
			return TextEditUtil.merge(newEdit1, edit2);
		}
		return newEdit1;
	}

	/**
	 * Return whether a given method invocation when inlined will bring in local variable declarations.
	 *
	 * @param methodInvocation - MethodInvocation to check
	 *
	 * @return true if declared method has local variables, false otherwise
	 * @throws CoreException - if an error occurs
	 */
	private boolean usesLocals(MethodInvocation methodInvocation) throws CoreException {
		IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
		if (methodBinding == null) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		IMethod method= (IMethod)methodBinding.getJavaElement();
		if (method == null) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		CompilationUnit sourceCu= (CompilationUnit)methodInvocation.getRoot();
		CompilationUnit cu= QuickAssistProcessorUtil.findCUForMethod(sourceCu, (ICompilationUnit)sourceCu.getJavaElement(), methodBinding);
		if (cu == null) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		MethodDeclaration targetMethodDeclaration= null;
		try {
			targetMethodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, cu);
		} catch (JavaModelException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (targetMethodDeclaration != null) {
			class FindLocalVarUse extends ASTVisitor {
				boolean usesLocals= false;
				public boolean usesLocals() {
					return usesLocals;
				}
				@Override
				public boolean visit(SimpleName node) {
					IBinding binding= node.resolveBinding();
					if (binding instanceof IVariableBinding varBinding) {
						usesLocals= true;
						throw new AbortSearchException();
					}
					return false;
				}
			}
			FindLocalVarUse findLocalVarUse= new FindLocalVarUse();
			try {
				targetMethodDeclaration.accept(findLocalVarUse);
			} catch (AbortSearchException e) {
				// this is ok, ignore
			}
			return findLocalVarUse.usesLocals();
		}
		throw new CoreException(Status.CANCEL_STATUS);
	}

	/**
	 * Get the method declaration that a method invocation is calling.
	 *
	 * @param methodInvocation - MethodInvocation to find declaration for
	 * @return - MethodDeclaration or null if error occurs
	 */
	private MethodDeclaration getMethodDeclaration(MethodInvocation methodInvocation) {
		IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
		if (methodBinding == null) {
			return null;
		}
		IMethod method= (IMethod)methodBinding.getJavaElement();
		if (method == null) {
			return null;
		}
		CompilationUnit sourceCu= (CompilationUnit)methodInvocation.getRoot();
		CompilationUnit cu= QuickAssistProcessorUtil.findCUForMethod(sourceCu, (ICompilationUnit)sourceCu.getJavaElement(), methodBinding);
		if (cu == null) {
			return null;
		}
		try {
			MethodDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, cu);
			return methodDeclaration;
		} catch (JavaModelException e) {
			// fall-through
		}
		return null;
	}

	/**
	 * Return all local variable names for a method declaration.
	 *
	 * @param methodDeclaration - MethodDeclaration to check
	 * @return Set of String containing all local variable names declared
	 * @throws AbortSearchException - if error occurs
	 */
	private Set<String> getLocalVarNames(MethodDeclaration methodDeclaration) throws AbortSearchException {
		final Set<String> localVarNames= new HashSet<>();
		ASTVisitor getLocalNames= new ASTVisitor() {
			@Override
			public boolean visit(SimpleName node) {
				IBinding binding= node.resolveBinding();
				if (binding == null) {
					throw new AbortSearchException();
				}
				if (binding instanceof IVariableBinding varBinding) {
					if (!varBinding.isField() && !varBinding.isParameter() && !varBinding.isRecordComponent() && !varBinding.isEnumConstant()) {
						localVarNames.add(node.getFullyQualifiedName());
					}
				}
				return true;
			}
		};
		methodDeclaration.accept(getLocalNames);
		return localVarNames;
	}

	@Override
	public String getDisplayString() {
		return fName;
	}

	@Override
	public String getAdditionalProposalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IStatus getStatus() {
		// TODO Auto-generated method stub
		return null;
	}


}
