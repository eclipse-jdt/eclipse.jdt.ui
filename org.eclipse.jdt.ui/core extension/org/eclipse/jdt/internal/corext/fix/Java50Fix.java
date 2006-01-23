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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsConstraintCreator;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsConstraintsSolver;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsTCModel;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsUpdate;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsUpdate.CuUpdate;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

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
	
	private static final String OVERRIDE= "Override"; //$NON-NLS-1$
	private static final String DEPRECATED= "Deprecated"; //$NON-NLS-1$
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
			
			ConvertForLoopOperation forConverter= new ConvertForLoopOperation(fCompilationUnit, node, identifierName);
			if (forConverter.satisfiesPreconditions()) {
				fForConverters.add(forConverter);
				fUsedNames.put(node, identifierName);
			} else {
				ConvertIterableLoopOperation iterableConverter= new ConvertIterableLoopOperation(fCompilationUnit, node, identifierName);
				if (iterableConverter.isApplicable()) {
					fForConverters.add(iterableConverter);
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
		private final String fAnnotation;

		public AnnotationRewriteOperation(BodyDeclaration bodyDeclaration, String annotation) {
			fBodyDeclaration= bodyDeclaration;
			fAnnotation= annotation;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			AST ast= cuRewrite.getRoot().getAST();
			ListRewrite listRewrite= cuRewrite.getASTRewrite().getListRewrite(fBodyDeclaration, fBodyDeclaration.getModifiersProperty());
			Annotation newAnnotation= ast.newMarkerAnnotation();
			newAnnotation.setTypeName(ast.newSimpleName(fAnnotation));
			TextEditGroup group= new TextEditGroup(Messages.format(FixMessages.Java50Fix_AddMissingAnnotation_description, new String[] {fAnnotation}));
			textEditGroups.add(group);
			listRewrite.insertFirst(newAnnotation, group);
		}
	}
	
	private static class AddTypeParametersOperation extends AbstractLinkedFixRewriteOperation {

		private static final String HIERARCHY_SEPARATOR= "  "; //$NON-NLS-1$
		
		private static final Hashtable HARD_CODED_PROPOSALS= createHardCodedProposals();
		
		private static Hashtable createHardCodedProposals() {
			Hashtable table= new Hashtable();
			table.put("java.lang.Object", getObject()); //$NON-NLS-1$
			table.put("java.lang.Number", getNumber()); //$NON-NLS-1$
			table.put("java.lang.Iterable", getIterable()); //$NON-NLS-1$
			table.put("java.util.Collection", getCollection()); //$NON-NLS-1$
			table.put("java.util.List", getList()); //$NON-NLS-1$
			table.put("java.util.Set", getSet()); //$NON-NLS-1$
			table.put("java.util.Map", getMap()); //$NON-NLS-1$
			return table;
		}
		
		private static String[] getObject() {
			List result= new ArrayList();
			result.add("java.lang.Number"); //$NON-NLS-1$
				String[] numbers= getNumber();
				for (int i= 0; i < numbers.length; i++) {
					result.add(HIERARCHY_SEPARATOR + numbers[i]);
				}
			result.add("java.util.Map"); //$NON-NLS-1$
				String[] maps= getMap();
				for (int i= 0; i < maps.length; i++) {
					result.add(HIERARCHY_SEPARATOR + maps[i]);
				}
			result.add("java.util.Iterable"); //$NON-NLS-1$
				String[] iterables= getIterable();
				for (int i= 0; i < iterables.length; i++) {
					result.add(HIERARCHY_SEPARATOR + iterables[i]);
				}
			return (String[])result.toArray(new String[result.size()]);
		}
		
		private static String[] getNumber() {
			String[] result= new String[8];
			result[0]= "java.lang.Byte"; //$NON-NLS-1$
			result[1]= "java.lang.Double"; //$NON-NLS-1$
			result[2]= "java.lang.Float"; //$NON-NLS-1$
			result[3]= "java.lang.Integer"; //$NON-NLS-1$
			result[4]= "java.lang.Long"; //$NON-NLS-1$
			result[5]= "java.lang.Short"; //$NON-NLS-1$
			result[6]= "java.math.BigDecimal"; //$NON-NLS-1$
			result[7]= "java.math.BigInteger"; //$NON-NLS-1$
			return result;
		}
		
		private static String[] getIterable() {
			List result= new ArrayList();
			result.add("java.lang.Collection"); //$NON-NLS-1$
				String[] collections= getCollection();
				for (int i= 0; i < collections.length; i++) {
					result.add(HIERARCHY_SEPARATOR + collections[i]);
				}
			return (String[])result.toArray(new String[result.size()]);
		}
		
		private static String[] getCollection() {
			List result= new ArrayList();
			result.add("java.util.List"); //$NON-NLS-1$
				String[] lists= getList();
				for (int i= 0; i < lists.length; i++) {
					result.add(HIERARCHY_SEPARATOR + lists[i]);
				}
			result.add("java.util.Queue"); //$NON-NLS-1$
			result.add("java.util.Set"); //$NON-NLS-1$
				String[] sets= getSet();
				for (int i= 0; i < sets.length; i++) {
					result.add(HIERARCHY_SEPARATOR + sets[i]);
				}
			return (String[])result.toArray(new String[result.size()]);
		}
		
		private static String[] getList() {
			String[] result= new String[4];
			result[0]= "java.util.ArrayList"; //$NON-NLS-1$
			result[1]= "java.util.Collection"; //$NON-NLS-1$
			result[2]= "java.util.LinkedList"; //$NON-NLS-1$
			result[3]= "java.util.Vector"; //$NON-NLS-1$
			return result;
		}
		
		private static String[] getSet() {
			String[] result= new String[3];
			result[0]= "java.util.HashSet"; //$NON-NLS-1$
			result[1]= "java.util.LinkedHashSet"; //$NON-NLS-1$
			result[2]= "java.util.SortedSet"; //$NON-NLS-1$
			return result;
		}
		
		private static String[] getMap() {
			String[] result= new String[4];
			result[0]= "java.util.HashMap"; //$NON-NLS-1$
			result[1]= "java.util.Hashtable"; //$NON-NLS-1$
			result[2]= "java.util.LinkedList"; //$NON-NLS-1$
			result[3]= "java.util.SortedMap"; //$NON-NLS-1$
			return result;
		}
		
		private final SimpleType fType;

		public AddTypeParametersOperation(SimpleType type) {
			fType= type;
		}

		/**
		 * {@inheritDoc}
		 */
		public ITrackedNodePosition rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups, List positionGroups) throws CoreException {
			
			TextEditGroup group= new TextEditGroup(FixMessages.Java50Fix_ParametrizeTypeReference_description);
			textEditGroups.add(group);
			
			if (smartFix(cuRewrite, positionGroups, group))
				return endPosition(cuRewrite.getASTRewrite(), fType);
			
			if (classInstanceCreationFix(cuRewrite, group))
				return endPosition(cuRewrite.getASTRewrite(), fType);
		
			if (dumbFix(cuRewrite, positionGroups, group))
				return endPosition(cuRewrite.getASTRewrite(), fType);
			
			return null;
		}

		private boolean smartFix(CompilationUnitRewrite cuRewrite, List positionGroups, TextEditGroup group) {
			InferTypeArgumentsTCModel tCModel= new InferTypeArgumentsTCModel();
			InferTypeArgumentsConstraintCreator creator= new InferTypeArgumentsConstraintCreator(tCModel, true);
			
			CompilationUnit root= cuRewrite.getRoot();
			root.setProperty(RefactoringASTParser.SOURCE_PROPERTY, cuRewrite.getCu());
			root.accept(creator);
			
			InferTypeArgumentsConstraintsSolver solver= new InferTypeArgumentsConstraintsSolver(tCModel);
			InferTypeArgumentsUpdate iTAUpdate= solver.solveConstraints(new NullProgressMonitor());
			solver= null; //free caches
			
			filter(iTAUpdate, fType, root);
			
			HashMap/*<ICompilationUnit, CuUpdate>*/ updates= iTAUpdate.getUpdates();
			Set entrySet= updates.entrySet();
			
			if (entrySet.size() == 0)
				return false;
			
			boolean hasRewrite= false;
			for (Iterator iter= entrySet.iterator(); iter.hasNext();) {
				
				Map.Entry entry= (Map.Entry) iter.next();

				cuRewrite.setResolveBindings(false);
				CuUpdate cuUpdate= (CuUpdate) entry.getValue();
				
				hasRewrite|= InferTypeArgumentsRefactoring.rewriteDeclarations(cuUpdate, cuRewrite, tCModel, true);
			}
			return hasRewrite;
		}
		
		private void filter(InferTypeArgumentsUpdate update, SimpleType type, CompilationUnit root) {
			Set entrySet= update.getUpdates().entrySet();
			for (Iterator iter= entrySet.iterator(); iter.hasNext();) {
				Map.Entry entry= (Map.Entry) iter.next();
				CuUpdate cuUpdate= (CuUpdate) entry.getValue();
				
				for (Iterator cvIter= cuUpdate.getDeclarations().iterator(); cvIter.hasNext();) {
					ConstraintVariable2 cv= (ConstraintVariable2) cvIter.next();
					
					if (cv instanceof CollectionElementVariable2) {
						ConstraintVariable2 parentElement= ((CollectionElementVariable2) cv).getParentConstraintVariable();
						if (parentElement instanceof TypeVariable2) {
							TypeVariable2 typeCv= (TypeVariable2) parentElement;
							
							ASTNode node= typeCv.getRange().getNode(root);
							if (node instanceof Name && node.getParent() instanceof Type) {
								Type originalType= (Type) node.getParent();
							
								if (originalType != type)
									cvIter.remove();
								
							} else {
								cvIter.remove();
							}
						} else {
							cvIter.remove();
						}
					} else {
						cvIter.remove();
					}
				}
					
				if (cuUpdate.getDeclarations().size() == 0) {
					iter.remove();
				}
			}
		}

		private boolean dumbFix(CompilationUnitRewrite cuRewrite, List positionGroups, TextEditGroup group) {
			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			
			ITypeBinding resolveBinding= fType.resolveBinding();
			if (resolveBinding == null)
				return false;
			
			ITypeBinding binding= resolveBinding.getTypeDeclaration();
			if (binding == null)
				return false;
			
			ITypeBinding[] parameters= binding.getTypeParameters();
			
			Type moveTarget= (Type)rewrite.createMoveTarget(fType);
			ParameterizedType parameterizedType= ast.newParameterizedType(moveTarget);				
			rewrite.replace(fType, parameterizedType, group);

			ListRewrite listRewrite= rewrite.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
			for (int i= 0; i < parameters.length; i++) {
				ITypeBinding parameter= parameters[i];
				ITypeBinding[] typeBounds= parameter.getTypeBounds();
				
				String name;
				SimpleName newName;
				if (typeBounds.length == 1) {
					name= typeBounds[0].getName();
					newName= (SimpleName)rewrite.createStringPlaceholder(name, ASTNode.SIMPLE_NAME);
					cuRewrite.getImportRewrite().addImport(typeBounds[0]);
				} else {
					name= "Object"; //$NON-NLS-1$
					newName= ast.newSimpleName(name);
				}
				listRewrite.insertLast(newName, null);
				
				PositionGroup positionGroup= new PositionGroup(name);
				if (i==0) {
					positionGroup.addFirstPosition(rewrite.track(newName));
				} else {
					positionGroup.addPosition(rewrite.track(newName));
				}
				
				positionGroup.addProposal("?", "?"); //$NON-NLS-1$ //$NON-NLS-2$
				
				String[] proposals;
				if (name.equals("Object")) { //$NON-NLS-1$
					proposals= (String[])HARD_CODED_PROPOSALS.get("java.lang.Object"); //$NON-NLS-1$
				} else {
					String qualifiedName= typeBounds[0].getTypeDeclaration().getErasure().getQualifiedName();
					proposals= (String[])HARD_CODED_PROPOSALS.get(qualifiedName);
				}
				if (proposals != null) {
					for (int j= 0; j < proposals.length; j++) {
						String dispName= proposals[j];
						String instName= Signature.getSimpleName(dispName);
						positionGroup.addProposal(dispName, instName);
					}
				}
				
				positionGroups.add(positionGroup);
			}
			return true;
		}

		private boolean classInstanceCreationFix(CompilationUnitRewrite cuRewrite, TextEditGroup group) {
			ClassInstanceCreation creation= (ClassInstanceCreation)ASTNodes.getParent(fType, ClassInstanceCreation.class);
			if (creation == null)
				return false;
			
			if (!(creation.getType() instanceof SimpleType))
				return false;
			
			if (creation.getLocationInParent() != VariableDeclarationFragment.INITIALIZER_PROPERTY)
				return false;
			
			VariableDeclarationStatement declStmt= (VariableDeclarationStatement)ASTNodes.getParent(creation, VariableDeclarationStatement.class);
			
			if (!(declStmt.getType() instanceof ParameterizedType))
				return false;
			
			injectParameters((ParameterizedType)declStmt.getType(), fType, cuRewrite.getASTRewrite(), cuRewrite.getAST(), group);
			return true;
		}

		private void injectParameters(ParameterizedType parameterizedType, SimpleType simpleType, ASTRewrite rewrite, AST ast, TextEditGroup group) {
			ParameterizedType copy= (ParameterizedType)ASTNode.copySubtree(ast, parameterizedType);
			SimpleType moveSimpleType= (SimpleType)rewrite.createMoveTarget(simpleType);
			
			rewrite.replace(copy.getType(), moveSimpleType, group);
			rewrite.replace(simpleType, copy, group);
		}
		
		private ITrackedNodePosition endPosition(ASTRewrite rewrite, SimpleType type) {
			Statement stmt= (Statement)ASTNodes.getParent(type, Statement.class);
			if (stmt == null)
				return null;
			
			return rewrite.track(stmt);
		}
	}
	
	public static Java50Fix createAddOverrideAnnotationFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() != IProblem.MissingOverrideAnnotation)
			return null;
		
		return createFix(compilationUnit, problem, OVERRIDE, FixMessages.Java50Fix_AddOverride_description);
	}
	
	public static Java50Fix createAddDeprectatedAnnotation(CompilationUnit compilationUnit, IProblemLocation problem) {
		int id= problem.getProblemId();
		if (id != IProblem.FieldMissingDeprecatedAnnotation && 
			id != IProblem.MethodMissingDeprecatedAnnotation && 
			id != IProblem.TypeMissingDeprecatedAnnotation)
			
			return null;
			
		return createFix(compilationUnit, problem, DEPRECATED, FixMessages.Java50Fix_AddDeprecated_description);
	}
	
	private static Java50Fix createFix(CompilationUnit compilationUnit, IProblemLocation problem, String annotation, String label) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;
		
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode == null)
			return null;
		
		ASTNode declaringNode= getDeclaringNode(selectedNode);
		if (!(declaringNode instanceof BodyDeclaration)) 
			return null;
		
		BodyDeclaration declaration= (BodyDeclaration) declaringNode;
		
		AnnotationRewriteOperation operation= new AnnotationRewriteOperation(declaration, annotation);
		
		return new Java50Fix(label, compilationUnit, new IFixRewriteOperation[] {operation});
	}
	
	public static Java50Fix createRawTypeReferenceFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		List operations= new ArrayList();
		createRawTypeReferenceOperations(compilationUnit, new IProblemLocation[] {problem}, operations);
		if (operations.size() == 0)
			return null;
		
		ASTNode node= problem.getCoveredNode(compilationUnit);
		
		if (node instanceof ClassInstanceCreation) {
			node= (ASTNode)node.getStructuralProperty(ClassInstanceCreation.TYPE_PROPERTY);
		} else if (node instanceof SimpleName) {
			node= node.getParent();
		}
		
		return new Java50Fix(Messages.format(FixMessages.Java50Fix_AddTypeParameters_description, ((SimpleType)node).getName()), compilationUnit, (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]));
	}
	
	public static Java50Fix createConvertForLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertForLoopOperation loopConverter= new ConvertForLoopOperation(compilationUnit, loop, FOR_LOOP_ELEMENT_IDENTIFIER);
		if (!loopConverter.satisfiesPreconditions())
			return null;
		
		return new Java50Fix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
	}
	
	public static Java50Fix createConvertIterableLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertIterableLoopOperation loopConverter= new ConvertIterableLoopOperation(compilationUnit, loop, FOR_LOOP_ELEMENT_IDENTIFIER);
		if (!loopConverter.isApplicable())
			return null;

		return new Java50Fix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, 
			boolean addOverrideAnnotation, 
			boolean addDeprecatedAnnotation, 
			boolean convertToEnhancedForLoop, 
			boolean rawTypeReference) {
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;
		
		if (!addOverrideAnnotation && !addDeprecatedAnnotation && !convertToEnhancedForLoop && !rawTypeReference)
			return null;

		List/*<IFixRewriteOperation>*/ operations= new ArrayList();

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}
		
		if (addOverrideAnnotation)
			createAddOverrideAnnotationOperations(compilationUnit, locations, operations);
		
		if (addDeprecatedAnnotation)
			createAddDeprecatedAnnotationOperations(compilationUnit, locations, operations);
		
		if (convertToEnhancedForLoop) {
			ForLoopConverterGenerator forLoopFinder= new ForLoopConverterGenerator(operations, compilationUnit);
			compilationUnit.accept(forLoopFinder);
		}
		
		if (rawTypeReference)
			createRawTypeReferenceOperations(compilationUnit, locations, operations);
		
		if (operations.size() == 0)
			return null;
		
		IFixRewriteOperation[] operationsArray= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new Java50Fix("", compilationUnit, operationsArray); //$NON-NLS-1$
	}

	public static IFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems,
			boolean addOverrideAnnotation, 
			boolean addDeprecatedAnnotation,
			boolean rawTypeReferences) {
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;
		
		if (!addOverrideAnnotation && !addDeprecatedAnnotation && !rawTypeReferences)
			return null;

		List/*<IFixRewriteOperation>*/ operations= new ArrayList();
		
		if (addOverrideAnnotation)
			createAddOverrideAnnotationOperations(compilationUnit, problems, operations);
		
		if (addDeprecatedAnnotation)
			createAddDeprecatedAnnotationOperations(compilationUnit, problems, operations);
		
		if (rawTypeReferences)
			createRawTypeReferenceOperations(compilationUnit, problems, operations);

		if (operations.size() == 0)
			return null;
		
		IFixRewriteOperation[] operationsArray= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new Java50Fix("", compilationUnit, operationsArray); //$NON-NLS-1$
	}
	
	private static void createAddDeprecatedAnnotationOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List result) {
		for (int i= 0; i < locations.length; i++) {
			int id= locations[i].getProblemId();
			
			if (id == IProblem.FieldMissingDeprecatedAnnotation ||
				id == IProblem.MethodMissingDeprecatedAnnotation ||
				id == IProblem.TypeMissingDeprecatedAnnotation) {
				
				IProblemLocation problem= locations[i];

				ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
				if (selectedNode != null) { 
					
					ASTNode declaringNode= getDeclaringNode(selectedNode);
					if (declaringNode instanceof BodyDeclaration) {
						BodyDeclaration declaration= (BodyDeclaration) declaringNode;
						AnnotationRewriteOperation operation= new AnnotationRewriteOperation(declaration, DEPRECATED);
						result.add(operation);
					}
				}
			}	
		}
	}

	private static void createAddOverrideAnnotationOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List result) {
		for (int i= 0; i < locations.length; i++) {
			
			if (locations[i].getProblemId() == IProblem.MissingOverrideAnnotation) {

				IProblemLocation problem= locations[i];

				ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
				if (selectedNode != null) { 
					
					ASTNode declaringNode= getDeclaringNode(selectedNode);
					if (declaringNode instanceof BodyDeclaration) {
						BodyDeclaration declaration= (BodyDeclaration) declaringNode;
						AnnotationRewriteOperation operation= new AnnotationRewriteOperation(declaration, OVERRIDE);
						result.add(operation);
					}
				}
			}	
		}
	}
	
	private static void createRawTypeReferenceOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List operations) {
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation problem= locations[i];
			ASTNode node= problem.getCoveredNode(compilationUnit);
			
			if (node instanceof ClassInstanceCreation) {
				node= (ASTNode)node.getStructuralProperty(ClassInstanceCreation.TYPE_PROPERTY);
			} else if (node instanceof SimpleName) {
				node= node.getParent();
			}
			
			if (!(node instanceof SimpleType))
				return;
				
			ITypeBinding binding= ((SimpleType)node).resolveBinding().getTypeDeclaration();
			ITypeBinding[] parameters= binding.getTypeParameters();
			if (parameters.length == 0)
				return;
			
			operations.add(new AddTypeParametersOperation((SimpleType)node));
		}
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
	
	private Java50Fix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewrites) {
		super(name, compilationUnit, fixRewrites);
	}
	
}
