/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.fix.LinkedFix.AbstractLinkedFixRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


public class ConvertForLoopOperation extends AbstractLinkedFixRewriteOperation {

	private ForStatement fOldForStatement;
	private EnhancedForStatement fEnhancedForStatement;
	private AST fAst;
	private Name fCollectionName;
	private SingleVariableDeclaration fParameterDeclaration;
	private ITypeBinding fOldCollectionTypeBinding;
	private IBinding fOldCollectionBinding;
	private IBinding fIndexBinding;
	private boolean fCollectionIsMethodCall= false;
	private MethodInvocation fMethodInvocation;
	private final String fParameterName;
	private final ICompilationUnit fCompilationUnit;
	private FieldAccess fFieldAccess;
	private final CompilationUnit fRoot;
	private final boolean fAddBlock;
	private final boolean fRemoveUnnecessaryBlocks;

	/**
	 * Visitor class for finding all references to a certain Name within the
	 * specified scope (e.g. finds all references to a local variable within the
	 * Body of a For loop).
	 */
	private class LocalOccurencesFinder extends ASTVisitor {

		private List fOccurences;
		private ASTNode fScope;
		private IBinding fTempBinding;
		private ITypeBinding fTempTypeBinding;

		/**
		 * @param collectionName The inferred name of the collection to be
		 *        iterated over
		 * @param oldCollectionBinding The binding of the inferred collection
		 * @param oldCollectionTypeBinding The type binding of the inferred
		 *        collection
		 * @param scope The scope of the search (i.e. the body of a For
		 *        Statement
		 */
		public LocalOccurencesFinder(Name collectionName, IBinding oldCollectionBinding, ITypeBinding oldCollectionTypeBinding,
			ASTNode scope) {
			this.fScope= scope;
			fOccurences= new ArrayList();
			fTempBinding= oldCollectionBinding;
			fTempTypeBinding= oldCollectionTypeBinding;
		}

		public LocalOccurencesFinder(Name name, ASTNode scope) {
			this.fScope= scope;
			fOccurences= new ArrayList();
			fTempBinding= name.resolveBinding();
		}

		public LocalOccurencesFinder(IBinding binding, ASTNode scope) {
			this.fScope= scope;
			fOccurences= new ArrayList();
			fTempBinding= binding;
		}

		public void perform() {
			fScope.accept(this);
		}

		public boolean visit(SimpleName node) {
			if (node.getParent() instanceof VariableDeclaration) {
				if (((VariableDeclaration)node.getParent()).getName() == node)
					return true; //don't include declaration
			}
			if (fTempBinding != null && Bindings.equals(fTempBinding, node.resolveBinding())) {
				fOccurences.add(node);
			}
			return true;
		}

		public boolean visit(MethodInvocation methodInvocation) {
			ArrayAccess arrayAccess= (ArrayAccess)ASTNodes.getParent(methodInvocation, ArrayAccess.class);
			if (arrayAccess != null && fTempTypeBinding != null
					&& Bindings.equals(fTempBinding, methodInvocation.resolveMethodBinding())) {
				fOccurences.add(arrayAccess);
				return false;
			}
			return true;
		}

		public List getOccurences() {
			return fOccurences;
		}
	}

	/**
	 * @param forStatement The For statement to be converted
	 * @param root 
	 * @param addBlock 
	 * @param removeUnnecessaryBlocks 
	 */
	public ConvertForLoopOperation(CompilationUnit root, ForStatement forStatement, String parameterName, boolean addBlock, boolean removeUnnecessaryBlocks) {
		fRoot= root;
		fAddBlock= addBlock;
		fRemoveUnnecessaryBlocks= removeUnnecessaryBlocks;
		fCompilationUnit= (ICompilationUnit)root.getJavaElement();
		this.fOldForStatement= forStatement;
		fAst= root.getAST();
		fParameterName= parameterName;
	}

