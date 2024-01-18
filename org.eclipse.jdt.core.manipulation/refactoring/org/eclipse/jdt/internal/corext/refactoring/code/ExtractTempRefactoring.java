/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [extract local] Extract to local variable not replacing multiple occurrences in same statement - https://bugs.eclipse.org/406347
 *     Nicolaj Hoess <nicohoess@gmail.com> - [extract local] puts declaration at wrong position - https://bugs.eclipse.org/65875
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [inline] Allow inlining of local variable initialized to null. - https://bugs.eclipse.org/93850
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Extract to local variable may result in NullPointerException. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Improve the Safety of Extract Local Variable Refactorings concering ClassCasts. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/331
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Improve the Safety of Extract Local Variable Refactorings by Identifying the Side Effect of Selected Expression. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/348
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Improve the Safety of Extract Local Variable Refactorings by identifying statements that may change the value of the extracted expressions - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/432
 *     Taiming Wang <3120205503@bit.edu.cn> - [extract local] Automated Name Recommendation For The Extract Local Variable Refactoring. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/601
 *     Taiming Wang <3120205503@bit.edu.cn> - [extract local] Context-based Automated Name Recommendation For The Extract Local Variable Refactoring. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/655
 *     Taiming Wang <3120205503@bit.edu.cn> - [extract local] Extract Similar Expression in All Methods If End-Users Want. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/785
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.CopySourceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditVisitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractLocalDescriptor;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroupCore;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.ChangedValueChecker;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.SideEffectChecker;
import org.eclipse.jdt.internal.corext.refactoring.util.UnsafeCheckTester;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.util.Progress;
/**
 * Extract Local Variable (from selected expression inside method or initializer).
 */
public class ExtractTempRefactoring extends Refactoring {

	private static final String ATTRIBUTE_REPLACE= "replace"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REPLACE_ALL= "replaceAllInThisFile"; //$NON-NLS-1$

	private static final String ATTRIBUTE_FINAL= "final"; //$NON-NLS-1$

	private static final String ATTRIBUTE_TYPE_VAR= "varType"; //$NON-NLS-1$

	private static final class ForStatementChecker extends ASTVisitor {

		private final Collection<IVariableBinding> fForInitializerVariables;

		private boolean fReferringToForVariable= false;

		public ForStatementChecker(Collection<IVariableBinding> forInitializerVariables) {
			Assert.isNotNull(forInitializerVariables);
			fForInitializerVariables= forInitializerVariables;
		}

		public boolean isReferringToForVariable() {
			return fReferringToForVariable;
		}

