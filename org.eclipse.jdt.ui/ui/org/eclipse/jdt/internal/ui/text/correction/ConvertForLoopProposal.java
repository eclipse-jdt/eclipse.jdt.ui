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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;


public class ConvertForLoopProposal extends LinkedCorrectionProposal {

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

	private static final String ELEMENT_KEY_REFERENCE= "element"; //$NON-NLS-1$
	

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
	 * @param name message to be displayed in the quick fix popup
	 * @param cu CompilationUnit containing the For Statement
	 * @param forStatement The For statement to be converted
	 * @param relevance
	 * @param image
	 */
	public ConvertForLoopProposal(String name, ICompilationUnit cu, ForStatement forStatement, int relevance, Image image) {
		super(name, cu, null, relevance, image);
		this.fOldForStatement= forStatement;
		fAst= fOldForStatement.getAST();
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
		return is5_0_Source() 
			&& arrayCanBeInferred() 
			&& typeBindingsAreNotNull()
			&& bodySatifiesPreconditions()		
			&& initializersSatisfyPreconditions() 
			&& updatersSatifyPreconditions();
	}

	private boolean is5_0_Source() {
		IJavaProject project= getCompilationUnit().getJavaProject();
		String version= project.getOption("org.eclipse.jdt.core.compiler.compliance", true); //$NON-NLS-1$
		return JavaCore.VERSION_1_5.equals(version);
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
						// had to treat the case when indexNotReferenced flag does not get overriden
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
		if (arrayAccess.getArray() instanceof Name) {
			Name arrayName= (Name)arrayAccess.getArray();
			IBinding binding= arrayName.resolveBinding();
			if (binding == fOldCollectionBinding)
				writeAccesses.add(arrayAccess);
		}
	}

	private boolean isAccessToADifferentArray(ArrayAccess arrayAccess) {
		Expression expression= arrayAccess.getArray();
		if (expression instanceof Name) {
			return isNameDifferentThanInferredArray((Name)expression);
		} else if (expression instanceof FieldAccess){
			FieldAccess fieldAccess= (FieldAccess)expression;
			return isNameDifferentThanInferredArray(fieldAccess.getName());	
		} else if (expression instanceof MethodInvocation){
			MethodInvocation methodCall= (MethodInvocation)expression;
			return isNameDifferentThanInferredArray(methodCall.getName());
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
		return indexNotDecremented() && onlyOneIndexUsed(); 
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

	protected ASTRewrite getRewrite() {
		ASTRewrite rewrite= ASTRewrite.create(fAst);
		doConvert(rewrite);
		return rewrite;
	}

	private void doConvert(ASTRewrite rewrite) {
		doInferCollection();
		doInferElement();
		doFindAndReplaceInBody(rewrite);

		AST ast= fOldForStatement.getAST();
		fEnhancedForStatement= ast.newEnhancedForStatement();
		ASTNode theBody= rewrite.createMoveTarget(fOldForStatement.getBody());
		fEnhancedForStatement.setBody((Statement)theBody);
		fEnhancedForStatement.setExpression(createExpression(rewrite));
		fEnhancedForStatement.setParameter(fParameterDeclaration);
		addLinkedPosition(rewrite.track(fParameterDeclaration.getName()), true, ConvertForLoopProposal.ELEMENT_KEY_REFERENCE);

		addProposalsForElement();

		rewrite.replace(fOldForStatement, fEnhancedForStatement, null);
	}
	
	private Expression createExpression(ASTRewrite rewrite) {
		if (fCollectionIsMethodCall){
			MethodInvocation methodCall= (MethodInvocation) rewrite.createMoveTarget(fMethodInvocation);
			return methodCall;
		}
		else return fCollectionName;
	}

	private String[] addProposalsForElement() {
		ICompilationUnit icu= getCompilationUnit();
		IJavaProject javaProject= icu.getJavaProject();
		int dimensions= fOldCollectionTypeBinding.getDimensions() - 1;
		String[] proposals= StubUtility.getLocalNameSuggestions(javaProject, ConvertForLoopProposal.ELEMENT_KEY_REFERENCE,
			dimensions, getUsedVariableNames());
		for (int i= 0; i < proposals.length; i++) {
			String proposal= proposals[i];
			addLinkedPositionProposal(ConvertForLoopProposal.ELEMENT_KEY_REFERENCE, proposal, null);
		}
		// my hack for the shortname version of the original proposals
		String[] shortProposals= StubUtility.getLocalNameSuggestions(javaProject, ELEMENT_KEY_REFERENCE.substring(0, 1),
			dimensions, getUsedVariableNames());
		for (int i= 0; i < proposals.length; i++) {
			String proposal= shortProposals[i];
			addLinkedPositionProposal(ConvertForLoopProposal.ELEMENT_KEY_REFERENCE, proposal, null);
		}
		return proposals;
	}

	private String[] getUsedVariableNames() {
		CompilationUnit root= (CompilationUnit)fOldForStatement.getRoot();
		IBinding[] varsBefore= (new ScopeAnalyzer(root)).getDeclarationsInScope(fOldForStatement.getStartPosition(),
			ScopeAnalyzer.VARIABLES);
		IBinding[] varsAfter= (new ScopeAnalyzer(root)).getDeclarationsAfter(fOldForStatement.getStartPosition()
			+ fOldForStatement.getLength(), ScopeAnalyzer.VARIABLES);

		String[] names= new String[varsBefore.length + varsAfter.length];
		for (int i= 0; i < varsBefore.length; i++) {
			names[i]= varsBefore[i].getName();
		}
		for (int i= 0; i < varsAfter.length; i++) {
			names[i + varsBefore.length]= varsAfter[i].getName();
		}
		return names;
	}
	
	private void doFindAndReplaceInBody(ASTRewrite rewrite) {
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
					replaceSingleVariableDeclaration(rewrite, arrayAccess);
					return;
				}
			}
		}