	/**
	 * Check if the OldFor can be converted to Enhanced For. Unless all
	 * preconditions hold true, there is no reason for this QuickAssist to pop
	 * up.
	 *
	 * @return true if all preconditions (arrayCanBeInferred &&
	 *         arrayOrIndexNotAssignedTo indexNotReferencedOutsideInferredArray &&
	 *         onlyOneIndexUsed && additionalTempsNotReferenced) are satisfied
	 */
	public boolean satisfiesPreconditions() {
		return JavaModelUtil.is50OrHigher(fCompilationUnit.getJavaProject())
			&& fOldForStatement.getExpression() != null
			&& arrayCanBeInferred()
			&& typeBindingsAreNotNull()
			&& bodySatifiesPreconditions()
			&& initializersSatisfyPreconditions()
			&& updatersSatifyPreconditions();
	}

	private boolean typeBindingsAreNotNull() {
		fIndexBinding= getIndexBinding();
		return fOldCollectionBinding != null && fOldCollectionTypeBinding != null && fIndexBinding != null;
	}

	private boolean bodySatifiesPreconditions() {
		// checks in a single pass through Loop's body that arrayOrIndexNotAssignedTo
		// and indexNotReferencedOutsideInferredArray
		final List writeAccesses= new ArrayList();
		final boolean isIndexReferenced[]= {false};

		fOldForStatement.getBody().accept(new ASTVisitor() {
			public boolean visit(Assignment assignment) {
				classifyWriteAccess(assignment.getLeftHandSide());
				return true;
			}
			public boolean visit(PostfixExpression node) {
				classifyWriteAccess(node.getOperand());
				return true;
			}
			public boolean visit(PrefixExpression node) {
				classifyWriteAccess(node.getOperand());
				return true;
			}
			public boolean visit(SimpleName name) {
				IBinding binding= name.resolveBinding();
				if (Bindings.equals(fIndexBinding, binding)) {
					ASTNode parent= name.getParent();
					// check if the direct parent is an ArrayAcces
					if (parent instanceof ArrayAccess){
						// even if the Index is referenced within an ArrayAccess
						// it could happen that the Array is not the same as the
						// inferred Array

						// On fixing bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=73890
						// had to treat the case when indexNotReferenced flag does not get overridden
						// by subsequent passes through this loop
						isIndexReferenced[0]= isIndexReferenced[0] || isAccessToADifferentArray((ArrayAccess)parent);
					}
					else {
						//otherwise the Index is referenced outside ArrayAccess
						isIndexReferenced[0]= true;
					}
				}
				return false;
			}
			private void classifyWriteAccess(Expression expression) {
				//check that
				if (expression instanceof ArrayAccess) {
					checkThatArrayIsNotAssigned(writeAccesses, expression);
				} else if (expression instanceof Name) {
					checkThatIndexIsNotAssigned(writeAccesses, expression);
				}
			}
		});
		return writeAccesses.isEmpty() && !isIndexReferenced[0];
	}

	private void checkThatIndexIsNotAssigned(final List writeAccesses, Expression expression) {
		Name name= (Name)expression;
		IBinding binding= name.resolveBinding();
		if (binding == fIndexBinding) {
			writeAccesses.add(name);
		}
	}

	private void checkThatArrayIsNotAssigned(final List writeAccesses, Expression expression) {
		ArrayAccess arrayAccess= (ArrayAccess)expression;
		Expression array= arrayAccess.getArray();
		IBinding binding= null;
		if (array instanceof Name) {
			binding= ((Name)array).resolveBinding();
		} else if (array instanceof FieldAccess) {
			binding= ((FieldAccess)array).resolveFieldBinding();
		} else if (array instanceof SuperFieldAccess) {
			binding= ((SuperFieldAccess)array).resolveFieldBinding();
		}
		if (binding == fOldCollectionBinding)
			writeAccesses.add(arrayAccess);
	}

	private boolean isAccessToADifferentArray(ArrayAccess arrayAccess) {
		Expression expression= arrayAccess.getArray();
		if (expression instanceof Name) {
			return isNameDifferentThanInferredArray((Name)expression);
		} else if (expression instanceof FieldAccess){
			FieldAccess fieldAccess= (FieldAccess)expression;
			return isNameDifferentThanInferredArray(fieldAccess.getName());
		} else if (expression instanceof MethodInvocation){
			//Be more conservative here, just because it's the same name
			//does not imply that it is the same array. The method can
			//return different arrays. It could even be a method call on
			//a different object. https://bugs.eclipse.org/bugs/show_bug.cgi?id=138353
			return true;
		}else {
			return true; //conservative approach: if it doesn't fall within the above cases
						 // I return that it's an access to a different Array (causing the precondition
						 // to fail)
		}
	}