		@Override
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding != null && fForInitializerVariables.contains(binding)) {
				fReferringToForVariable= true;
			}
			return false;
		}
	}

	private static final class LocalVariableWithIdenticalExpressionFinder extends ASTVisitor {
		private ArrayList<String> usedNames;

		private ASTNode expression;

		private Boolean ifStopOnFirstMatch;


		public LocalVariableWithIdenticalExpressionFinder(ASTNode expression, Boolean ifStopOnFirstMatch) {
			usedNames= new ArrayList<>();
			this.expression= expression;
			this.ifStopOnFirstMatch= ifStopOnFirstMatch;
		}

		public String getUsedName() {
			return this.usedNames.get(0);
		}

		public ArrayList<String> getUsedNames() {
			return this.usedNames;
		}

		public void setUsedName(String usedName) {
			this.usedNames.add(usedName);
		}

		@Override
		public boolean visit(VariableDeclarationStatement node) {
			for (Object obj : node.fragments()) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment)obj;
				if (fragment.getInitializer() != null) {
					Expression initializer= fragment.getInitializer();
					if (initializer.subtreeMatch(new ASTMatcher(), this.expression)) {
						setUsedName(fragment.getName().toString());
						if (this.ifStopOnFirstMatch.booleanValue())
							throw new AbortSearchException();
					}
				}
			}
			return true;
		}
	}

	private static final class IdenticalExpressionFinder extends GenericVisitor {
		private ArrayList<ASTNode> identicalFragments;

		private ASTNode expression;

		private HashMap<ASTNode, String> enclosingKeyMap;

		public IdenticalExpressionFinder(ASTNode expression) {
			this.identicalFragments= new ArrayList<>();
			this.enclosingKeyMap= new HashMap<>();
			this.expression= expression;
		}

		public ASTNode[] getIdenticalFragments() {
			return this.identicalFragments.toArray(new ASTNode[identicalFragments.size()]);
		}

		public HashMap<ASTNode, String> getEclosingKeyMap() {
			return enclosingKeyMap;
		}


		@Override
		public boolean visitNode(ASTNode node) {
			if (node.subtreeMatch(new ASTMatcher(), this.expression)) {
				this.identicalFragments.add(node);
				MethodDeclaration methodDeclaration= ASTNodes.getFirstAncestorOrNull(node, MethodDeclaration.class);
				this.enclosingKeyMap.put(node, methodDeclaration.resolveBinding().getMethodDeclaration().getKey());
			}

			return true;
		}
	}

	private static boolean allArraysEqual(ASTNode[][] arrays, int position) {
		Object element= arrays[0][position];
		for (ASTNode[] a : arrays) {
			Object[] array= a;
			if (!element.equals(array[position]))
				return false;
		}
		return true;
	}


	private static boolean canReplace(IASTFragment fragment) {
		ASTNode node= fragment.getAssociatedNode();
		ASTNode parent= node.getParent();
		if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) parent;
			if (node.equals(vdf.getName()))
				return false;
		}
		if (isMethodParameter(node))
			return false;
		if (isThrowableInCatchBlock(node))
			return false;
		if (parent instanceof ExpressionStatement)
			return false;
		if (parent instanceof LambdaExpression)
			return false;
		if (isLeftValue(node))
			return false;
		if (isReferringToLocalVariableFromFor((Expression) node))
			return false;
		if (isUsedInForInitializerOrUpdater((Expression) node))
			return false;
		if (parent instanceof SuperConstructorInvocation)
			return false;
		if (parent instanceof ConstructorInvocation)
			return false;
		if (parent instanceof SwitchCase)
			return true;
		if (node instanceof SimpleName && node.getLocationInParent() != null) {
			return !"name".equals(node.getLocationInParent().getId()); //$NON-NLS-1$
		}
		return true;
	}

	private static ASTNode[] getArrayPrefix(ASTNode[] array, int prefixLength) {
		Assert.isTrue(prefixLength <= array.length);
		Assert.isTrue(prefixLength >= 0);
		ASTNode[] prefix= new ASTNode[prefixLength];
		System.arraycopy(array, 0, prefix, 0, prefix.length);
		return prefix;
	}

	// return List<IVariableBinding>
	private static List<IVariableBinding> getForInitializedVariables(VariableDeclarationExpression variableDeclarations) {
		List<IVariableBinding> forInitializerVariables= new ArrayList<>(1);
		for (Iterator<VariableDeclarationFragment> iter= variableDeclarations.fragments().iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= iter.next();
			IVariableBinding binding= fragment.resolveBinding();
			if (binding != null)
				forInitializerVariables.add(binding);
		}
		return forInitializerVariables;
	}

	private static ASTNode[] getLongestArrayPrefix(ASTNode[][] arrays) {
		int length= -1;
		if (arrays.length == 0)
			return new ASTNode[0];
		int minArrayLength= arrays[0].length;
		for (int i= 1; i < arrays.length; i++)
			minArrayLength= Math.min(minArrayLength, arrays[i].length);

		for (int i= 0; i < minArrayLength; i++) {
			if (!allArraysEqual(arrays, i))
				break;
			length++;
		}
		if (length == -1)
			return new ASTNode[0];
		return getArrayPrefix(arrays[0], length + 1);
	}

	private static ASTNode[] getParents(ASTNode node) {
		ASTNode current= node;
		List<ASTNode> parents= new ArrayList<>();
		do {
			parents.add(current.getParent());
			current= current.getParent();
		} while (current.getParent() != null);
		Collections.reverse(parents);
		return parents.toArray(new ASTNode[parents.size()]);
	}

	private static boolean isLeftValue(ASTNode node) {
		ASTNode parent= node.getParent();
		if (parent instanceof Assignment) {
			Assignment assignment= (Assignment) parent;
			if (assignment.getLeftHandSide() == node)
				return true;
		}
		if (parent instanceof PostfixExpression)
			return true;
		if (parent instanceof PrefixExpression) {
			PrefixExpression.Operator op= ((PrefixExpression) parent).getOperator();
			if (op.equals(PrefixExpression.Operator.DECREMENT))
				return true;
			if (op.equals(PrefixExpression.Operator.INCREMENT))
				return true;
			return false;
		}
		return false;
	}

	private static boolean isMethodParameter(ASTNode node) {
		return (node instanceof SimpleName) && (node.getParent() instanceof SingleVariableDeclaration) && (node.getParent().getParent() instanceof MethodDeclaration);
	}

	private static boolean isReferringToLocalVariableFromFor(Expression expression) {
		ASTNode current= expression;
		ASTNode parent= current.getParent();

		while (parent != null && !(parent instanceof BodyDeclaration)) {
			if (parent instanceof ForStatement) {
				ForStatement forStmt= (ForStatement) parent;
				if (forStmt.initializers().contains(current) || forStmt.updaters().contains(current) || forStmt.getExpression() == current) {
					List<Expression> initializers= forStmt.initializers();
					if (initializers.size() == 1 && initializers.get(0) instanceof VariableDeclarationExpression) {
						List<IVariableBinding> forInitializerVariables= getForInitializedVariables((VariableDeclarationExpression) initializers.get(0));
						ForStatementChecker checker= new ForStatementChecker(forInitializerVariables);
						expression.accept(checker);
						if (checker.isReferringToForVariable())
							return true;
					}
				}
			}
			current= parent;
			parent= current.getParent();
		}
		return false;
	}

	private static boolean isThrowableInCatchBlock(ASTNode node) {
		return (node instanceof SimpleName) && (node.getParent() instanceof SingleVariableDeclaration) && (node.getParent().getParent() instanceof CatchClause);
	}

	private static boolean isUsedInForInitializerOrUpdater(Expression expression) {
		ASTNode parent= expression.getParent();
		if (parent instanceof ForStatement) {
			ForStatement forStmt= (ForStatement) parent;
			return forStmt.initializers().contains(expression) || forStmt.updaters().contains(expression);
		}
		return false;
	}

	private IASTFragment[] retainOnlyReplacableMatches(IASTFragment[] allMatches) {
		List<IASTFragment> result= new ArrayList<>(allMatches.length);
		for (IASTFragment match : allMatches) {
			if (canReplace(match)) {
				result.add(match);
			}
		}
		Comparator<IASTFragment> comparator= (o1, o2) -> o1.getStartPosition() - o2.getStartPosition();
		result.sort(comparator);
		try {
			boolean flag= false;
			IExpressionFragment selectedFragment= getSelectedExpression();
			ASTNode associatedNode= selectedFragment.getAssociatedNode();
			IExpressionFragment firstExpression= getCertainReplacedExpression(result.toArray(new IASTFragment[result.size()]), 0);
			if (firstExpression.getStartPosition() < selectedFragment.getStartPosition())
				flag= false;
			else
				flag= (associatedNode.getParent() instanceof ExpressionStatement || associatedNode.getParent() instanceof LambdaExpression)
						&& selectedFragment.matches(ASTFragmentFactory.createFragmentForFullSubtree(associatedNode));
			int upper= result.size();
			if (flag) {
				ASTNode parent= associatedNode.getParent();
				StructuralPropertyDescriptor location= parent.getLocationInParent();
				while (parent != null) {
					if (parent instanceof Block || parent instanceof BodyDeclaration || parent instanceof LambdaExpression && ((LambdaExpression) parent).resolveMethodBinding() != null
							|| parent instanceof EnhancedForStatement || parent instanceof WhileStatement
							|| parent instanceof ForStatement || parent instanceof DoStatement) {
						break;
					}
					location= parent.getLocationInParent();
					if (location == IfStatement.ELSE_STATEMENT_PROPERTY || location == IfStatement.THEN_STATEMENT_PROPERTY || location == SwitchStatement.STATEMENTS_PROPERTY) {
						break;
					}
					parent= parent.getParent();
				}
				if (parent == null) {
					return result.toArray(new IASTFragment[result.size()]);
				}
				int offset= parent.getStartPosition() + parent.getLength();
				if (location == SwitchStatement.STATEMENTS_PROPERTY) {
					SwitchStatement ss= (SwitchStatement) parent.getParent();
					Iterator<Object> iterator= ss.statements().iterator();
					int preOffset= -1;
					while (iterator.hasNext()) {
						Object obj= iterator.next();
						if (obj instanceof ASTNode) {
							ASTNode node= (ASTNode) obj;
							if (node instanceof SwitchCase && node.getStartPosition() > offset) {
								break;
							}
							preOffset= node.getStartPosition() + node.getLength();
						}
					}
					if (preOffset > 0)
						offset= preOffset;
				}
				for (int i= 0; i < result.size(); ++i) {
					if (result.get(i).getStartPosition() > offset) {
						upper= i;
						break;
					}
				}
			}
			return result.subList(0, upper).toArray(new IASTFragment[upper]);
		} catch (JavaModelException e) {
			return result.toArray(new IASTFragment[result.size()]);
		}
	}

	private CompilationUnit fCompilationUnitNode;

	private CompilationUnitRewrite fCURewrite;

	private ICompilationUnit fCu;

	private boolean fDeclareFinal;

	private boolean fDeclareVarType;

	private String[] fExcludedVariableNames;

	private boolean fReplaceAllOccurrences;

	private boolean fReplaceAllOccurrencesInThisFile;

	// caches:
	private IExpressionFragment fSelectedExpression;

	private int fSelectionLength;

	private int fSelectionStart;

	private String fTempName;

	private String[] fGuessedTempNames;

	private boolean fCheckResultForCompileProblems;

	private CompilationUnitChange fChange;

	private LinkedProposalModelCore fLinkedProposalModel;

	private static final String KEY_NAME= "name"; //$NON-NLS-1$

	private static final String KEY_TYPE= "type"; //$NON-NLS-1$

	private int fStartPoint;

	private int fEndPoint;

	private HashSet<IASTFragment> fSeen= new HashSet<>();

	private String fEnclosingKey;
	private HashSet<String> fEnclosingKeySet;

	private Map<String,String> fFormatterOptions;

	/**
	 * Creates a new extract temp refactoring
	 *
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart start of selection
	 * @param selectionLength length of selection
	 */
	public ExtractTempRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength) {
		this(unit, selectionStart, selectionLength, null);
	}

	public ExtractTempRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength, Map<String,String> formatterOptions) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= unit;
		fCompilationUnitNode= null;
		fReplaceAllOccurrences= true; // default
		fReplaceAllOccurrencesInThisFile= false; //default
		fDeclareFinal= false; // default
		fDeclareVarType= false; // default
		fTempName= ""; //$NON-NLS-1$

		fLinkedProposalModel= null;
		fCheckResultForCompileProblems= true;

		fStartPoint= -1; // default
		fEndPoint= -1; // default
		fEnclosingKey= null;
		fEnclosingKeySet= new HashSet<>();
		fFormatterOptions = formatterOptions;
	}

	public ExtractTempRefactoring(CompilationUnit astRoot, int selectionStart, int selectionLength) {
		this(astRoot, selectionStart, selectionLength, null);
	}

	public ExtractTempRefactoring(CompilationUnit astRoot, int selectionStart, int selectionLength, Map<String,String> formatterOptions) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(astRoot.getTypeRoot() instanceof ICompilationUnit);

		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= (ICompilationUnit) astRoot.getTypeRoot();
		fCompilationUnitNode= astRoot;

		fReplaceAllOccurrences= true; // default
		fReplaceAllOccurrencesInThisFile= false; //default
		fDeclareFinal= false; // default
		fDeclareVarType= false; // default
		fTempName= ""; //$NON-NLS-1$

		fLinkedProposalModel= null;
		fCheckResultForCompileProblems= true;

		fStartPoint= -1; // default
		fEndPoint= -1; // default
		fEnclosingKey= null;
		fEnclosingKeySet= new HashSet<>();
		fFormatterOptions = formatterOptions;
	}

	public ExtractTempRefactoring(JavaRefactoringArguments arguments, RefactoringStatus status) {
		this((ICompilationUnit) null, 0, 0);

		fStartPoint= -1; // default
		fEndPoint= -1; // default
		fEnclosingKey= null;
		fEnclosingKeySet= new HashSet<>();
		RefactoringStatus initializeStatus= initialize(arguments);
		status.merge(initializeStatus);

	}


	public void setCheckResultForCompileProblems(boolean checkResultForCompileProblems) {
		fCheckResultForCompileProblems= checkResultForCompileProblems;
	}


	public void setLinkedProposalModel(LinkedProposalModelCore linkedProposalModel) {
		fLinkedProposalModel= linkedProposalModel;
	}

	private void addReplaceExpressionWithTemp() throws JavaModelException {
		IASTFragment[] fragmentsToReplace= retainOnlyReplacableMatches(getMatchingFragments());

		if (fragmentsToReplace.length == 0 || fEndPoint == -1) {
			return;
		}
		//TODO: should not have to prune duplicates here...
		ASTRewrite rewrite= fCURewrite.getASTRewrite();
		for (int i= fEndPoint; i >= fStartPoint; --i) {
			IASTFragment fragment= fragmentsToReplace[i];
			if (!fSeen.add(fragment))
				continue;

			SimpleName tempName= fCURewrite.getAST().newSimpleName(fTempName);
			TextEditGroup description= fCURewrite.createGroupDescription(RefactoringCoreMessages.ExtractTempRefactoring_replace);

			fragment.replace(rewrite, tempName, description);
			if (fLinkedProposalModel != null)
				fLinkedProposalModel.getPositionGroup(KEY_NAME, true).addPosition(rewrite.track(tempName), false);
		}
	}

	private RefactoringStatus checkExpression() throws JavaModelException {
		Expression selectedExpression= getSelectedExpression().getAssociatedExpression();
		if (selectedExpression != null) {
			final ASTNode parent= selectedExpression.getParent();
			if (selectedExpression instanceof NullLiteral) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_null_literals);
			} else if (selectedExpression instanceof ArrayInitializer) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_array_initializer);
			} else if (selectedExpression instanceof Assignment) {
				if (parent instanceof Expression && !(parent instanceof ParenthesizedExpression))
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_assignment);
				else
					return null;
			} else if (selectedExpression instanceof SimpleName) {
				if ((((SimpleName) selectedExpression)).isDeclaration())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_names_in_declarations);
				if (parent instanceof QualifiedName && selectedExpression.getLocationInParent() == QualifiedName.NAME_PROPERTY
						|| parent instanceof FieldAccess && selectedExpression.getLocationInParent() == FieldAccess.NAME_PROPERTY)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_select_expression);
			} else if (selectedExpression instanceof VariableDeclarationExpression && parent instanceof TryStatement) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_resource_in_try_with_resources);
			}
		}

		return null;
	}

	// !! Same as in ExtractConstantRefactoring
	private RefactoringStatus checkExpressionFragmentIsRValue() throws JavaModelException {
		switch (Checks.checkExpressionIsRValue(getSelectedExpression().getAssociatedExpression())) {
			case Checks.NOT_RVALUE_MISC:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.ExtractTempRefactoring_select_expression, null, JavaManipulationPlugin.getPluginId(),
						RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, null);
			case Checks.NOT_RVALUE_VOID:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.ExtractTempRefactoring_no_void, null, JavaManipulationPlugin.getPluginId(),
						RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID, null);
			case Checks.IS_RVALUE_GUESSED:
			case Checks.IS_RVALUE:
				return new RefactoringStatus();
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	private ITypeBinding guessBindingForReference(Expression expression) {
		ITypeBinding binding= expression.resolveTypeBinding();
		if (binding == null) {
			binding= ASTResolving.guessBindingForReference(expression);
		}
		return binding;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.ExtractTempRefactoring_checking_preconditions, 4);

			fCURewrite= new CompilationUnitRewrite(null, fCu, fCompilationUnitNode, fFormatterOptions);
			fCURewrite.getASTRewrite().setTargetSourceRangeComputer(new NoCommentSourceRangeComputer());

			RefactoringStatus result= new RefactoringStatus();
			fStartPoint= -1;
			fEndPoint= -1;
			fSeen.clear();
			boolean replaceAll= fReplaceAllOccurrences;

			if (fReplaceAllOccurrences) {
				RefactoringStatus checkSideEffectsInSelectedExpression= checkSideEffectsInSelectedExpression();
				if (checkSideEffectsInSelectedExpression.hasInfo()) {
					fReplaceAllOccurrences= false;
					result.merge(checkSideEffectsInSelectedExpression);
				}
			}

			doCreateChange(Progress.subMonitor(pm, 2));

			fChange= fCURewrite.createChange(RefactoringCoreMessages.ExtractTempRefactoring_change_name, true, Progress.subMonitor(pm, 1));

			fChange.getEdit().accept(new TextEditVisitor() {
				@Override
				public void preVisit(TextEdit edit) {
					TextEdit[] children= edit.getChildren();
					for (TextEdit te : children)
						if (te instanceof CopySourceEdit) {
							CopySourceEdit cse= (CopySourceEdit) te;
							if (cse.getTargetEdit() == null || cse.getTargetEdit().getOffset() == 0) {
								edit.removeChild(te);
							}
						}
					super.preVisit(edit);
				}
			});
			if (Arrays.asList(getExcludedVariableNames()).contains(fTempName))
				result.addWarning(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_another_variable, BasicElementLabels.getJavaElementName(fTempName)));

			result.merge(checkMatchingFragments());
			fChange.setKeepPreviewEdits(false);

			if (fCheckResultForCompileProblems) {
				result.merge(RefactoringAnalyzeUtil.checkNewSource(fChange, fCu, fCompilationUnitNode, pm));
			}
			fReplaceAllOccurrences= replaceAll;
			return result;
		} finally {
			pm.done();
		}
	}

	private final ExtractLocalDescriptor createRefactoringDescriptor() {
		final Map<String, String> arguments= new HashMap<>();
		String project= null;
		IJavaProject javaProject= fCu.getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		final String description= Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_descriptor_description_short, BasicElementLabels.getJavaElementName(fTempName));
		final String expression= ASTNodes.asString(fSelectedExpression.getAssociatedExpression());
		final String header= Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_descriptor_description,
				new String[] { BasicElementLabels.getJavaElementName(fTempName), BasicElementLabels.getJavaCodeString(expression) });
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_name_pattern, BasicElementLabels.getJavaElementName(fTempName)));
		final BodyDeclaration decl= ASTNodes.getParent(fSelectedExpression.getAssociatedExpression(), BodyDeclaration.class);
		if (decl instanceof MethodDeclaration) {
			final IMethodBinding method= ((MethodDeclaration) decl).resolveBinding();
			final String label= method != null
					? BindingLabelProviderCore.getBindingLabel(method, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)
					: BasicElementLabels.getJavaElementName('{' + JavaElementLabelsCore.ELLIPSIS_STRING + '}');
			comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_destination_pattern, label));
		}
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_expression_pattern, BasicElementLabels.getJavaCodeString(expression)));
		if (fReplaceAllOccurrences)
			comment.addSetting(RefactoringCoreMessages.ExtractTempRefactoring_replace_occurrences);
		if (fReplaceAllOccurrencesInThisFile)
			comment.addSetting(RefactoringCoreMessages.ExtractTempRefactoring_replace_occurrences_in_this_file);
		if (fDeclareFinal)
			comment.addSetting(RefactoringCoreMessages.ExtractTempRefactoring_declare_final);
		if (fDeclareVarType)
			comment.addSetting(RefactoringCoreMessages.ExtractTempRefactoring_declare_var_type);
		final ExtractLocalDescriptor descriptor= RefactoringSignatureDescriptorFactory.createExtractLocalDescriptor(project, description, comment.asString(), arguments, RefactoringDescriptor.NONE);
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, JavaRefactoringDescriptorUtil.elementToHandle(project, fCu));
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME, fTempName);
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION, Integer.toString(fSelectionStart) + " " + Integer.toString(fSelectionLength)); //$NON-NLS-1$
		arguments.put(ATTRIBUTE_REPLACE, Boolean.toString(fReplaceAllOccurrences));
		arguments.put(ATTRIBUTE_REPLACE_ALL, Boolean.toString(fReplaceAllOccurrencesInThisFile));
		arguments.put(ATTRIBUTE_FINAL, Boolean.toString(fDeclareFinal));
		arguments.put(ATTRIBUTE_TYPE_VAR, Boolean.toString(fDeclareVarType));
		return descriptor;
	}

	private void doCreateChange(IProgressMonitor pm) {
		try {
			pm.beginTask(RefactoringCoreMessages.ExtractTempRefactoring_checking_preconditions, 1);
			try {
				processSelectedExpression();
				if (replaceAllOccurrences() && replaceAllOccurrencesInThisFile()) {
					processOtherIdenticalExpressions();
				}
			} catch (CoreException exception) {
				JavaManipulationPlugin.log(exception);
			}
		} finally {
			pm.done();
		}
	}

	private void processSelectedExpression() throws CoreException {
		int cnt= 1;
		getSelectedExpression();
		MethodDeclaration methodDeclaration= ASTNodes.getFirstAncestorOrNull(fSelectedExpression.getAssociatedNode(), MethodDeclaration.class);
		if (methodDeclaration != null && methodDeclaration.resolveBinding() != null && methodDeclaration.resolveBinding().getMethodDeclaration() != null) {
			fEnclosingKey= methodDeclaration.resolveBinding().getMethodDeclaration().getKey();
			fEnclosingKeySet.add(fEnclosingKey);
		}
		fSelectionStart= fSelectedExpression.getStartPosition();
		fSelectionLength= fSelectedExpression.getLength();
		IASTFragment[] retainOnlyReplacableMatches= retainOnlyReplacableMatches(getMatchingFragments());
		int tmpFSelectionStart= fSelectionStart;
		int tmpFSelectionLength= fSelectionLength;
		IExpressionFragment tmpFSelectedExpression= fSelectedExpression;
		Collection<String> usedNames= getUsedLocalNames(fSelectedExpression.getAssociatedNode());
		String newName= fTempName;
		if (!replaceAllOccurrences() || shouldReplaceSelectedExpressionWithTempDeclaration()
				|| retainOnlyReplacableMatches.length == 0) {
			createTempDeclaration();
			addReplaceExpressionWithTemp();
			fTempName= newName + ++cnt;
			while (usedNames.contains(fTempName)) {
				fTempName= newName + ++cnt;
			}

		}
		while (replaceAllOccurrences() &&
				retainOnlyReplacableMatches.length > fSeen.size()) {
			fStartPoint= -1;
			fEndPoint= -1;
			boolean flag= false;
			for (int i= 0; i < retainOnlyReplacableMatches.length; ++i) {
				if (!fSeen.contains(retainOnlyReplacableMatches[i])) {
					fSelectionStart= retainOnlyReplacableMatches[i].getStartPosition();
					fSelectionLength= retainOnlyReplacableMatches[i].getLength();
					fSelectedExpression= null;
					getSelectedExpression();
					flag= true;
					break;
				}
			}
			if (flag == false)
				break;
			createTempDeclaration();
			if (fStartPoint != -1 && fEndPoint != -1) {
				addReplaceExpressionWithTemp();
				fTempName= newName + ++cnt;
				while (usedNames.contains(fTempName)) {
					fTempName= newName + ++cnt;
				}
			}
		}
		fSelectionStart= tmpFSelectionStart;
		fSelectionLength= tmpFSelectionLength;
		fSelectedExpression= tmpFSelectedExpression;
		fTempName= newName;

	}

	private void processOtherIdenticalExpressions() throws CoreException {
		ASTNode associatedNode= getSelectedExpression().getAssociatedNode();
		CompilationUnit cuNode= ASTNodes.getFirstAncestorOrNull(associatedNode, CompilationUnit.class);
		IdenticalExpressionFinder identicalExpressionFinder= new IdenticalExpressionFinder(associatedNode);
		cuNode.accept(identicalExpressionFinder);
		ASTNode[] matchedFragments= identicalExpressionFinder.getIdenticalFragments();
		HashMap<ASTNode, String> enclosingKeyMap= identicalExpressionFinder.getEclosingKeyMap();
		int realStartOffset= getRealStartOffset(matchedFragments, enclosingKeyMap);
		int realLength= fSelectionLength;

		for (ASTNode fragment : matchedFragments) {
			fSeen.clear();
			if (!fEnclosingKeySet.contains(enclosingKeyMap.get(fragment))) {
				fSelectedExpression= getSelectedExpression(fragment.getStartPosition() + realStartOffset, realLength);
				processSelectedExpression();
			}
		}
	}

	private int getRealStartOffset(ASTNode[] matchedFragments, HashMap<ASTNode, String> enclosingKeyMap) {
		int realStartOffset= 0;
		for (ASTNode fragment : matchedFragments) {
			if (fragment.getLength() == fSelectionLength) {
				return realStartOffset;
			}
			if (fEnclosingKeySet.contains(enclosingKeyMap.get(fragment))) {
				int startOffset= Math.abs(fSelectionStart - fragment.getStartPosition());
				if (realStartOffset != 0) {
					realStartOffset= Math.min(realStartOffset, startOffset);
				} else {
					realStartOffset= startOffset;
				}
			}
		}
		return realStartOffset;
	}

	/**
	 * Retrieves used names for the block containing a node.
	 *
	 * @param selected the selected node
	 *
	 * @return an array of used variable names to avoid
	 */
	private Collection<String> getUsedLocalNames(ASTNode selected) {
		ASTNode surroundingBlock= selected;
		while ((surroundingBlock= surroundingBlock.getParent()) != null) {
			if (surroundingBlock instanceof Block || surroundingBlock instanceof MethodDeclaration) {
				break;
			}
		}
		if (surroundingBlock == null) {
			return new ArrayList<>();
		}
		Collection<String> localUsedNames= new ScopeAnalyzer((CompilationUnit) selected.getRoot()).getUsedVariableNames(surroundingBlock.getStartPosition(), surroundingBlock.getLength());
		return localUsedNames;
	}

	/**
	 * Retrieves a used name in the whole compilation unit that is assigned with the same
	 * expression.
	 *
	 * @param selected the selected node
	 *
	 * @return a used variable name for recommending new names.
	 */
	private String getUsedNameForIdenticalExpressionInCu(ASTNode selected) {
		ASTNode surroundingBlock= selected;
		surroundingBlock= ASTNodes.getFirstAncestorOrNull(surroundingBlock, CompilationUnit.class);
		if (surroundingBlock == null) {
			return null;
		}
		LocalVariableWithIdenticalExpressionFinder localVariableWithIdenticalExpressionFinder= new LocalVariableWithIdenticalExpressionFinder(selected, Boolean.TRUE);
		String usedNameForIdenticalExpressionInCu= null;
		try {
			surroundingBlock.accept(localVariableWithIdenticalExpressionFinder);
		} catch (AbortSearchException e) {
			usedNameForIdenticalExpressionInCu= localVariableWithIdenticalExpressionFinder.getUsedName();
		}
		return usedNameForIdenticalExpressionInCu;
	}

	/**
	 * Retrieves used names in the method declaration that is assigned with the same expression.
	 *
	 * @param selected the selected node
	 *
	 * @return an array of used variable names for avoiding conflicts.
	 */
	private Collection<String> getUsedNameForIdenticalExpressionInMethod(ASTNode selected) {
		ASTNode surroundingBlock= selected;
		surroundingBlock= ASTNodes.getFirstAncestorOrNull(surroundingBlock, MethodDeclaration.class);
		if (surroundingBlock == null) {
			return new ArrayList<>();
		}
		LocalVariableWithIdenticalExpressionFinder localVariableWithIdenticalExpressionFinder= new LocalVariableWithIdenticalExpressionFinder(selected, Boolean.FALSE);
		surroundingBlock.accept(localVariableWithIdenticalExpressionFinder);
		List<String> UsedNameForIdenticalExpressionInMethod= localVariableWithIdenticalExpressionFinder.getUsedNames();
		return UsedNameForIdenticalExpressionInMethod;
	}


	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 6); //$NON-NLS-1$

			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[] { fCu }), getValidationContext(), pm);
			if (result.hasFatalError())
				return result;

			if (fCompilationUnitNode == null) {
				fCompilationUnitNode= RefactoringASTParser.parseWithASTProvider(fCu, true, Progress.subMonitor(pm, 3));
			} else {
				pm.worked(3);
			}

			result.merge(checkSelection(Progress.subMonitor(pm, 3)));
			if (!result.hasFatalError() && isLiteralNodeSelected())
				fReplaceAllOccurrences= false;
			return result;

		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkMatchingFragments() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (IASTFragment matchingFragment : getMatchingFragments()) {
			ASTNode node= matchingFragment.getAssociatedNode();
			if (isLeftValue(node) && !isReferringToLocalVariableFromFor((Expression) node)) {
				String msg= RefactoringCoreMessages.ExtractTempRefactoring_assigned_to;
				result.addWarning(msg, JavaStatusContext.create(fCu, node));
			}
		}
		return result;
	}

	private RefactoringStatus checkSideEffectsInSelectedExpression() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (replaceAllOccurrences() && retainOnlyReplacableMatches(getMatchingFragments()).length > 1) {
			SideEffectChecker ev= new SideEffectChecker(getSelectedExpression().getAssociatedNode(), fEnclosingKey);
			ASTNode node= getSelectedExpression().getAssociatedNode();
			node.accept(ev);
			if (ev.hasSideEffect()) {
				String msg= RefactoringCoreMessages.ExtractTempRefactoring_side_effcts_in_selected_expression;
				result.addInfo(msg, JavaStatusContext.create(fCu, node));
			}
		}
		return result;
	}

	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 8); //$NON-NLS-1$

			IExpressionFragment selectedExpression= getSelectedExpression();

			if (selectedExpression == null) {
				String message= RefactoringCoreMessages.ExtractTempRefactoring_select_expression;
				return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
			}
			pm.worked(1);

			if (isUsedInExplicitConstructorCall())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_explicit_constructor);
			pm.worked(1);

			ASTNode associatedNode= selectedExpression.getAssociatedNode();
			if (getEnclosingBodyNode() == null || ASTNodes.getParent(associatedNode, Annotation.class) != null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_expr_in_method_or_initializer);
			pm.worked(1);

			if (associatedNode instanceof Name && associatedNode.getParent() instanceof ClassInstanceCreation && associatedNode.getLocationInParent() == ClassInstanceCreation.TYPE_PROPERTY)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_name_in_new);
			pm.worked(1);

			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkExpression());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			result.merge(checkExpressionFragmentIsRValue());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			if (isUsedInForInitializerOrUpdater(getSelectedExpression().getAssociatedExpression()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_for_initializer_updater);
			pm.worked(1);

			if (isReferringToLocalVariableFromFor(getSelectedExpression().getAssociatedExpression()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_refers_to_for_variable);
			pm.worked(1);

			return result;
		} finally {
			pm.done();
		}
	}

	public RefactoringStatus checkTempName(String newName) {
		RefactoringStatus status= Checks.checkTempName(newName, fCu);
		if (Arrays.asList(getExcludedVariableNames()).contains(newName))
			status.addWarning(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_another_variable, BasicElementLabels.getJavaElementName(newName)));
		return status;
	}

	private boolean hasFinalModifer(List<IExtendedModifier> modifiers) {
		for (IExtendedModifier modifier : modifiers) {
			if (modifier.isModifier() && Modifier.isFinal(((Modifier) modifier).getKeyword().toFlagValue())) {
				return true;
			}
		}
		return false;
	}

	private void createAndInsertTempDeclaration() throws CoreException {
		Expression initializer= getSelectedExpression().createCopyTarget(fCURewrite.getASTRewrite(), true);
		VariableDeclarationStatement vds= createTempDeclaration(initializer);

		boolean insertAtSelection;
		if (!fReplaceAllOccurrences) {
			insertAtSelection= true;
		} else {
			IASTFragment[] replacableMatches= retainOnlyReplacableMatches(getMatchingFragments());
			insertAtSelection= replacableMatches.length == 0
					|| replacableMatches.length == 1 && replacableMatches[0].getAssociatedNode().equals(getSelectedExpression().getAssociatedExpression());
		}
		ASTNode node= ASTResolving.findParentStatement(getSelectedExpression().getAssociatedNode());
		if (node instanceof SwitchCase) {
			/* VariableDeclarationStatement must be final for switch/case */

			if (!hasFinalModifer(vds.modifiers())) {
				vds.modifiers().add(vds.getAST().newModifier(ModifierKeyword.FINAL_KEYWORD));
			}
			node= ASTNodes.getParent(node, SwitchStatement.class);
			fDeclareFinal= true;
		}

		IASTFragment[] retainOnlyReplacableMatches= retainOnlyReplacableMatches(getMatchingFragments());
		if (retainOnlyReplacableMatches == null) {
			return;
		}

		int selectNumber= -1;

		for (int i= 0; i < retainOnlyReplacableMatches.length; ++i) {
			if (fSelectionStart == retainOnlyReplacableMatches[i].getStartPosition()) {
				selectNumber= i;
				break;
			}
		}
		if (node instanceof SwitchStatement || node instanceof EnhancedForStatement) {
			/* must insert above switch statement */
			fStartPoint= 0;
			fEndPoint= retainOnlyReplacableMatches.length - 1;
			insertAt(node, vds);
			return;
		} else {
			if (insertAtSelection) {
				ASTNode realCommonASTNode= null;
				realCommonASTNode= evalStartAndEnd(retainOnlyReplacableMatches, selectNumber, null);
				if (realCommonASTNode == null && selectNumber >= 0) {
					fSeen.add(retainOnlyReplacableMatches[selectNumber]);
				}
				if (realCommonASTNode != null || retainOnlyReplacableMatches.length == 0) {
					insertAt(getSelectedExpression().getAssociatedNode(), vds);
				}
				return;
			}
		}
		ASTNode realCommonASTNode= null;
		realCommonASTNode= evalStartAndEnd(retainOnlyReplacableMatches, selectNumber, null);
		if (realCommonASTNode == null && selectNumber >= 0) {
			fSeen.add(retainOnlyReplacableMatches[selectNumber]);
		}
		if (realCommonASTNode != null) {
			insertAt(realCommonASTNode, vds);
		}
		return;
	}

	private ASTNode evalStartAndEnd(IASTFragment[] retainOnlyReplacableMatches, int selectNumber, Integer fixedStartOffset) throws JavaModelException {
		ASTNode realCommonASTNode= null;
		if (selectNumber < 0 || selectNumber >= retainOnlyReplacableMatches.length) {
			return realCommonASTNode;
		}
		ASTNode firstReplaceExpression;
		int start= selectNumber;
		int end= selectNumber;
		int expandFlag= 2; // 2:backward 1:forward 0:break

		ChangedValueChecker cvc= new ChangedValueChecker(getSelectedExpression().getAssociatedNode(), fEnclosingKey);

		while (expandFlag > 0 && start <= end) {
			IASTFragment iASTFragment= retainOnlyReplacableMatches[start];
			firstReplaceExpression= getCertainReplacedExpression(retainOnlyReplacableMatches, start).getAssociatedNode();
			ASTNode[] firstReplaceNodeParents= getParents(firstReplaceExpression);
			// fix (1+i)+1+i
			ASTNode[] commonPath= start == end ? firstReplaceNodeParents : findDeepestCommonSuperNodePathForReplacedNodes(start, end);

			Assert.isTrue(commonPath.length <= firstReplaceNodeParents.length);
			ASTNode deepestCommonParent= firstReplaceNodeParents[commonPath.length - 1];
			int startOffset;
			ASTNode expression= iASTFragment.getAssociatedNode();
			int endOffset= expression.getStartPosition();
			ASTNode commonASTNode;
			if (deepestCommonParent instanceof Block) {
				commonASTNode= firstReplaceNodeParents[commonPath.length];
			} else {
				commonASTNode= deepestCommonParent;
			}
			commonASTNode= convertToExtractNode(commonASTNode);
			boolean flag= false;
			startOffset= fixedStartOffset != null ? fixedStartOffset : commonASTNode.getStartPosition() - 1;
			int lastExprOffset= retainOnlyReplacableMatches[end].getStartPosition();

			UnsafeCheckTester uct= new UnsafeCheckTester(fCompilationUnitNode, fCu, commonASTNode, expression, startOffset, endOffset);

			ArrayList<IASTFragment> candidateList= new ArrayList<>();
			for (int i= start; i <= end; ++i)
				candidateList.add(retainOnlyReplacableMatches[i]);
			cvc.detectConflict(startOffset, lastExprOffset, retainOnlyReplacableMatches[end].getAssociatedNode(), deepestCommonParent, candidateList);
			flag= !uct.hasUnsafeCheck() && !cvc.hasConflict();
			if (flag) {//at least one be extracted
				fStartPoint= start;
				fEndPoint= end;
				realCommonASTNode= commonASTNode;
				if (expandFlag == 2 && (end == retainOnlyReplacableMatches.length - 1
						|| fSeen.contains(retainOnlyReplacableMatches[end + 1]))) {
					expandFlag= 1;
				}
				if (expandFlag == 1 && (start == 0
						|| fSeen.contains(retainOnlyReplacableMatches[start - 1]))) {
					expandFlag= 0;
				}
				if (expandFlag == 1) {
					start--;
				} else if (expandFlag == 2) {
					end++;
				}
			} else {
				if (expandFlag == 2) {
					expandFlag= 1;
					if (end != selectNumber)//restore
						end--;
					if (start == 0) {
						expandFlag= 0;
					} else {
						start--;
					}
				} else {
					expandFlag= 0;
				}
			}
		}
		return realCommonASTNode;
	}

	private ASTNode convertToExtractNode(ASTNode target) {
		ASTNode parent= target.getParent();
		StructuralPropertyDescriptor locationInParent= target.getLocationInParent();
		while (locationInParent != Block.STATEMENTS_PROPERTY && locationInParent != SwitchStatement.STATEMENTS_PROPERTY) {
			if (locationInParent == IfStatement.THEN_STATEMENT_PROPERTY
					|| locationInParent == IfStatement.ELSE_STATEMENT_PROPERTY
					|| locationInParent == ForStatement.BODY_PROPERTY
					|| locationInParent == EnhancedForStatement.BODY_PROPERTY
					|| locationInParent == DoStatement.BODY_PROPERTY
					|| locationInParent == WhileStatement.BODY_PROPERTY) {
				break;
			} else if (locationInParent == LambdaExpression.BODY_PROPERTY && ((LambdaExpression) parent).getBody() instanceof Expression) {
				break;
			}
			target= parent;
			parent= parent.getParent();
			locationInParent= target.getLocationInParent();
		}
		return target;
	}

	private VariableDeclarationStatement createTempDeclaration(Expression initializer) throws CoreException {
		AST ast= fCURewrite.getAST();

		VariableDeclarationFragment vdf= ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(fTempName));
		vdf.setInitializer(initializer);

		VariableDeclarationStatement vds= ast.newVariableDeclarationStatement(vdf);
		if (fDeclareFinal) {
			if (!hasFinalModifer(vds.modifiers())) {
				vds.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
			}
		}
		vds.setType(createTempType());

		if (fLinkedProposalModel != null) {
			ASTRewrite rewrite= fCURewrite.getASTRewrite();
			LinkedProposalPositionGroupCore nameGroup= fLinkedProposalModel.getPositionGroup(KEY_NAME, true);
			nameGroup.addPosition(rewrite.track(vdf.getName()), true);

			String[] nameSuggestions= guessTempNamesWithContext();
			if (nameSuggestions.length > 0 && !nameSuggestions[0].equals(fTempName)) {
				nameGroup.addProposal(fTempName, nameSuggestions.length + 1);
			}
			for (int i= 0; i < nameSuggestions.length; i++) {
				nameGroup.addProposal(nameSuggestions[i], nameSuggestions.length - i);
			}
		}
		return vds;
	}

	private void insertAt(ASTNode target, Statement declaration) {
		ASTRewrite rewrite= fCURewrite.getASTRewrite();
		TextEditGroup groupDescription= fCURewrite.createGroupDescription(RefactoringCoreMessages.ExtractTempRefactoring_declare_local_variable);
		ASTNode parent= target.getParent();
		StructuralPropertyDescriptor locationInParent= target.getLocationInParent();
		while (locationInParent != Block.STATEMENTS_PROPERTY && locationInParent != SwitchStatement.STATEMENTS_PROPERTY) {
			if (locationInParent == IfStatement.THEN_STATEMENT_PROPERTY
					|| locationInParent == IfStatement.ELSE_STATEMENT_PROPERTY
					|| locationInParent == ForStatement.BODY_PROPERTY
					|| locationInParent == EnhancedForStatement.BODY_PROPERTY
					|| locationInParent == DoStatement.BODY_PROPERTY
					|| locationInParent == WhileStatement.BODY_PROPERTY) {
				// create intermediate block if target was the body property of a control statement:
				Block replacement= rewrite.getAST().newBlock();
				ListRewrite replacementRewrite= rewrite.getListRewrite(replacement, Block.STATEMENTS_PROPERTY);
				replacementRewrite.insertFirst(declaration, null);
				replacementRewrite.insertLast(rewrite.createMoveTarget(target), null);
				rewrite.replace(target, replacement, groupDescription);
				return;
			} else if (locationInParent == LambdaExpression.BODY_PROPERTY && ((LambdaExpression) parent).getBody() instanceof Expression) {
				Block replacement= rewrite.getAST().newBlock();
				ListRewrite replacementRewrite= rewrite.getListRewrite(replacement, Block.STATEMENTS_PROPERTY);
				replacementRewrite.insertFirst(declaration, null);
				ASTNode moveTarget= rewrite.createMoveTarget(target);
				AST ast= rewrite.getAST();
				if (Bindings.isVoidType(((LambdaExpression) parent).resolveMethodBinding().getReturnType())) {
					ExpressionStatement expressionStatement= ast.newExpressionStatement((Expression) moveTarget);
					moveTarget= expressionStatement;
				} else {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression((Expression) moveTarget);
					moveTarget= returnStatement;
				}
				replacementRewrite.insertLast(moveTarget, null);
				rewrite.replace(target, replacement, groupDescription);
				return;
			}
			target= parent;
			parent= parent.getParent();
			locationInParent= target.getLocationInParent();
		}
		ListRewrite listRewrite= rewrite.getListRewrite(parent, (ChildListPropertyDescriptor) locationInParent);
		listRewrite.insertBefore(declaration, target, groupDescription);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.ExtractTempRefactoring_checking_preconditions, 1);

			ExtractLocalDescriptor descriptor= createRefactoringDescriptor();
			fChange.setDescriptor(new RefactoringChangeDescriptor(descriptor));
			return fChange;
		} finally {
			pm.done();
		}
	}

	private void createTempDeclaration() throws CoreException {
		if (shouldReplaceSelectedExpressionWithTempDeclaration())
			replaceSelectedExpressionWithTempDeclaration();
		else
			createAndInsertTempDeclaration();
	}


	public boolean declareFinal() {
		return fDeclareFinal;
	}

	public boolean declareVarType() {
		return fDeclareVarType;
	}

	private ASTNode[] findDeepestCommonSuperNodePathForReplacedNodes(int start, int end) throws JavaModelException {
		ASTNode[] matchNodes= getMatchNodes();
		ASTNode[][] matchingNodesParents= new ASTNode[end + 1 - start][];
		for (int i= start; i < matchNodes.length && i < end + 1; ++i) {
			matchingNodesParents[i - start]= getParents(matchNodes[i]);
		}
		List<ASTNode> l= Arrays.asList(getLongestArrayPrefix(matchingNodesParents));
		return l.toArray(new ASTNode[l.size()]);
	}

	private ASTNode getEnclosingBodyNode() throws JavaModelException {
		ASTNode node= getSelectedExpression().getAssociatedNode();

		// expression must be in a method, lambda or initializer body
		// make sure it is not in method or parameter annotation
		StructuralPropertyDescriptor location= null;
		while (node != null && !(node instanceof BodyDeclaration)) {
			location= node.getLocationInParent();
			node= node.getParent();
			if (node instanceof LambdaExpression) {
				break;
			}
		}
		if (node == null) {
			return null;
		}
		if (location == MethodDeclaration.BODY_PROPERTY || location == Initializer.BODY_PROPERTY
				|| (location == LambdaExpression.BODY_PROPERTY && ((LambdaExpression) node).resolveMethodBinding() != null)) {
			node= (ASTNode) node.getStructuralProperty(location);
			if (node instanceof MethodDeclaration && ((MethodDeclaration) node).resolveBinding() != null
					&& ((MethodDeclaration) node).resolveBinding().getMethodDeclaration() != null) {
				fEnclosingKey= ((MethodDeclaration) node).resolveBinding().getMethodDeclaration().getKey();
			} else if (node instanceof LambdaExpression &&
					((LambdaExpression) node).resolveMethodBinding().getMethodDeclaration() != null) {
				fEnclosingKey= ((LambdaExpression) node).resolveMethodBinding().getMethodDeclaration().getKey();
			}
			return node;
		}
		return null;
	}

	private static boolean excludeVariableName(ASTNode enclosingNode, int modifiers, IBinding binding) {
		if (Modifier.isStatic(modifiers) && !Modifier.isStatic(binding.getModifiers())) {
			VariableDeclaration bindingDeclaration= ASTNodes.findVariableDeclaration((IVariableBinding) binding, enclosingNode.getRoot());
			if (bindingDeclaration != null) {
				ASTNode bindingMethodDeclarationparent= ASTNodes.getParent(bindingDeclaration, ASTNode.METHOD_DECLARATION);
				if (enclosingNode == bindingMethodDeclarationparent) {
					// if variables live in same methods then we exclude the name
					return true;
				}
			}
			return false;
		}
		return true;
	}

	private String[] getExcludedVariableNames() {
		if (fExcludedVariableNames == null) {
			try {
				IBinding[] bindings= new ScopeAnalyzer(fCompilationUnitNode)
						.getDeclarationsInScope(getSelectedExpression().getStartPosition(), ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY);
				ASTNode enclosingNode= getEnclosingBodyNode().getParent();
				List<String> excludedVariableNames= new ArrayList<>();
				for (IBinding binding : bindings) {
					BodyDeclaration bodyDeclaration= ASTNodes.getParent(getEnclosingBodyNode(), BodyDeclaration.class);
					int modifiers= bodyDeclaration.getModifiers();
					modifiers= bodyDeclaration instanceof TypeDeclaration ? modifiers&= ~Modifier.STATIC : modifiers;
					// Bug 100430 - Work around ScopeAnalyzer#getDeclarationsInScope(..) returning out-of-scope elements
					if (excludeVariableName(enclosingNode, modifiers, binding)) {
						excludedVariableNames.add(binding.getName());
					}
				}
				fExcludedVariableNames= excludedVariableNames.toArray(new String[0]);
			} catch (JavaModelException e) {
				fExcludedVariableNames= new String[0];
			}
		}
		return fExcludedVariableNames;
	}

	private IExpressionFragment getCertainReplacedExpression(IASTFragment[] nodesToReplace, int index) throws JavaModelException {
		if (!fReplaceAllOccurrences)
			return getSelectedExpression();
		if (nodesToReplace.length == 0)
			return getSelectedExpression();
		if (index >= nodesToReplace.length) {
			return (IExpressionFragment) nodesToReplace[nodesToReplace.length - 1];
		}
		return (IExpressionFragment) nodesToReplace[index];
	}

	private IASTFragment[] getMatchingFragments() throws JavaModelException {
		if (fReplaceAllOccurrences) {
			IASTFragment[] allMatches= ASTFragmentFactory.createFragmentForFullSubtree(getEnclosingBodyNode()).getSubFragmentsMatching(getSelectedExpression());
			return allMatches;
		} else
			return new IASTFragment[] { getSelectedExpression() };
	}


	private ASTNode[] getMatchNodes() throws JavaModelException {
		IASTFragment[] matches= retainOnlyReplacableMatches(getMatchingFragments());
		ASTNode[] result= new ASTNode[matches.length];
		for (int i= 0; i < matches.length; i++) {
			result[i]= matches[i].getAssociatedNode();
		}

		return result;
	}

	@Override
	public String getName() {
		return RefactoringCoreMessages.ExtractTempRefactoring_name;
	}

	private IExpressionFragment getSelectedExpression(int startOffset, int length) throws JavaModelException {
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(startOffset, length), fCompilationUnitNode, fCu);
		if (selectedFragment instanceof IExpressionFragment && !Checks.isInsideJavadoc(selectedFragment.getAssociatedNode())) {
			fSelectedExpression= (IExpressionFragment)selectedFragment;
		} else if (selectedFragment != null) {
			if (selectedFragment.getAssociatedNode() instanceof ExpressionStatement) {
				ExpressionStatement exprStatement= (ExpressionStatement)selectedFragment.getAssociatedNode();
				Expression expression= exprStatement.getExpression();
				fSelectedExpression= (IExpressionFragment)ASTFragmentFactory.createFragmentForFullSubtree(expression);
			} else if (selectedFragment.getAssociatedNode() instanceof Assignment) {
				Assignment assignment= (Assignment)selectedFragment.getAssociatedNode();
				fSelectedExpression= (IExpressionFragment)ASTFragmentFactory.createFragmentForFullSubtree(assignment);
			}
		}

		if (fSelectedExpression != null && Checks.isEnumCase(fSelectedExpression.getAssociatedExpression().getParent())) {
			fSelectedExpression= null;
		}

		return fSelectedExpression;
	}


	private IExpressionFragment getSelectedExpression() throws JavaModelException {
		if (fSelectedExpression != null)
			return fSelectedExpression;
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(fSelectionStart, fSelectionLength), fCompilationUnitNode, fCu);
		if (selectedFragment instanceof IExpressionFragment && !Checks.isInsideJavadoc(selectedFragment.getAssociatedNode())) {
			fSelectedExpression= (IExpressionFragment)selectedFragment;
		} else if (selectedFragment != null) {
			if (selectedFragment.getAssociatedNode() instanceof ExpressionStatement) {
				ExpressionStatement exprStatement= (ExpressionStatement)selectedFragment.getAssociatedNode();
				Expression expression= exprStatement.getExpression();
				fSelectedExpression= (IExpressionFragment)ASTFragmentFactory.createFragmentForFullSubtree(expression);
			} else if (selectedFragment.getAssociatedNode() instanceof Assignment) {
				Assignment assignment= (Assignment)selectedFragment.getAssociatedNode();
				fSelectedExpression= (IExpressionFragment)ASTFragmentFactory.createFragmentForFullSubtree(assignment);
			}
		}

		if (fSelectedExpression != null && Checks.isEnumCase(fSelectedExpression.getAssociatedExpression().getParent())) {
			fSelectedExpression= null;
		}

		return fSelectedExpression;
	}


	private Type createTempType() throws CoreException {
		Expression expression= getSelectedExpression().getAssociatedExpression();

		Type resultingType= null;
		ITypeBinding typeBinding= expression.resolveTypeBinding();

		ASTRewrite rewrite= fCURewrite.getASTRewrite();
		AST ast= rewrite.getAST();

		if (isVarTypeAllowed() && fDeclareVarType) {
			resultingType= ast.newSimpleType(ast.newSimpleName("var")); //$NON-NLS-1$
		} else if (expression instanceof ClassInstanceCreation && (typeBinding == null || typeBinding.getTypeArguments().length == 0)) {
			resultingType= (Type) rewrite.createCopyTarget(((ClassInstanceCreation) expression).getType());
		} else if (expression instanceof CastExpression) {
			resultingType= (Type) rewrite.createCopyTarget(((CastExpression) expression).getType());
		} else {
			if (typeBinding == null) {
				typeBinding= ASTResolving.guessBindingForReference(expression);
			}
			if (typeBinding != null) {
				typeBinding= Bindings.normalizeForDeclarationUse(typeBinding, ast);
				ImportRewrite importRewrite= fCURewrite.getImportRewrite();
				ImportRewriteContext context= new ContextSensitiveImportRewriteContext(expression, importRewrite);
				resultingType= importRewrite.addImport(typeBinding, ast, context, TypeLocation.LOCAL_VARIABLE);
			} else {
				resultingType= ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
			}
		}
		if (fLinkedProposalModel != null) {
			LinkedProposalPositionGroupCore typeGroup= fLinkedProposalModel.getPositionGroup(KEY_TYPE, true);
			typeGroup.addPosition(rewrite.track(resultingType), false);
			if (typeBinding != null) {
				ITypeBinding[] relaxingTypes= ASTResolving.getNarrowingTypes(ast, typeBinding);
				for (int i= 0; i < relaxingTypes.length; i++) {
					typeGroup.addProposal(relaxingTypes[i], fCURewrite.getCu(), relaxingTypes.length - i);
				}
			}
		}
		return resultingType;
	}

	public String guessTempName() {
		String[] proposals= guessTempNames();
		if (proposals.length == 0)
			return fTempName;
		else
			return proposals[0];
	}

	public String guessTempNameWithContext() {
		String[] proposals= guessTempNamesWithContext();
		if (proposals.length == 0)
			return fTempName;
		else
			return proposals[0];
	}

	/**
	 * @return proposed variable names (may be empty, but not null). The first proposal should be
	 *         used as "best guess" (if it exists).
	 */
	public String[] guessTempNames() {
		if (fGuessedTempNames == null) {
			try {
				Expression expression= getSelectedExpression().getAssociatedExpression();
				if (expression != null) {
					ITypeBinding binding= guessBindingForReference(expression);
					fGuessedTempNames= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, fCu.getJavaProject(), binding, expression, Arrays.asList(getExcludedVariableNames()));
				}
			} catch (JavaModelException e) {
				JavaManipulationPlugin.log(e);
			}
			if (fGuessedTempNames == null)
				fGuessedTempNames= new String[0];
		}
		return fGuessedTempNames;
	}

	/**
	 * @return proposed variable names based on context (may be empty, but not null). The first
	 *         proposal should be used as "best guess" (if it exists).
	 */
	public String[] guessTempNamesWithContext() {
		if (fGuessedTempNames == null) {
			try {
				Expression expression= getSelectedExpression().getAssociatedExpression();
				if (expression != null) {
					ITypeBinding binding= guessBindingForReference(expression);
					String usedNameForIdenticalExpressionInCu= getUsedNameForIdenticalExpressionInCu(fSelectedExpression.getAssociatedNode());
					Collection<String> usedNamesForIdenticalExpressionInMethod= getUsedNameForIdenticalExpressionInMethod(fSelectedExpression.getAssociatedNode());
					fGuessedTempNames= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, fCu.getJavaProject(), binding, expression, Arrays.asList(getExcludedVariableNames()),
							usedNameForIdenticalExpressionInCu, usedNamesForIdenticalExpressionInMethod);
				}
			} catch (JavaModelException e) {
				JavaManipulationPlugin.log(e);
			}
			if (fGuessedTempNames == null)
				fGuessedTempNames= new String[0];
		}
		return fGuessedTempNames;
	}

	private boolean isLiteralNodeSelected() throws JavaModelException {
		IExpressionFragment fragment= getSelectedExpression();
		if (fragment == null)
			return false;
		Expression expression= fragment.getAssociatedExpression();
		if (expression == null)
			return false;

		switch (expression.getNodeType()) {
			case ASTNode.BOOLEAN_LITERAL:
			case ASTNode.CHARACTER_LITERAL:
			case ASTNode.NULL_LITERAL:
			case ASTNode.NUMBER_LITERAL:
				return true;

			default:
				return false;
		}
	}

	private boolean isUsedInExplicitConstructorCall() throws JavaModelException {
		Expression selectedExpression= getSelectedExpression().getAssociatedExpression();
		if (ASTNodes.getParent(selectedExpression, ConstructorInvocation.class) != null)
			return true;
		if (ASTNodes.getParent(selectedExpression, SuperConstructorInvocation.class) != null)
			return true;
		return false;
	}

	public boolean replaceAllOccurrences() {
		return fReplaceAllOccurrences;
	}

	public boolean replaceAllOccurrencesInThisFile() {
		return fReplaceAllOccurrencesInThisFile;
	}

	private void replaceSelectedExpressionWithTempDeclaration() throws CoreException {
		ASTRewrite rewrite= fCURewrite.getASTRewrite();
		Expression selectedExpression= getSelectedExpression().getAssociatedExpression(); // whole expression selected

		evalStartAndEnd(retainOnlyReplacableMatches(getMatchingFragments()), 0, Integer.valueOf(selectedExpression.getStartPosition() + selectedExpression.getLength()));

		Expression initializer= (Expression) rewrite.createMoveTarget(selectedExpression);
		VariableDeclarationStatement tempDeclaration= createTempDeclaration(initializer);
		ASTNode replacement;

		ASTNode parent= selectedExpression.getParent();
		boolean isParentLambda= parent instanceof LambdaExpression;
		AST ast= rewrite.getAST();
		if (isParentLambda) {
			Block blockBody= ast.newBlock();
			blockBody.statements().add(tempDeclaration);
			if (!Bindings.isVoidType(((LambdaExpression) parent).resolveMethodBinding().getReturnType())) {
				List<VariableDeclarationFragment> fragments= tempDeclaration.fragments();
				SimpleName varName= fragments.get(0).getName();
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(ast.newSimpleName(varName.getIdentifier()));
				blockBody.statements().add(returnStatement);
			}
			replacement= blockBody;
		} else if (ASTNodes.isControlStatementBody(parent.getLocationInParent())) {
			Block block= ast.newBlock();
			block.statements().add(tempDeclaration);
			replacement= block;
		} else {
			replacement= tempDeclaration;
		}
		ASTNode replacee= isParentLambda || !ASTNodes.hasSemicolon((ExpressionStatement) parent, fCu) ? selectedExpression : parent;
		rewrite.replace(replacee, replacement, fCURewrite.createGroupDescription(RefactoringCoreMessages.ExtractTempRefactoring_declare_local_variable));
	}

	public void setDeclareFinal(boolean declareFinal) {
		fDeclareFinal= declareFinal;
	}

	public void setDeclareVarType(boolean declareVarType) {
		fDeclareVarType= declareVarType;
	}

	public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
		fReplaceAllOccurrences= replaceAllOccurrences;
	}

	public void setReplaceAllOccurrencesInThisFile(boolean replaceAllOccurrencesInThisFile) {
		fReplaceAllOccurrencesInThisFile= replaceAllOccurrencesInThisFile;
	}


	public void setTempName(String newName) {
		fTempName= newName;
	}

	private boolean shouldReplaceSelectedExpressionWithTempDeclaration() throws JavaModelException {
		IExpressionFragment selectedFragment= getSelectedExpression();
		ASTNode associatedNode= selectedFragment.getAssociatedNode();
		IExpressionFragment firstExpression= getCertainReplacedExpression(retainOnlyReplacableMatches(getMatchingFragments()), 0);
		if (firstExpression.getStartPosition() < selectedFragment.getStartPosition())
			return false;
		return (associatedNode.getParent() instanceof ExpressionStatement || associatedNode.getParent() instanceof LambdaExpression)
				&& selectedFragment.matches(ASTFragmentFactory.createFragmentForFullSubtree(associatedNode));
	}

	private RefactoringStatus initialize(JavaRefactoringArguments arguments) {
		final String selection= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION);
		if (selection != null) {
			int offset= -1;
			int length= -1;
			final StringTokenizer tokenizer= new StringTokenizer(selection);
			if (tokenizer.hasMoreTokens())
				offset= Integer.parseInt(tokenizer.nextToken());
			if (tokenizer.hasMoreTokens())
				length= Integer.parseInt(tokenizer.nextToken());
			if (offset >= 0 && length >= 0) {
				fSelectionStart= offset;
				fSelectionLength= length;
			} else
				return RefactoringStatus.createFatalErrorStatus(
						Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection, JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION }));
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION));
		final String handle= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(arguments.getProject(), handle, false);
			if (element == null || !element.exists() || element.getElementType() != IJavaElement.COMPILATION_UNIT)
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getName(), IJavaRefactorings.EXTRACT_LOCAL_VARIABLE);
			else
				fCu= (ICompilationUnit) element;
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
		final String name= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME);
		if (name != null && !"".equals(name)) //$NON-NLS-1$
			fTempName= name;
		else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME));
		final String replace= arguments.getAttribute(ATTRIBUTE_REPLACE);
		if (replace != null) {
			fReplaceAllOccurrences= Boolean.parseBoolean(replace);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE));
		final String replaceAll= arguments.getAttribute(ATTRIBUTE_REPLACE_ALL);
		if (replaceAll != null) {
			fReplaceAllOccurrencesInThisFile= Boolean.parseBoolean(replaceAll);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE_ALL));
		final String declareFinal= arguments.getAttribute(ATTRIBUTE_FINAL);
		if (declareFinal != null) {
			fDeclareFinal= Boolean.parseBoolean(declareFinal);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FINAL));
		final String declareVarType= arguments.getAttribute(ATTRIBUTE_TYPE_VAR);
		if (declareVarType != null) {
			fDeclareVarType= Boolean.parseBoolean(declareVarType);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_TYPE_VAR));
		return new RefactoringStatus();
	}

	public boolean isVarTypeAllowed() {
		boolean isAllowed= false;
		if (fCompilationUnitNode != null) {
			IJavaElement root= fCompilationUnitNode.getJavaElement();
			if (root != null) {
				IJavaProject javaProject= root.getJavaProject();
				if (javaProject != null && JavaModelUtil.is10OrHigher(javaProject)) {
					isAllowed= true;
				}
			}
		}
		return isAllowed;
	}

}
