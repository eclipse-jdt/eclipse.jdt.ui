package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.UpdateTypeReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class ExtractInterfaceRefactoring extends Refactoring {

	private final CodeGenerationSettings fCodeGenerationSettings;

	private IType fInputClass;
	private String fNewInterfaceName;
	private IMember[] fExtractedMembers;
	private boolean fReplaceOccurrences= true; //XXX 
	private TextChangeManager fChangeManager;
	private Set fBadVarSet;
	private Map fCUsToCuNodes;//ICompilationUnit -> CompilationUnit
	
	public ExtractInterfaceRefactoring(IType clazz, CodeGenerationSettings codeGenerationSettings){
		Assert.isNotNull(clazz);
		Assert.isNotNull(codeGenerationSettings);
		fInputClass= clazz;
		fCodeGenerationSettings= codeGenerationSettings;
		fExtractedMembers= new IMember[0];
		fBadVarSet= new HashSet(0);
		fCUsToCuNodes= new HashMap(0);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return "Extract Interface";
	}
	
	public IType getInputClass() {
		return fInputClass;
	}

	public String getNewInterfaceName() {
		return fNewInterfaceName;
	}

	public boolean isReplaceOccurrences() {
		return fReplaceOccurrences;
	}

	public void setNewInterfaceName(String newInterfaceName) {
		Assert.isNotNull(newInterfaceName);
		fNewInterfaceName= newInterfaceName;
	}

	public void setReplaceOccurrences(boolean replaceOccurrences) {
		fReplaceOccurrences= replaceOccurrences;
	}
	
	public void setExtractedMembers(IMember[] extractedMembers) throws JavaModelException{
		Assert.isTrue(areAllExtractableMembersOfClass(extractedMembers));
		fExtractedMembers= extractedMembers;
	}
	
	public IMember[] getExtractableMembers() throws JavaModelException{
		List members= new ArrayList();
		IJavaElement[] children= fInputClass.getChildren();
		for (int i= 0; i < children.length; i++) {
			if (children[i] instanceof IMember && isExtractableMember((IMember)children[i]))
				members.add(children[i]);
		}
		return (IMember[]) members.toArray(new IMember[members.size()]);
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= Checks.checkAvailability(fInputClass);	
		if (result.hasFatalError())
			return result;
		if (fInputClass.isInterface())
			result.addFatalError("Cannot perform Extract Interface on interfaces.");
		if (result.hasFatalError())
			return result;

		if (fInputClass.isLocal())
			result.addFatalError("Cannot perform Extract Interface on local types.");
		if (result.hasFatalError())
			return result;

		if (fInputClass.isAnonymous())
			result.addFatalError("Cannot perform Extract Interface on anonymous types.");
		if (result.hasFatalError())
			return result;

		//XXX for now
		if (! Checks.isTopLevel(fInputClass))
			result.addFatalError("Cannot perform Extract Interface on member classes.");
		if (result.hasFatalError())
			return result;
			
		return result;	
	}
	
	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		IType orig= (IType)WorkingCopyUtil.getOriginal(fInputClass);
		if (orig == null || ! orig.exists()){
			String key= "The selected type has been deleted from ''{0}''";
			String message= MessageFormat.format(key, new String[]{fInputClass.getCompilationUnit().getElementName()});
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fInputClass= orig;
		
		return Checks.checkIfCuBroken(fInputClass);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();		
			
			if (getInputClassPackage().getCompilationUnit(getCuNameForNewInterface()).exists()){
				String pattern= "Compilation Unit named ''{0}'' already exists in package ''{1}''";
				String message= MessageFormat.format(pattern, new String[]{getCuNameForNewInterface(), getInputClassPackage().getElementName()});
				result.addFatalError(message);
			}	
				
			result.merge(checkNewInterfaceName(fNewInterfaceName));
			
			result.merge(checkInterfaceTypeName());
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}

	private RefactoringStatus checkInterfaceTypeName() throws JavaModelException {
		ICompilationUnit[] cus= getInputClassPackage().getCompilationUnits();
		for (int i= 0; i < cus.length; i++) {
			if (cus[i].getType(fNewInterfaceName).exists()){
				String pattern= "Type named ''{0}'' already exists in package ''{1}''";
				String message= MessageFormat.format(pattern, new String[]{fNewInterfaceName, getInputClassPackage().getElementName()});
				return RefactoringStatus.createFatalErrorStatus(message);
			}	
		}
		return null;
	}
	
	public RefactoringStatus checkNewInterfaceName(String newName){
		RefactoringStatus result= Checks.checkTypeName(newName);
		if (result.hasFatalError())
			return result;
		result.merge(Checks.checkCompilationUnitName(newName + ".java"));
		return result;
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Creating change", 1);
			CompositeChange builder= new CompositeChange("Extract Interface");
			builder.addAll(fChangeManager.getAllChanges());
			builder.add(createExtractedInterface());
			return builder;	
		} catch(CoreException e){
			throw new JavaModelException(e);
		}	
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			updateTypeDeclaration(manager, new SubProgressMonitor(pm, 1));
			if (fReplaceOccurrences)
				updateReferences(manager, new SubProgressMonitor(pm, 1));
			else
				pm.worked(1);	
			return manager;
		} finally{
			pm.done();
		}	
	}

	private void updateReferences(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException, CoreException {
		pm.beginTask("", 2);//$NON-NLS-1$
		ISearchPattern pattern= SearchEngine.createSearchPattern(fInputClass, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= RefactoringScopeFactory.create(fInputClass);
		SearchResultGroup[] referenceGroups= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), scope, pattern);
		addReferenceUpdatesAndImports(manager, new SubProgressMonitor(pm, 1), referenceGroups);
		pm.done();
	}
	
	private void addReferenceUpdatesAndImports(TextChangeManager manager, IProgressMonitor pm, SearchResultGroup[] resultGroups) throws CoreException {
		Map mapping= getNodeMapping(resultGroups, pm); //ASTNode -> ICompilationUnit
		Set updatedCus= new HashSet(0);
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			ICompilationUnit cu= (ICompilationUnit)mapping.get(node);
			manager.get(cu).addTextEdit("update", createTypeUpdateEdit(new SourceRange(node)));	
			updatedCus.add(cu);
		}
		for (Iterator iter= updatedCus.iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit) iter.next();
			if (! getInputClassPackage().equals(cu.getParent()))
				addInterfaceImport(manager, cu);			
		}
	}

	//ASTNode -> ICompilationUnit
	private Map getNodeMapping(SearchResultGroup[] resultGroups, IProgressMonitor pm) throws JavaModelException {
		Map mapping= new HashMap();
		for (int i= 0; i < resultGroups.length; i++) {
			SearchResultGroup searchResultGroup= resultGroups[i];
			IJavaElement element= JavaCore.create(searchResultGroup.getResource());
			if (!(element instanceof ICompilationUnit))
				continue;
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)element);
			ASTNode[] nodes= getAstNodes(searchResultGroup.getSearchResults(), getAST(cu));
			for (int j= 0; j < nodes.length; j++) {
				mapping.put(nodes[j], cu);
			}			
		}
		retainUpdatableNodes(mapping, pm);
		return mapping;
	}

	//ASTNode -> ICompilationUnit
	private void retainUpdatableNodes(Map mapping, IProgressMonitor pm) throws JavaModelException {
		Collection nodesToRemove= computeNodesToRemove(mapping, pm);
		for (Iterator iter= nodesToRemove.iterator(); iter.hasNext();) {
			mapping.remove(iter.next());
		}
	}
	
	//ASTNode -> ICompilationUnit	
	private Collection computeNodesToRemove(Map mapping, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", mapping.keySet().size()); //XXX
		Collection nodesToRemove= new HashSet(0);
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			if (hasDirectProblems(node, new SubProgressMonitor(pm, 1)))
				nodesToRemove.add(node);	
		}
		boolean reiterate;
		do {
			reiterate= false;
			for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
				ASTNode node= (ASTNode) iter.next();
				if (! nodesToRemove.contains(node) && hasIndirectProblems(node, nodesToRemove, new NullProgressMonitor())){ //XXX
					reiterate= true;
					nodesToRemove.add(node);			
				}
			}
		} while (reiterate);	
		pm.done();
		return nodesToRemove;
	}

	private boolean hasIndirectProblems(ASTNode node, Collection nodesToRemove, IProgressMonitor pm) throws JavaModelException {
		ASTNode parentNode= node.getParent();
		if (parentNode instanceof VariableDeclarationStatement){
			VariableDeclarationStatement vds= (VariableDeclarationStatement)parentNode;
			if (vds.getType() != node)
				return false; 
			VariableDeclarationFragment[] vdfs= getVariableDeclarationFragments(vds);	
			for (int i= 0; i < vdfs.length; i++) {
				if (hasIndirectProblems(vdfs[i], nodesToRemove))
					return true;	
			}
		} else if (parentNode instanceof VariableDeclaration){
			if (hasIndirectProblems((VariableDeclaration)parentNode, nodesToRemove))
				return true;	
		} else if (parentNode instanceof CastExpression){
			if (! isReferenceUpdatable(parentNode, nodesToRemove))
				return true;	
		} else if (parentNode instanceof MethodDeclaration){
			MethodDeclaration methodDeclaration= (MethodDeclaration)parentNode;
			if (methodDeclaration.getReturnType() == node){
				IMethodBinding methodBinding= methodDeclaration.resolveBinding();
				if (methodBinding != null){
					IMethod method= Binding2JavaModel.find(methodBinding, getCompilationUnit(methodDeclaration).getJavaProject());
					if (method == null)
						return true; //XXX
					IMethod topMethod= getTopMethod(method, new SubProgressMonitor(pm, 1));
					IMethod[] methods= RippleMethodFinder.getRelatedMethods(topMethod, new SubProgressMonitor(pm, 1), new ICompilationUnit[0]);
					if (isAnyMethodReturnTypeNodeExcluded(methods, nodesToRemove)){
						nodesToRemove.addAll(getAllReturnTypeNodes(methods));
						return true;
					}	
				}	
			}		
		}
		return false;
	}

	private Collection getAllReturnTypeNodes(IMethod[] methods) throws JavaModelException {
		List result= new ArrayList(methods.length);
		for (int i= 0; i < methods.length; i++) {
			result.add(getMethodDeclarationNode(methods[i]).getReturnType());
		}
		return result;
	}

	private boolean isAnyMethodReturnTypeNodeExcluded(IMethod[] methods, Collection nodesToRemove) throws JavaModelException{
		for (int i= 0; i < methods.length; i++) {
			if (nodesToRemove.contains(getMethodDeclarationNode(methods[i]).getReturnType()))
				return true;
		}
		return false;
	}
	
	private static IMethod getTopMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(method);
		pm.beginTask("", 1);
		IMethod top= method;
		IMethod oldTop;
		do {
			oldTop = top;
			top= MethodChecks.overridesAnotherMethod(top, new NullProgressMonitor()); //XXX 	
		} while(top != null);
		pm.done();
		return oldTop;
	}
	
	private boolean hasIndirectProblems(VariableDeclaration tempDeclaration, Collection nodesToRemove) throws JavaModelException{
		ASTNode[] references= TempOccurrenceFinder.findTempOccurrenceNodes(tempDeclaration, true, false);
		for (int i= 0; i < references.length; i++) {
			if (! isReferenceUpdatable(references[i], nodesToRemove)){
				fBadVarSet.add(tempDeclaration);
				return true;
			}	
		}
		return false;
	}
	
	private boolean isReferenceUpdatable(ASTNode varReference, Collection nodesToRemove) throws JavaModelException{
		ASTNode parent= varReference.getParent();
		if (parent instanceof VariableDeclarationFragment){
			VariableDeclarationFragment r1= (VariableDeclarationFragment)parent;
			if (varReference == r1.getInitializer()){
				IVariableBinding vb= r1.resolveBinding();
				if (vb != null && fBadVarSet.contains(getCompilationUnitNode(varReference).findDeclaringNode(vb)))
					return false;
			}
		} else if (parent instanceof Assignment){
			Assignment assmnt= (Assignment)parent;
			if (varReference == assmnt.getRightHandSide()){
				Expression lhs= assmnt.getLeftHandSide();
				if (lhs instanceof SimpleName){
					IBinding binding= ((SimpleName)lhs).resolveBinding();
					if (binding != null && fBadVarSet.contains(getCompilationUnitNode(lhs).findDeclaringNode(binding)))
						return false;
				}
			}
		} else if (parent instanceof MethodInvocation){
			MethodInvocation mi= (MethodInvocation)parent;
			int argumentIndex= mi.arguments().indexOf(varReference);
			if (argumentIndex != -1 ){
				IBinding bin= mi.getName().resolveBinding();
				if (! (bin instanceof IMethodBinding))
					return false;
				IMethod method= Binding2JavaModel.find((IMethodBinding)bin, getInputClass().getJavaProject());
				ICompilationUnit cu= method.getCompilationUnit();
				if (cu == null)
					return false;
				SingleVariableDeclaration parDecl= (SingleVariableDeclaration)getMethodDeclarationNode(method).parameters().get(argumentIndex);
				if (fBadVarSet.contains(parDecl))
					return false;
			}
		} else if (parent instanceof ReturnStatement){
			MethodDeclaration md= (MethodDeclaration)ASTNodes.getParent(parent, MethodDeclaration.class);
			if (nodesToRemove.contains(md.getReturnType()))
				return false;
		} 
		return true;
	}
	
	private static CompilationUnit getCompilationUnitNode(ASTNode node) {
		return (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
	}

	private static ASTNode getUnparenthesizedParent(ASTNode node){
		if (! (node.getParent() instanceof ParenthesizedExpression))
			return node.getParent();
		ASTNode parent= (ParenthesizedExpression)node.getParent();
		while(parent instanceof ParenthesizedExpression){
			parent= parent.getParent();
		}
		return parent;
	}
	
	private boolean hasDirectProblems(ASTNode node, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			ASTNode parentNode= getUnparenthesizedParent(node);
			if (parentNode instanceof ClassInstanceCreation){
				if (node == ((ClassInstanceCreation)parentNode).getName())
					return true;
			}	
			if (parentNode instanceof TypeLiteral)
				return true;
				
			if (parentNode instanceof MethodDeclaration){
				MethodDeclaration md= (MethodDeclaration)parentNode;
				if (md.thrownExceptions().contains(node))
					return true;
				if (node == md.getReturnType()){
					ICompilationUnit cu= getCompilationUnit(node);
					IMethodBinding binding= md.resolveBinding();
					if (binding == null)
						return true; //XXX
					IMethod method= Binding2JavaModel.find(binding, cu.getJavaProject());
					if (method != null){
						ISearchPattern pattern= SearchEngine.createSearchPattern(method, IJavaSearchConstants.REFERENCES);
						IJavaSearchScope scope= RefactoringScopeFactory.create(method);
						SearchResultGroup[] groups= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), scope, pattern);
						for (int i= 0; i < groups.length; i++) {
							ICompilationUnit referencedCU= groups[i].getCompilationUnit();
							if (referencedCU == null)
								continue;
							SearchResult[] searchResults= groups[i].getSearchResults();
							ASTNode[] referenceNodes= getAstNodes(searchResults, getAST(referencedCU));
							for (int j= 0; j < referenceNodes.length; j++) {
								ASTNode aSTNode= referenceNodes[j];
								if (aSTNode.getParent() instanceof MethodInvocation){
									if (! isMethodInvocationOk((MethodInvocation)aSTNode.getParent())	)
										return true;
								}
							}
						}
					}	
				}
			}	
			
			if (parentNode instanceof FieldDeclaration){
				FieldDeclaration fd= (FieldDeclaration)parentNode;
				if (fd.getType() == node){
					VariableDeclarationFragment[] fragments= (VariableDeclarationFragment[]) fd.fragments().toArray(new VariableDeclarationFragment[fd.fragments().size()]);
					for (int i= 0; i < fragments.length; i++) {
						IVariableBinding vb= fragments[i].resolveBinding();
						IField field= Binding2JavaModel.lookupIField(vb, getCompilationUnit(fd).getJavaProject());
						ISearchPattern pattern= SearchEngine.createSearchPattern(field, IJavaSearchConstants.REFERENCES);
						IJavaSearchScope scope= RefactoringScopeFactory.create(field);
						SearchResultGroup[] resultGroups= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), scope, pattern);
						for (int j= 0; j < resultGroups.length; j++) {
							ICompilationUnit referencedCU= resultGroups[i].getCompilationUnit();
							if (referencedCU == null){
								addAllToBadVarSet(fragments);
								return true;
							}	
							SearchResult[] searchResults= resultGroups[i].getSearchResults();
							ASTNode[] referenceNodes= getAstNodes(searchResults, getAST(referencedCU));
							for (int k= 0; k < referenceNodes.length; k++) {
								ASTNode aSTNode= referenceNodes[j];
								//XXX code dup
								if (aSTNode.getParent() instanceof MethodInvocation){
									MethodInvocation mi= (MethodInvocation)aSTNode.getParent();
									if (mi.getExpression() == aSTNode && ! isMethodInvocationOk(mi)){
										addAllToBadVarSet(fragments);
										return true;
									}	
								}
								if (aSTNode.getParent() instanceof FieldAccess){
									addAllToBadVarSet(fragments);
									return true;
								}
							}
						}
					}
				}
			}
			
			if (parentNode instanceof SingleVariableDeclaration && parentNode.getParent() instanceof CatchClause)
				return true;
				
			if (parentNode instanceof TypeDeclaration){
				if(node == ((TypeDeclaration)parentNode).getSuperclass())
				 	return true;
			}
			if (parentNode instanceof VariableDeclarationStatement){
				VariableDeclarationStatement vds= (VariableDeclarationStatement)parentNode;
				if (vds.getType() == node && ! canReplaceTypeInVariableDeclarationStatement(vds))
					return true; 
			}	
	
			if (parentNode instanceof SingleVariableDeclaration){
				if (! areAllTempReferencesOK((SingleVariableDeclaration)parentNode)){
					addToBadVarSet((SingleVariableDeclaration)parentNode);
					return true;
				}	
			} 
			if (parentNode instanceof CastExpression){
				ASTNode pp= getUnparenthesizedParent(parentNode);
				//XXX code dup
				if (pp instanceof FieldAccess)
					return true;
				if (pp instanceof MethodInvocation){
					MethodInvocation mi= (MethodInvocation)pp;
					if (parentNode.getParent() == mi.getExpression() && ! isMethodInvocationOk(mi))
						return true;
				}	
			}
			return false;	
		} finally {
			pm.done();
		}	
	}

	private static ASTNode[] getAstNodes(SearchResult[] searchResults, CompilationUnit cuNode) {
		List result= new ArrayList(searchResults.length);
		for (int i= 0; i < searchResults.length; i++) {
			ASTNode node= getAstNode(searchResults[i], cuNode);
			if (node != null)
				result.add(node);
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	private static ASTNode getAstNode(SearchResult searchResult, CompilationUnit cuNode) {
		ASTNode selectedNode= getAstNode(cuNode, searchResult.getStart(), searchResult.getEnd() - searchResult.getStart());
		if (selectedNode == null)
			return null;
		if (selectedNode.getParent() == null)
			return null;
		return selectedNode;
	}

	private static ASTNode getAstNode(CompilationUnit cuNode, int start, int length){
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(start, length), true);
		cuNode.accept(analyzer);
		return analyzer.getFirstSelectedNode();
	}
	
	private void addInterfaceImport(TextChangeManager manager, ICompilationUnit cu) throws CoreException {
		ImportEdit importEdit= new ImportEdit(cu, fCodeGenerationSettings);
		importEdit.addImport(getFullyQualifiedInterfaceName());
		String pattern= "Adding import to ''{0}''";
		String editName= MessageFormat.format(pattern, new String[]{getFullyQualifiedInterfaceName()});
		manager.get(cu).addTextEdit(editName, importEdit);
	}

	private String getFullyQualifiedInterfaceName() {
		return getInputClassPackage().getElementName() + "." + fNewInterfaceName;
	}

	private TextEdit createTypeUpdateEdit(SearchResult searchResult) {
		int offset= searchResult.getStart();
		int length= searchResult.getEnd() - searchResult.getStart();
		return new UpdateTypeReferenceEdit(offset, length, fNewInterfaceName, fInputClass.getElementName());
	}

	private TextEdit createTypeUpdateEdit(ISourceRange sourceRange) {
		return new UpdateTypeReferenceEdit(sourceRange.getOffset(), sourceRange.getLength(), fNewInterfaceName, fInputClass.getElementName());
	}
		
	private boolean areAllExtractableMembersOfClass(IMember[] extractedMembers) throws JavaModelException {
		for (int i= 0; i < extractedMembers.length; i++) {
			if (! extractedMembers[i].getParent().equals(fInputClass))
				return false;
			if (! isExtractableMember(extractedMembers[i]))
				return false;
		}
		return true;
	}

	private static boolean isExtractableMember(IMember iMember) throws JavaModelException {
		if (iMember.getElementType() == IJavaElement.METHOD)
			return isExtractableMethod((IMethod)iMember);
		if (iMember.getElementType() == IJavaElement.FIELD)	
			return isExtractableField((IField)iMember);
		return false;	
	}

	private static boolean isExtractableField(IField iField) throws JavaModelException {
		if (! JdtFlags.isPublic(iField))
			return false;
		if (! JdtFlags.isStatic(iField))
			return false;
		if (! JdtFlags.isFinal(iField))
			return false;
		return true;		
	}

	private static boolean isExtractableMethod(IMethod iMethod) throws JavaModelException {
		if (! JdtFlags.isPublic(iMethod))
			return false;
		if (JdtFlags.isStatic(iMethod))
			return false;
		return true;		
	}
	
	//----- methods related to creation of the new interface -------
	private IChange createExtractedInterface() throws CoreException {
		String lineSeparator= getLineSeperator(); 
		IPath cuPath= ResourceUtil.getFile(fInputClass.getCompilationUnit()).getFullPath();
		IPath interfaceCuPath= cuPath
										.removeLastSegments(1)
										.append(getCuNameForNewInterface());
		//XXX need to destroy
		ICompilationUnit newCuWC= getInputClassPackage().getCompilationUnit(getCuNameForNewInterface());
		String source= createExtractedInterfaceCUSource(newCuWC);
		String formattedSource= ToolFactory.createCodeFormatter().format(source, 0, null, lineSeparator);
		return new CreateTextFileChange(interfaceCuPath, formattedSource, true);	
	}
	
	private String getCuNameForNewInterface() {
		return fNewInterfaceName + ".java";
	}
	
	private IPackageFragment getInputClassPackage() {
		return (IPackageFragment)fInputClass.getCompilationUnit().getParent();
	}

	private static String getLineSeperator() {
		return System.getProperty("line.separator", "\n");//$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean inputClassHasDirectSuperinterfaces() throws JavaModelException {
		return fInputClass.getSuperInterfaceNames().length > 0;
	}

	private void updateTypeDeclaration(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);//$NON-NLS-1$
		String editName= "Update Type Declaration";
		int offset= computeIndexOfSuperinterfaceNameInsertion();
		String text=  computeTextOfSuperinterfaceNameInsertion();
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fInputClass.getCompilationUnit());
		manager.get(cu).addTextEdit(editName, SimpleTextEdit.createInsert(offset, text));
		pm.done();
	}

	private String computeTextOfSuperinterfaceNameInsertion() throws JavaModelException {
		if (! inputClassHasDirectSuperinterfaces())
			return " implements " + fNewInterfaceName;	//$NON-NLS-1$
		else
			return ", " + fNewInterfaceName; //$NON-NLS-1$
	}

	private int computeIndexOfSuperinterfaceNameInsertion() throws JavaModelException {
		TypeDeclaration typeNode= getTypeDeclarationNode();
		if (typeNode.superInterfaces().isEmpty()){
			if (typeNode.getSuperclass() == null)
				return ASTNodes.getExclusiveEnd(typeNode.getName());
			else 
				return ASTNodes.getExclusiveEnd(typeNode.getSuperclass());
		} else {
			Name lastInterfaceName= (Name)typeNode.superInterfaces().get(typeNode.superInterfaces().size() - 1);
			return ASTNodes.getExclusiveEnd(lastInterfaceName);
		}
	}

	private TypeDeclaration getTypeDeclarationNode() throws JavaModelException {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(fInputClass.getNameRange().getOffset(), fInputClass.getNameRange().getLength() +1), true);
		getAST(fInputClass.getCompilationUnit()).accept(analyzer);
		if (analyzer.getFirstSelectedNode() != null){
			if (analyzer.getFirstSelectedNode().getParent() instanceof TypeDeclaration)
				return (TypeDeclaration)analyzer.getFirstSelectedNode().getParent();
		}
		//XXX workaround for 21757
		if (analyzer.getLastCoveringNode() instanceof TypeDeclaration)
			return (TypeDeclaration) analyzer.getLastCoveringNode();
		return null;	
	}

	private String createExtractedInterfaceCUSource(ICompilationUnit newCu) throws CoreException {
		StringBuffer buffer= new StringBuffer();
		if (fCodeGenerationSettings.createFileComments)
			buffer.append(createFileCommentsSource(newCu));
		buffer.append(createPackageDeclarationSource());
		buffer.append(createImportsSource());
		if (fCodeGenerationSettings.createComments){
			buffer.append(getLineSeperator());
			buffer.append(createTypeCommentSource(newCu));
		}	
		buffer.append(createInterfaceSource());
		return buffer.toString();
	}

	private String createInterfaceSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		buff.append(createInterfaceModifierString())
			 .append("interface ")
			 .append(fNewInterfaceName)
			 .append(" {")
			 .append(createInterfaceMemberDeclarationsSource())
			 .append("}");
		return buff.toString();
	}

	private String createInterfaceModifierString() throws JavaModelException {
		if (JdtFlags.isPublic(fInputClass))
			return "public ";
		else
			return "";	
	}

	private String createInterfaceMemberDeclarationsSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		sortByOffset(fExtractedMembers);
		for (int i= 0; i < fExtractedMembers.length; i++) {
			buff.append(createInterfaceMemberDeclarationsSource(fExtractedMembers[i]));
		}
		return buff.toString();
	}
	
	private static void sortByOffset(IMember[] members) {
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2) {
				ISourceReference sr1= (ISourceReference)o1;
				ISourceReference sr2= (ISourceReference)o2;
				try {
					return sr1.getSourceRange().getOffset() - sr2.getSourceRange().getOffset();
				} catch (JavaModelException e) {
					return 0;
				}
			}
		};
		Arrays.sort(members, comparator);
	}

	private String createInterfaceMemberDeclarationsSource(IMember iMember) throws JavaModelException {
		Assert.isTrue(iMember.getElementType() == IJavaElement.FIELD || iMember.getElementType() == IJavaElement.METHOD);
		if (iMember.getElementType() == IJavaElement.FIELD)
			return iMember.getSource();
		else 
			return createInterfaceMethodDeclarationsSource((IMethod)iMember);
	}

	private String createInterfaceMethodDeclarationsSource(IMethod iMethod) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(iMethod);
		if (methodDeclaration == null)
			return ""; 
	
		int offset= methodDeclaration.getReturnType().getStartPosition();
		int length= getMethodDeclarationLength(iMethod, methodDeclaration);
		String methodDeclarationSource= iMethod.getCompilationUnit().getBuffer().getText(offset, length);
		if (methodDeclaration.getBody() == null)
			return methodDeclarationSource;
		else
			return methodDeclarationSource + ";";
	}
	
	private static int getMethodDeclarationLength(IMethod iMethod, MethodDeclaration methodDeclaration) throws JavaModelException{
		int preDeclarationSourceLength= methodDeclaration.getReturnType().getStartPosition() - iMethod.getSourceRange().getOffset();
		if (methodDeclaration.getBody() == null)
			return methodDeclaration.getLength() - preDeclarationSourceLength;
		else
			return iMethod.getSourceRange().getLength() - methodDeclaration.getBody().getLength() - preDeclarationSourceLength;
	}

	private MethodDeclaration getMethodDeclarationNode(IMethod iMethod) throws JavaModelException{
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(iMethod.getSourceRange().getOffset(), iMethod.getSourceRange().getLength()), true);
		getAST(iMethod.getCompilationUnit()).accept(analyzer);
		if (! (analyzer.getFirstSelectedNode() instanceof MethodDeclaration))
			return null;
		return (MethodDeclaration) analyzer.getFirstSelectedNode();
	}
	
	private String createImportsSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		ITypeBinding[] typesUsed= getTypesUsedInExtractedMemberDeclarations();
		for (int i= 0; i < typesUsed.length; i++) {
			ITypeBinding binding= typesUsed[i];
			if (binding != null && ! binding.isPrimitive()){
				buff.append("import ");//$NON-NLS-1$
				buff.append(Bindings.getFullyQualifiedImportName(binding));
				buff.append(";");//$NON-NLS-1$
			}	
		}
		return buff.toString();
	}

	private ITypeBinding[] getTypesUsedInExtractedMemberDeclarations() throws JavaModelException{
		Set typesUsed= new HashSet();
		for (int i= 0; i < fExtractedMembers.length; i++) {
			if (fExtractedMembers[i].getElementType() == IJavaElement.METHOD)
				typesUsed.addAll(getTypesUsedInDeclaration((IMethod)fExtractedMembers[i]));
			else if (fExtractedMembers[i].getElementType() == IJavaElement.FIELD)	
				typesUsed.addAll(getTypesUsedInDeclaration((IField)fExtractedMembers[i]));	
			else
				Assert.isTrue(false);	
		}
		return (ITypeBinding[]) typesUsed.toArray(new ITypeBinding[typesUsed.size()]);
	}
	
	//set of ITypeBindings
	private Set getTypesUsedInDeclaration(IField iField) {
		//XXX
		return new HashSet(0);
	}

	//set of ITypeBindings
	private Set getTypesUsedInDeclaration(IMethod iMethod) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(iMethod);
		if (methodDeclaration == null)
			return new HashSet(0);
		Set result= new HashSet();	
		result.add(methodDeclaration.getReturnType().resolveBinding());
			
		for (Iterator iter= methodDeclaration.parameters().iterator(); iter.hasNext();) {
			result.add(((SingleVariableDeclaration) iter.next()).getType().resolveBinding()); 
		}
		
		for (Iterator iter= methodDeclaration.thrownExceptions().iterator(); iter.hasNext();) {
			result.add(((Name) iter.next()).resolveTypeBinding());
		}
		return result;
	}

	private String createPackageDeclarationSource() {
		return "package " + getInputClassPackage().getElementName() + ";";//$NON-NLS-2$ //$NON-NLS-1$
	}

	private String createTypeCommentSource(ICompilationUnit newCu) throws CoreException {
		return getTemplate("typecomment", 0, newCu);//$NON-NLS-1$
	}

	private String createFileCommentsSource(ICompilationUnit newCu) throws CoreException {
		return getTemplate("filecomment", 0, newCu);//$NON-NLS-1$
	}

	private static String getTemplate(String name, int pos, ICompilationUnit newCu) throws CoreException {
		Template[] templates= Templates.getInstance().getTemplates(name);
		if (templates.length == 0)
			return ""; //$NON-NLS-1$	
		String template= JavaContext.evaluateTemplate(templates[0], newCu, pos);
		if (template == null)
			return ""; //$NON-NLS-1$
		return template;
	}

	private static VariableDeclarationFragment[] getVariableDeclarationFragments(VariableDeclarationStatement vds){
		return (VariableDeclarationFragment[]) vds.fragments().toArray(new VariableDeclarationFragment[vds.fragments().size()]);
	}
	
	//--- 'can replace*' related methods 
	private boolean canReplaceTypeInVariableDeclarationStatement(VariableDeclarationStatement vds) throws JavaModelException{
		VariableDeclarationFragment[] fragments= getVariableDeclarationFragments(vds);
		for (int i= 0; i < fragments.length; i++) {
			if (! areAllTempReferencesOK(fragments[i])){
				addToBadVarSet(fragments[i]);
				return false;
			}	
		}
		return true;
	}

	private void addToBadVarSet(VariableDeclaration variableDeclaration) {
		fBadVarSet.add(variableDeclaration);
	}
	
	private void addAllToBadVarSet(VariableDeclaration[] variableDeclarations) {
		for (int i= 0; i < variableDeclarations.length; i++) {
			addToBadVarSet(variableDeclarations[i]);
		}
	}
	

	private boolean areAllTempReferencesOK(VariableDeclaration tempDeclaration) throws JavaModelException{
		ASTNode[] tempReferences= TempOccurrenceFinder.findTempOccurrenceNodes(tempDeclaration, true, false);			
		for (int i= 0; i < tempReferences.length; i++) {
			ASTNode parentNode= tempReferences[i].getParent();
			if (parentNode instanceof FieldAccess)
				return false;
			if (parentNode instanceof MethodInvocation){
				MethodInvocation mi= (MethodInvocation)parentNode;
				if (tempReferences[i] == mi.getExpression() &&  ! isMethodInvocationOk(mi))
					return false;
			}	
		}	
		return true;
	}
	
	private boolean isMethodInvocationOk(MethodInvocation mi) throws JavaModelException{
		IBinding miBinding= mi.getName().resolveBinding();
		if (miBinding == null || miBinding.getKind() != IBinding.METHOD)
			return false;
		IMethod method= Binding2JavaModel.find((IMethodBinding)miBinding, fInputClass);
		if (method == null || ! Arrays.asList(fExtractedMembers).contains(method))
			return false;	
		return true;	
	}
	
	private CompilationUnit getAST(ICompilationUnit cu){
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		if (fCUsToCuNodes.containsKey(wc)){
			return (CompilationUnit)fCUsToCuNodes.get(wc);
		}	
		CompilationUnit cuNode= AST.parseCompilationUnit(wc, true);
		fCUsToCuNodes.put(wc, cuNode);
		return cuNode;	
	}
	
	private ICompilationUnit getCompilationUnit(ASTNode node) {
		CompilationUnit cuNode= (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
		Assert.isTrue(fCUsToCuNodes.containsValue(cuNode)); //the cu node must've been created before
		
		for (Iterator iter= fCUsToCuNodes.keySet().iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit) iter.next();
			if (fCUsToCuNodes.get(cu) == cuNode)
				return cu;
		}	
		Assert.isTrue(false); //checked before
		return null;
	}

	
}