		replaceMultipleOccurences(rewrite, occurences);
	}

	private void replaceSingleVariableDeclaration(ASTRewrite rewrite, ArrayAccess arrayAccess) {
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

		LocalOccurencesFinder finder2= new LocalOccurencesFinder(theTempVariable.resolveBinding(), fOldForStatement.getBody());
		finder2.perform();
		List occurences2= finder2.getOccurences();

		linkAllReferences(rewrite, occurences2);

		rewrite.replace(declarationStatement, null, null);
		return;
	}

	private void linkAllReferences(ASTRewrite rewrite, List occurences) {
		for (Iterator iter= occurences.iterator(); iter.hasNext();) {
			ASTNode variableRef= (ASTNode)iter.next();
			addLinkedPosition(rewrite.track(variableRef), false, ELEMENT_KEY_REFERENCE);
		}
	}

	private void replaceMultipleOccurences(ASTRewrite rewrite, List occurences) {
		for (Iterator iter= occurences.iterator(); iter.hasNext();) {
			ASTNode element= (ASTNode)iter.next();
			ArrayAccess arrayAccess= element instanceof ArrayAccess ? (ArrayAccess)element : (ArrayAccess)ASTNodes.getParent(
				element, ArrayAccess.class);
			if (arrayAccess != null) {
				SimpleName elementReference= fAst.newSimpleName(fParameterDeclaration.getName().getIdentifier());

				rewrite.replace(arrayAccess, elementReference, null);
				addLinkedPosition(rewrite.track(elementReference), false, ELEMENT_KEY_REFERENCE);

			}
		}
	}

	private void doInferElement() {
		if (fCollectionName == null) {
			createDefaultParameter();
		} else {
			if (fOldCollectionTypeBinding.isArray()) {
				ITypeBinding elementType= fOldCollectionTypeBinding.getElementType();
				fParameterDeclaration= fAst.newSingleVariableDeclaration();
				SimpleName name= fAst.newSimpleName(ELEMENT_KEY_REFERENCE);
				fParameterDeclaration.setName(name);
				Type theType= null;
				theType= ASTNodeFactory.newType(getAst(), elementType, false);
				if (fOldCollectionTypeBinding.getDimensions() != 1) {
					theType= fAst.newArrayType(theType, fOldCollectionTypeBinding.getDimensions() - 1);
				}
				fParameterDeclaration.setType(theType);
			}
		}
	}

	private void createDefaultParameter() {
		fParameterDeclaration= fAst.newSingleVariableDeclaration();
		SimpleName name= fAst.newSimpleName(ELEMENT_KEY_REFERENCE);
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
				// this treats the case when the stop condition is a method call 
				// which returns an Array on which the "length" field is queried 
				FieldAccess fieldAccess= (FieldAccess) rightOperand;
				if ("length".equals(fieldAccess.getName().getIdentifier())) { //$NON-NLS-1$
					fCollectionIsMethodCall= true;
					if (fieldAccess.getExpression() instanceof MethodInvocation){
						MethodInvocation methodCall= (MethodInvocation) fieldAccess.getExpression();
						fMethodInvocation= methodCall;
						fOldCollectionBinding= methodCall.resolveMethodBinding();
						fOldCollectionTypeBinding= methodCall.resolveTypeBinding();
						fCollectionName= ASTNodeFactory.newName(fAst, methodCall.getName().getFullyQualifiedName());
					}
				}
				
			}
		}
	}

	private void doInferCollectionFromInitializers() {
		List initializers= fOldForStatement.initializers();
		for (Iterator iter= initializers.iterator(); iter.hasNext();) {
			VariableDeclarationExpression element= (VariableDeclarationExpression)iter.next();
			List declarationFragments= element.fragments();
			for (Iterator iterator= declarationFragments.iterator(); iterator.hasNext();) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment)iterator.next();
				doInferCollectionFromFragment(fragment);
			}
		}
	}

	/**
	 * @param fragment VariableDeclarationFragment to visit. This helper method is useful
	 *        for the IDIOM when the stop condition is expressed with another
	 *        variable within loop: for (int i=0, max= array.length; i < max;
	 *        i++){}
	 */
	private void doInferCollectionFromFragment(VariableDeclarationFragment fragment) {
		Expression initializer= fragment.getInitializer();
		initializer.accept(new ASTVisitor() {
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
				if (typeBinding.isArray()){
					fCollectionIsMethodCall= true;
					fMethodInvocation= methodCall;
					fOldCollectionTypeBinding= typeBinding;
					fOldCollectionBinding= methodCall.resolveMethodBinding();
					fCollectionName= ASTNodeFactory.newName(fAst, methodCall.getName().getFullyQualifiedName());
				}
				return false;
			}
			private void initializeBindings(Name name) {
				ITypeBinding typeBinding= name.resolveTypeBinding();
				if (typeBinding != null && typeBinding.isArray()) {
					fOldCollectionTypeBinding= typeBinding;
					fOldCollectionBinding= name.resolveBinding();
					fCollectionName= ASTNodeFactory.newName(fAst,name.getFullyQualifiedName());
				}
			}
		});
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
}