	private boolean isNameDifferentThanInferredArray(Name name) {
		IBinding arrayBinding= name.resolveBinding();
		if (!Bindings.equals(fOldCollectionBinding, arrayBinding)) {
			return true;
		}
		return false;
	}

	private boolean updatersSatifyPreconditions() {
		return onlyOneIndexUsed() && indexNotDecremented();
	}

	private boolean indexNotDecremented() {
		ASTNode updater= (ASTNode)fOldForStatement.updaters().get(0);

		if (updater instanceof PostfixExpression) {
			if ("++".equals(((PostfixExpression)updater).getOperator().toString())) //$NON-NLS-1$
				return true;
		}

		if (updater instanceof PrefixExpression){
			if ("++".equals(((PrefixExpression)updater).getOperator().toString())) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	private boolean initializersSatisfyPreconditions(){
		// Only one pass through Initializers
		// check if startsFromZero and additionalTempsNotReferenced

		final List tempVarsInInitializers= new ArrayList();
		final boolean startsFromZero[] = {false};
		List initializers= fOldForStatement.initializers();

		for (Iterator iter = initializers.iterator(); iter.hasNext();) {
			Expression element = (Expression) iter.next();
			if (!(element instanceof VariableDeclarationExpression))
				return false;
			
			element.accept(new ASTVisitor(){
				public boolean visit(VariableDeclarationFragment declarationFragment){
					Name indexName= declarationFragment.getName();
					tempVarsInInitializers.add(indexName);
					startsFromZero[0]= doesIndexStartFromZero(indexName, declarationFragment);
					return false;
				}
				public boolean visit(Assignment assignment){
					if (assignment.getLeftHandSide() instanceof Name) {
						Name indexName= (Name) assignment.getLeftHandSide();
						tempVarsInInitializers.add(indexName);
						startsFromZero[0]= doesIndexStartFromZero(indexName, assignment);
					}
					return false;
				}
			});
		}

		removeInferredIndexFrom(tempVarsInInitializers);

		return startsFromZero[0] && additionalTempsNotReferenced(tempVarsInInitializers);
	}

	private boolean doesIndexStartFromZero(Name indexName, ASTNode declaringNode) {
		IBinding binding= indexName.resolveBinding();
		if (Bindings.equals(fIndexBinding, binding)){
			Expression initializer = null;
			if (declaringNode instanceof VariableDeclarationFragment){
				initializer= ((VariableDeclarationFragment)declaringNode).getInitializer();
			} else if (declaringNode instanceof Assignment){
				initializer= ((Assignment) declaringNode).getRightHandSide();
			}

			if (initializer instanceof NumberLiteral){
				NumberLiteral number= (NumberLiteral) initializer;
				if (! "0".equals(number.getToken())) { //$NON-NLS-1$
					return false;
				}
			} else {
				//variable or method call, most probably not 0...
				//could also be a constant which is 0, but who would define such a constant?
				return false;
			}
		}
		return true; // we have to return true also for the cases when we test another variable besides
					 // Inferred Index
	}



	private void removeInferredIndexFrom(List localTemps) {
		Name indexName= null;
		for (Iterator iter= localTemps.iterator(); iter.hasNext();) {
			Name name= (Name)iter.next();
			IBinding binding= name.resolveBinding();
			//fIndexBinding has already been initialized via typeBindingsAreNotNull()
			if (Bindings.equals(fIndexBinding, binding)) {
				indexName= name;
				break;
			}
		}
		localTemps.remove(indexName);
	}

	private boolean additionalTempsNotReferenced(List localTemps) {
		for (Iterator iter= localTemps.iterator(); iter.hasNext();) {
			Name name= (Name)iter.next();
			LocalOccurencesFinder finder= new LocalOccurencesFinder(name, fOldForStatement.getBody());
			finder.perform();
			if (!finder.getOccurences().isEmpty())
				return false;
		}
		return true;
	}

	private boolean onlyOneIndexUsed() {
		return fOldForStatement.updaters().size() == 1;
	}

	private boolean arrayCanBeInferred() {
		doInferCollection();
		return (fCollectionName != null)
			&& fOldCollectionTypeBinding != null
			// for now, only iteration over Arrays are handled
			&& (fOldCollectionTypeBinding.isArray());
	}

	private IBinding inferIndexBinding() {
		List initializers= fOldForStatement.initializers();
		if (initializers.size() == 0)
			return null;
		
		Expression expression= (Expression)initializers.get(0);
		if (expression instanceof VariableDeclarationExpression) {
			VariableDeclarationFragment declaration= (VariableDeclarationFragment)((VariableDeclarationExpression)expression)
				.fragments().get(0);
			Name indexName= declaration.getName();
			fIndexBinding= indexName.resolveBinding();
		} else if (expression instanceof Assignment) {
			Assignment assignment= (Assignment)expression;
			Expression lhs= assignment.getLeftHandSide();
			if (lhs instanceof Name) {
				Name indexName= (Name)lhs;
				fIndexBinding= indexName.resolveBinding();
			}
		}
		return fIndexBinding;
	}

	private ITrackedNodePosition doConvert(ASTRewrite rewrite, ImportRewrite importRewrite, TextEditGroup group) throws CoreException {
		doInferCollection();
		doInferElement(importRewrite);
		doFindAndReplaceInBody(rewrite, group);

		AST ast= fOldForStatement.getAST();
		fEnhancedForStatement= ast.newEnhancedForStatement();
		if (fAddBlock && !(fOldForStatement.getBody() instanceof Block)) {
			Statement theBody= (Statement)rewrite.createMoveTarget(fOldForStatement.getBody());
			Block newBody= ast.newBlock();
			ListRewrite listRewrite= rewrite.getListRewrite(newBody, Block.STATEMENTS_PROPERTY);
			listRewrite.insertFirst(theBody, group);
			fEnhancedForStatement.setBody(newBody);
		} else if (fRemoveUnnecessaryBlocks && fOldForStatement.getBody() instanceof Block && ((Block)fOldForStatement.getBody()).statements().size() == 1) {
			Statement moveTarget= (Statement)rewrite.createMoveTarget((Statement)((Block)fOldForStatement.getBody()).statements().get(0));
			fEnhancedForStatement.setBody(moveTarget);
		} else {
			Statement theBody= (Statement)rewrite.createMoveTarget(fOldForStatement.getBody());
			fEnhancedForStatement.setBody(theBody);
		}
		fEnhancedForStatement.setExpression(createExpression(rewrite, ast));
		fEnhancedForStatement.setParameter(fParameterDeclaration);
		PositionGroup pg= getPositionGroup(fParameterName);
		pg.addFirstPosition(rewrite.track(fParameterDeclaration.getName()));

		String name= fParameterDeclaration.getName().getIdentifier();

		List proposals= getProposalsForElement();
		if (!proposals.contains(name))
			proposals.add(0, name);

		for (Iterator iterator= proposals.iterator(); iterator.hasNext();)
			pg.addProposal((String) iterator.next(), null);

		rewrite.replace(fOldForStatement, fEnhancedForStatement, group);
		return null;
	}

	private Expression createExpression(ASTRewrite rewrite, AST ast) {
		if (fCollectionIsMethodCall) {
			MethodInvocation methodCall= (MethodInvocation) rewrite.createMoveTarget(fMethodInvocation);
			return methodCall;
		} else
			if (fFieldAccess != null) {
				return (FieldAccess)rewrite.createMoveTarget(fFieldAccess);
			}
			return fCollectionName;
	}

	private List getProposalsForElement() {
		List list= new ArrayList();
		ICompilationUnit icu= fCompilationUnit;
		IJavaProject javaProject= icu.getJavaProject();
		int dimensions= fOldCollectionTypeBinding.getDimensions() - 1;
		final List used= getUsedVariableNames();
		String type= fOldCollectionTypeBinding.getName();
		if (fOldCollectionTypeBinding.isArray())
			type= fOldCollectionTypeBinding.getElementType().getName();
		String[] proposals= StubUtility.getLocalNameSuggestions(javaProject, type, dimensions, (String[]) used.toArray(new String[used.size()]));
		for (int i= 0; i < proposals.length; i++) {
			list.add(proposals[i]);
		}
		return list;
	}

	private List getUsedVariableNames() {
		CompilationUnit root= (CompilationUnit)fOldForStatement.getRoot();
		IBinding[] varsBefore= (new ScopeAnalyzer(root)).getDeclarationsInScope(fOldForStatement.getStartPosition(),
			ScopeAnalyzer.VARIABLES);
		IBinding[] varsAfter= (new ScopeAnalyzer(root)).getDeclarationsAfter(fOldForStatement.getStartPosition()
			+ fOldForStatement.getLength(), ScopeAnalyzer.VARIABLES);

		List names= new ArrayList();
		for (int i= 0; i < varsBefore.length; i++) {
			names.add(varsBefore[i].getName());
		}
		for (int i= 0; i < varsAfter.length; i++) {
			names.add(varsAfter[i].getName());
		}
		return names;
	}

	private void doFindAndReplaceInBody(ASTRewrite rewrite, TextEditGroup group) {
		LocalOccurencesFinder finder= new LocalOccurencesFinder(fCollectionName, fOldCollectionBinding,
			fOldCollectionTypeBinding, fOldForStatement.getBody());
		finder.perform();
		List occurences= finder.getOccurences();

		// this might be the "ideal" case (exercised in testNiceReduction)
		if (occurences.size() == 1) {
			ASTNode soleOccurence= (ASTNode)occurences.get(0);
			ArrayAccess arrayAccess= soleOccurence instanceof ArrayAccess
				? (ArrayAccess)soleOccurence
				: (ArrayAccess)ASTNodes.getParent(soleOccurence, ArrayAccess.class);
			if (arrayAccess != null) {
				if (arrayAccess.getParent() instanceof VariableDeclarationFragment) {
					replaceSingleVariableDeclaration(rewrite, arrayAccess, group);
					return;
				}
			}
		}

		replaceMultipleOccurences(rewrite, occurences, group);
	}

	private void replaceSingleVariableDeclaration(ASTRewrite rewrite, ArrayAccess arrayAccess, TextEditGroup group) {
		VariableDeclarationFragment declarationFragment= (VariableDeclarationFragment)arrayAccess.getParent();
		VariableDeclarationStatement declarationStatement= (VariableDeclarationStatement)declarationFragment.getParent();

		// if could not infer THE_ELEMENT from infer step, we might
		// be able to infer it from here
		if (fParameterDeclaration == null) {
			fParameterDeclaration= fAst.newSingleVariableDeclaration();
		}

		SimpleName theTempVariable= declarationFragment.getName();

		SimpleName name= fAst.newSimpleName(theTempVariable.getIdentifier());
		Type type= ASTNodeFactory.newType(getAst(), declarationFragment);
		fParameterDeclaration.setName(name);
		fParameterDeclaration.setType(type);
		if (ASTNodes.findModifierNode(Modifier.FINAL, declarationStatement.modifiers()) != null) {
			ModifierRewrite.create(rewrite, fParameterDeclaration).setModifiers(Modifier.FINAL, Modifier.NONE, group);
		}
		

		LocalOccurencesFinder finder2= new LocalOccurencesFinder(theTempVariable.resolveBinding(), fOldForStatement.getBody());
		finder2.perform();
		List occurences2= finder2.getOccurences();

		linkAllReferences(rewrite, occurences2);

		rewrite.replace(declarationStatement, null, group);
		return;
	}

	private void linkAllReferences(ASTRewrite rewrite, List occurences) {
		for (Iterator iter= occurences.iterator(); iter.hasNext();) {
			ASTNode variableRef= (ASTNode)iter.next();
			getPositionGroup(fParameterName).addPosition(rewrite.track(variableRef));
		}
	}

	private void replaceMultipleOccurences(ASTRewrite rewrite, List occurences, TextEditGroup group) {
		for (Iterator iter= occurences.iterator(); iter.hasNext();) {
			ASTNode element= (ASTNode)iter.next();
			ArrayAccess arrayAccess= element instanceof ArrayAccess ? (ArrayAccess)element : (ArrayAccess)ASTNodes.getParent(
				element, ArrayAccess.class);
			if (arrayAccess != null) {
				SimpleName elementReference= fAst.newSimpleName(fParameterDeclaration.getName().getIdentifier());

				rewrite.replace(arrayAccess, elementReference, group);
				getPositionGroup(fParameterName).addPosition(rewrite.track(elementReference));

			}
		}
	}

	private void doInferElement(ImportRewrite importRewrite) throws CoreException {
		if (fCollectionName == null) {
			createDefaultParameter();
		} else {
			if (fOldCollectionTypeBinding.isArray()) {
				final ITypeBinding elementType= fOldCollectionTypeBinding.getElementType();
				fParameterDeclaration= fAst.newSingleVariableDeclaration();
				SimpleName name= fAst.newSimpleName(fParameterName);
				fParameterDeclaration.setName(name);

				Type theType= importType(elementType, fOldForStatement, importRewrite, fRoot);
				if (fOldCollectionTypeBinding.getDimensions() != 1) {
					theType= fAst.newArrayType(theType, fOldCollectionTypeBinding.getDimensions() - 1);
				}
				fParameterDeclaration.setType(theType);
			}
		}
	}

	private void createDefaultParameter() {
		fParameterDeclaration= fAst.newSingleVariableDeclaration();
		SimpleName name= fAst.newSimpleName(fParameterName);
		Type type= fAst.newPrimitiveType(PrimitiveType.INT);
		fParameterDeclaration.setName(name);
		fParameterDeclaration.setType(type);
	}

	// Caches the inferred collection name and its bindings in local fields. These
	// won't change during the whole operation of the QuickFix.
	private void doInferCollection() {
		if (fCollectionName != null)
			return;

		doInferCollectionFromExpression();

		if (fCollectionName == null)
			doInferCollectionFromInitializers();

	}

	private void doInferCollectionFromExpression() {
		Expression stopCondition= fOldForStatement.getExpression();
		if (stopCondition.getNodeType() == ASTNode.INFIX_EXPRESSION) {
			Expression rightOperand= ((InfixExpression)stopCondition).getRightOperand();
			if (rightOperand.getNodeType() == ASTNode.QUALIFIED_NAME) {
				Name qualifier= ((QualifiedName)rightOperand).getQualifier();
				fCollectionName= ASTNodeFactory.newName(fAst,qualifier.getFullyQualifiedName());
				fOldCollectionBinding= qualifier.resolveBinding();
				fOldCollectionTypeBinding= qualifier.resolveTypeBinding();
			} else if (rightOperand.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodCall= (MethodInvocation)rightOperand;
				Expression exp= methodCall.getExpression();
				if (exp instanceof Name) {
					Name collectionName= (Name)exp;
					fOldCollectionBinding= collectionName.resolveBinding();
					fOldCollectionTypeBinding= collectionName.resolveTypeBinding();
					fCollectionName= ASTNodeFactory.newName(fAst,collectionName.getFullyQualifiedName());
				}
			} else if (rightOperand instanceof FieldAccess){
				// this treats the case when the stop condition is a method call or field access
				// which returns an Array on which the "length" field is queried
				FieldAccess fieldAccess= (FieldAccess) rightOperand;
				if ("length".equals(fieldAccess.getName().getIdentifier())) { //$NON-NLS-1$
					if (fieldAccess.getExpression() instanceof MethodInvocation){
						fCollectionIsMethodCall= true;
						MethodInvocation methodCall= (MethodInvocation) fieldAccess.getExpression();
						fMethodInvocation= methodCall;
						fOldCollectionBinding= methodCall.resolveMethodBinding();
						fOldCollectionTypeBinding= methodCall.resolveTypeBinding();
						fCollectionName= ASTNodeFactory.newName(fAst, methodCall.getName().getFullyQualifiedName());
					} else if (fieldAccess.getExpression() instanceof FieldAccess) {
						FieldAccess fieldCall= (FieldAccess)fieldAccess.getExpression();
						fFieldAccess= fieldCall;
						fOldCollectionBinding= fieldCall.resolveFieldBinding();
						fOldCollectionTypeBinding= fieldCall.resolveTypeBinding();
						fCollectionName= ASTNodeFactory.newName(fAst, fieldCall.getName().getFullyQualifiedName());
					}
				}

			}
		}
	}

	private void doInferCollectionFromInitializers() {
		List initializers= fOldForStatement.initializers();
		for (Iterator iter= initializers.iterator(); iter.hasNext();) {
			Object next= iter.next();
			if (next instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression element= (VariableDeclarationExpression)next;
				List declarationFragments= element.fragments();
				for (Iterator iterator= declarationFragments.iterator(); iterator.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)iterator.next();
					Expression initializer= fragment.getInitializer();
					if (initializer != null)
						doInferCollectionFromExpression(initializer);
				}
			} else if (next instanceof Assignment) {
				Assignment assignemnt= (Assignment)next;
				doInferCollectionFromExpression(assignemnt.getRightHandSide());
			}
		}
	}

	/**
	 * @param expression Expression to visit. This helper method is useful
	 *        for the IDIOM when the stop condition is expressed with another
	 *        variable within loop: for (int i=0, max= array.length; i < max;
	 *        i++){}
	 */
	private void doInferCollectionFromExpression(Expression expression) {
		final boolean[] foundMoreThenOneArray= new boolean[1];
		foundMoreThenOneArray[0]= false;
		expression.accept(new ASTVisitor() {
			public boolean visit(QualifiedName qualifiedName) {
				initializeBindings(qualifiedName.getQualifier());
				return false;
			}
			public boolean visit(SimpleName simpleName) {
				initializeBindings(simpleName);
				return false;
			}
			public boolean visit(MethodInvocation methodCall){
				ITypeBinding typeBinding= methodCall.resolveTypeBinding();
				if (typeBinding != null && typeBinding.isArray()){
					fCollectionIsMethodCall= true;
					fMethodInvocation= methodCall;
					fOldCollectionTypeBinding= typeBinding;
					fOldCollectionBinding= methodCall.resolveMethodBinding();
					fCollectionName= ASTNodeFactory.newName(fAst, methodCall.getName().getFullyQualifiedName());
				}
				return false;
			}
			public boolean visit(FieldAccess field) {
				if (initializeBindings(field.getName())) {
					fFieldAccess= field;
				}
				return true;
			}
			private boolean initializeBindings(Name name) {
				ITypeBinding typeBinding= name.resolveTypeBinding();
				if (typeBinding != null && typeBinding.isArray()) {
					fOldCollectionTypeBinding= typeBinding;
					if (fOldCollectionBinding == null) {
						fOldCollectionBinding= name.resolveBinding();
						fCollectionName= ASTNodeFactory.newName(fAst,name.getFullyQualifiedName());
						return true;
					} else {
						if (name.resolveBinding() != fOldCollectionBinding) {
							foundMoreThenOneArray[0]= true;
						}
						return true;
					}
				}
				return false;
			}
		});
		if (foundMoreThenOneArray[0]) {
			fOldCollectionBinding= null;
			fCollectionName= null;
		}
	}

	private AST getAst() {
		return fAst;
	}

	// lazy load. Caches the binding of the For's index in a field since it cannot
	// be change during the whole QuickFix
	private IBinding getIndexBinding(){
		if (fIndexBinding != null)
			return fIndexBinding;
		else return inferIndexBinding();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.LinkedFix.ILinkedFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List, java.util.List)
	 */
	public ITrackedNodePosition rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups, List positionGroups) throws CoreException {
		TextEditGroup group= createTextEditGroup(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description);
		textEditGroups.add(group);
		clearPositionGroups();
		ITrackedNodePosition endPosition= doConvert(cuRewrite.getASTRewrite(), cuRewrite.getImportRewrite(), group);
		positionGroups.addAll(getAllPositionGroups());
		return endPosition;
	}

}
