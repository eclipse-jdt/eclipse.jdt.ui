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
package org.eclipse.jdt.internal.corext.refactoring.reorg2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MovePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MovePackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.CreateCopyOfCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveCuUpdateCreator;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameSearchResult;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

class ReorgPolicyFactory {
	private ReorgPolicyFactory(){
	}
	
	public static ICopyPolicy createCopyPolicy(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		return (ICopyPolicy)createReorgPolicy(true, resources, javaElements, settings);
	}
	
	public static IMovePolicy createMovePolicy(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		return (IMovePolicy)createReorgPolicy(false, resources, javaElements, settings);
	}
	
	private static IReorgPolicy createReorgPolicy(boolean copy, IResource[] selectedResources, IJavaElement[] selectedJavaElements, CodeGenerationSettings settings) throws JavaModelException{
		final IReorgPolicy NO;
		if (copy)
			NO= new NoCopyPolicy();
		else
			NO= new NoMovePolicy();

		ActualSelectionComputer selectionComputer= new ActualSelectionComputer(selectedJavaElements, selectedResources);
		IResource[] resources= selectionComputer.getActualResourcesToReorg();
		IJavaElement[] javaElements= selectionComputer.getActualJavaElementsToReorg();
	
		if (isNothingToReorg(resources, javaElements) || 
			containsNull(resources) ||
			containsNull(javaElements) ||
			ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.JAVA_PROJECT) ||
			ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.JAVA_MODEL) ||
			ReorgUtils2.hasElementsOfType(resources, IResource.PROJECT | IResource.ROOT) ||
			! haveCommonParent(resources, javaElements))
			return NO;
			
		if (ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT)){
			if (resources.length != 0 || ReorgUtils2.hasElementsNotOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT))
				return NO;
			if (copy)
				return new CopyPackagesPolicy(ArrayTypeConverter.toPackageArray(javaElements));
			else
				return new MovePackagesPolicy(ArrayTypeConverter.toPackageArray(javaElements));
		}
		
		if (ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT_ROOT)){
			if (resources.length != 0 || ReorgUtils2.hasElementsNotOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT_ROOT))
				return NO;
			if (copy)
				return new CopyPackageFragmentRootsPolicy(ArrayTypeConverter.toPackageFragmentRootArray(javaElements));
			else
				return new MovePackageFragmentRootsPolicy(ArrayTypeConverter.toPackageFragmentRootArray(javaElements));
		}
		
		if (ReorgUtils2.hasElementsOfType(resources, IResource.FILE | IResource.FOLDER) || ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT)){
			if (ReorgUtils2.hasElementsNotOfType(javaElements, IJavaElement.COMPILATION_UNIT))
				return NO;
			if (ReorgUtils2.hasElementsNotOfType(resources, IResource.FILE | IResource.FOLDER))
				return NO;
			if (copy)
				return new CopyFilesFoldersAndCusPolicy(ReorgUtils2.getFiles(resources), ReorgUtils2.getFolders(resources), ArrayTypeConverter.toCuArray(javaElements));
			else
				return new MoveFilesFoldersAndCusPolicy(ReorgUtils2.getFiles(resources), ReorgUtils2.getFolders(resources), ArrayTypeConverter.toCuArray(javaElements), settings);
		}
		
		if (hasElementsSmallerThanCuOrClassFile(javaElements)){
			//assertions guaranteed by common parent
			Assert.isTrue(resources.length == 0);
			Assert.isTrue(! ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT));
			Assert.isTrue(! ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.CLASS_FILE));
			Assert.isTrue(! hasElementsLargerThanCuOrClassFile(javaElements));
			if (copy)
				return new CopySubCuElementsPolicy(javaElements);
			else
				return new MoveSubCuElementsPolicy(javaElements);
		}
		return NO;
	}
		
	private static boolean containsNull(Object[] objects) {
		for (int i= 0; i < objects.length; i++) {
			if (objects[i]==null) return  true;
		}
		return false;
	}

	private static boolean hasElementsSmallerThanCuOrClassFile(IJavaElement[] javaElements) {
		for (int i= 0; i < javaElements.length; i++) {
			if (ReorgUtils2.isInsideCompilationUnit(javaElements[i]))
				return true;
			if (ReorgUtils2.isInsideClassFile(javaElements[i]))
				return true;
		}
		return false;
	}

	private static boolean hasElementsLargerThanCuOrClassFile(IJavaElement[] javaElements) {
		for (int i= 0; i < javaElements.length; i++) {
			if (! ReorgUtils2.isInsideCompilationUnit(javaElements[i]) &&
				! ReorgUtils2.isInsideClassFile(javaElements[i]))
				return true;
		}
		return false;
	}

	private static boolean haveCommonParent(IResource[] resources, IJavaElement[] javaElements) {
		return new ParentChecker(resources, javaElements).haveCommonParent();
	}

	private static boolean isNothingToReorg(IResource[] resources, IJavaElement[] javaElements) {
		return resources.length + javaElements.length == 0;
	}

	private static abstract class ReorgPolicy implements IReorgPolicy{
		//invariant: only 1 of these can ever be not null
		private IResource fResourceDestination;
		private IJavaElement fJavaElementDestination;
			
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.ICopyPolicy#setDestination(org.eclipse.core.resources.IResource)
		 */
		public final RefactoringStatus setDestination(IResource destination) throws JavaModelException {
			Assert.isNotNull(destination);
			resetDestinations();
			fResourceDestination= destination;
			return verifyDestination(destination);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.ICopyPolicy#setDestination(org.eclipse.jdt.core.IJavaElement)
		 */
		public final RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException {
			Assert.isNotNull(destination);
			resetDestinations();
			fJavaElementDestination= destination;
			return verifyDestination(destination);
		}
		protected abstract RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException;
		protected abstract RefactoringStatus verifyDestination(IResource destination) throws JavaModelException;
			
		private void resetDestinations() {
			fJavaElementDestination= null;
			fResourceDestination= null;
		}
		public final IResource getResourceDestination(){
			return fResourceDestination;
		}
		public final IJavaElement getJavaElementDestination(){
			return fJavaElementDestination;
		}
		public IFile[] getAllModifiedFiles() {
			return new IFile[0];
		}
		public RefactoringStatus checkInput(IProgressMonitor pm, IReorgQueries reorgQueries) throws JavaModelException{
			Assert.isNotNull(reorgQueries);
			return Checks.validateModifiesFiles(getAllModifiedFiles());
		}
		public boolean hasAllInputSet() {
			return fJavaElementDestination != null || fResourceDestination != null;
		}
		public boolean canUpdateReferences() {
			return false;
		}
		public boolean getUpdateReferences() {
			Assert.isTrue(false);//should not be called if canUpdateReferences is not overridden and returns false
			return false;
		}
		public void setUpdateReferences(boolean update) {
			Assert.isTrue(false);//should not be called if canUpdateReferences is not overridden and returns false
		}
		public boolean canEnableQualifiedNameUpdating() {
			return false;
		}
		public boolean canUpdateQualifiedNames() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return false;
		}
		public String getFilePatterns() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return null;
		}
		public boolean getUpdateQualifiedNames() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return false;
		}
		public void setFilePatterns(String patterns) {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
		}
		public void setUpdateQualifiedNames(boolean update) {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
		}
		public boolean canEnable() throws JavaModelException {
			IResource[] resources= getResources();
			for (int i= 0; i < resources.length; i++) {
				IResource resource= resources[i];
				if (! resource.exists() || resource.isPhantom() || ! resource.isAccessible())
					return false;
			}
			
			IJavaElement[] javaElements= getJavaElements();
			for (int i= 0; i < javaElements.length; i++) {
				IJavaElement element= javaElements[i];
				if (!element.exists()) return false;
			}
			return true;
		}
	}

	private static abstract class FilesFoldersAndCusReorgPolicy extends ReorgPolicy{

		private ICompilationUnit[] fCus;
		private IFolder[] fFolders;
		private IFile[] fFiles;

		public FilesFoldersAndCusReorgPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
			fFiles= files;
			fFolders= folders;
			fCus= cus;
		}

		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this operation");
			Assert.isTrue(! (javaElement instanceof IJavaModel));
	
			if (javaElement.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus("The selected destination is read-only");
	
			if (! javaElement.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus("The structure of the selected destination is not known");
	
			if (javaElement instanceof IOpenable){
				IOpenable openable= (IOpenable)javaElement;
				if (! openable.isConsistent())
					return RefactoringStatus.createFatalErrorStatus("The selected destination is not consistent with its underlying resource or buffer");
			}				
	
			if (javaElement instanceof IPackageFragmentRoot){
				IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement;
				if (root.isArchive())
					return RefactoringStatus.createFatalErrorStatus("The selected destination is an archive");
				if (root.isExternal())
					return RefactoringStatus.createFatalErrorStatus("The selected destination is external to the workbench");
			}
	
			if (ReorgUtils2.isInsideCompilationUnit(javaElement))
				return RefactoringStatus.createFatalErrorStatus("Elements inside compilation units cannot be used as destinations for copying files, folders or compilation units");
	
			if (containsLinkedResources() && !ReorgUtils2.canBeDestinationForLinkedResources(javaElement))
				return RefactoringStatus.createFatalErrorStatus("Linked resources can only be copied to projects");
			return new RefactoringStatus();
		}

		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			Assert.isNotNull(resource);
			if (! resource.exists() || resource.isPhantom())
				return RefactoringStatus.createFatalErrorStatus("The selected destination does not exist or is a phantom resource");			
			if (!resource.isAccessible())
				return RefactoringStatus.createFatalErrorStatus("The selected destination is not accessible");
			Assert.isTrue(resource.getType() != IResource.ROOT);
					
			if (isChildOfOrEqualToAnyFolder(resource))
				return RefactoringStatus.createFatalErrorStatus("The selected resource cannot be used as a destination");
						
			if (containsLinkedResources() && !ReorgUtils2.canBeDestinationForLinkedResources(resource))
				return RefactoringStatus.createFatalErrorStatus("Linked resources can only be copied to projects");
	
			return new RefactoringStatus();
		}

		private boolean isChildOfOrEqualToAnyFolder(IResource resource) {
			for (int i= 0; i < fFolders.length; i++) {
				IFolder folder= fFolders[i];
				if (folder.equals(resource) || ParentChecker.isDescendantOf(resource, folder))
					return true;
			}
			return false;
		}
	
		private static IContainer getAsContainer(IResource resDest){
			if (resDest instanceof IContainer)
				return (IContainer)resDest;
			if (resDest instanceof IFile)
				return (IContainer)((IFile)resDest).getParent();
			Assert.isTrue(false);//there's nothing else
			return null;				
		}
				
		protected IContainer getDestinationAsContainer(){
			IResource resDest= getResourceDestination();
			if (resDest != null)
				return getAsContainer(resDest);
			IJavaElement jelDest= getJavaElementDestination();
			Assert.isNotNull(jelDest);				
			return getAsContainer(ReorgUtils2.getResource(jelDest));
		}
				
		protected IPackageFragment getDestinationAsPackageFragment() {
			IPackageFragment javaAsPackage= getJavaDestinationAsPackageFragment(getJavaElementDestination());
			if (javaAsPackage != null)
				return javaAsPackage;
			return getResourceDestinationAsPackageFragment(getResourceDestination());
		}
				
		private static IPackageFragment getJavaDestinationAsPackageFragment(IJavaElement javaDest){
			if( javaDest == null || ! javaDest.exists())
				return null;					
			if (javaDest instanceof IPackageFragment)
				return (IPackageFragment) javaDest;
			if (javaDest instanceof IPackageFragmentRoot)
				return ((IPackageFragmentRoot) javaDest).getPackageFragment("");
			if (javaDest instanceof ICompilationUnit)
				return (IPackageFragment)javaDest.getParent();				
			return null;
		}
				
		private static IPackageFragment getResourceDestinationAsPackageFragment(IResource resource){
			if (resource instanceof IFile)
				return getJavaDestinationAsPackageFragment(JavaCore.create(resource.getParent()));
			return null;	
		}

		public final IJavaElement[] getJavaElements(){
			return fCus;
		}

		public final IResource[] getResources() {
			return ReorgUtils2.union(fFiles, fFolders);
		}

		protected boolean containsLinkedResources() {
			return 	ReorgUtils2.containsLinkedResources(fFiles) || 
					ReorgUtils2.containsLinkedResources(fFolders) || 
					ReorgUtils2.containsLinkedResources(fCus);
		}

		protected final IFolder[] getFolders(){
			return fFolders;
		}
		protected final IFile[] getFiles(){
			return fFiles;
		}
		protected final ICompilationUnit[] getCus(){
			return fCus;
		}
		public RefactoringStatus checkInput(IProgressMonitor pm, IReorgQueries reorgQueries) throws JavaModelException {
			RefactoringStatus status= super.checkInput(pm, reorgQueries);
			confirmOverwritting(reorgQueries);
			return status;
		}

		private void confirmOverwritting(IReorgQueries reorgQueries) throws JavaModelException {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setFiles(fFiles);
			oh.setFolders(fFolders);
			oh.setCus(fCus);
			IPackageFragment destPack= getDestinationAsPackageFragment();
			if (destPack != null)
				oh.confirmOverwritting(reorgQueries, destPack);
			else
				oh.confirmOverwritting(reorgQueries, getDestinationAsContainer());
			fFiles= oh.getFilesWithoutUnconfirmedOnes();
			fFolders= oh.getFoldersWithoutUnconfirmedOnes();
			fCus= oh.getCusWithoutUnconfirmedOnes();
		}
	}

	private static abstract class SubCuElementReorgPolicy extends ReorgPolicy{
		private final IJavaElement[] fJavaElements;
		SubCuElementReorgPolicy(IJavaElement[] javaElements){
			Assert.isNotNull(javaElements);
			fJavaElements= javaElements;
		}

		protected final RefactoringStatus verifyDestination(IResource destination) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus("A resource cannot be the destination for the selected elements.");
		}

		protected final ICompilationUnit getSourceCu() {
			//all have a common parent, so all must be in the same cu
			//we checked before that the array in not null and not empty
			return (ICompilationUnit) fJavaElements[0].getAncestor(IJavaElement.COMPILATION_UNIT);
		}

		public final IJavaElement[] getJavaElements() {
			return fJavaElements;
		}
		public final IResource[] getResources() {
			return new IResource[0];
		}
	
		protected final ICompilationUnit getDestinationCu() {
			return getDestinationCu(getJavaElementDestination());
		}

		protected static final ICompilationUnit getDestinationCu(IJavaElement destination) {
			if (destination instanceof ICompilationUnit)
				return (ICompilationUnit) destination;
			return (ICompilationUnit) destination.getAncestor(IJavaElement.COMPILATION_UNIT);
		}
	
		protected static TextChange addTextEditFromRewrite(ICompilationUnit cu, ASTRewrite rewrite) throws CoreException {
			TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
			TextEdit resultingEdits= new MultiTextEdit();
			rewrite.rewriteNode(textBuffer, resultingEdits, null);
				
			TextChange textChange= new CompilationUnitChange(cu.getElementName(), cu);
			if (textChange instanceof TextFileChange){
				TextFileChange tfc= (TextFileChange)textChange;
				tfc.setSave(! cu.isWorkingCopy());
			}
			String message= "Copy";
			textChange.addTextEdit(message, resultingEdits);
			rewrite.removeModifications();
			return textChange;
		}
	
		protected void copyToDestination(IJavaElement element, ASTRewrite rewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			switch(element.getElementType()){
				case IJavaElement.FIELD: 
					copyFieldToDestination((IField)element, rewrite, sourceCuNode, destinationCuNode);
					break;
				case IJavaElement.IMPORT_CONTAINER:
					copyImportsToDestination((IImportContainer)element, rewrite, sourceCuNode, destinationCuNode);
					break;
				case IJavaElement.IMPORT_DECLARATION:
					copyImportToDestination((IImportDeclaration)element, rewrite, sourceCuNode, destinationCuNode);
					break;
				case IJavaElement.INITIALIZER:
					copyInitializerToDestination((IInitializer)element, rewrite, destinationCuNode);
					break;
				case IJavaElement.METHOD: 
					copyMethodToDestination((IMethod)element, rewrite, destinationCuNode);
					break;
				case IJavaElement.PACKAGE_DECLARATION:
					copyPackageDeclarationToDestination((IPackageDeclaration)element, rewrite, sourceCuNode, destinationCuNode);
					break;
				case IJavaElement.TYPE :
					copyTypeToDestination((IType) element, rewrite, destinationCuNode);
					break;

				default: Assert.isTrue(false); 
			}
		}

		private void copyFieldToDestination(IField field, ASTRewrite targetRewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			//cannot copy the whole field declaration - it can contain more than 1 field
			FieldDeclaration copiedNode= createNewFieldDeclarationNode(field, getAST(targetRewrite), sourceCuNode);
			targetRewrite.markAsInserted(copiedNode);
			TypeDeclaration targetClass= getTargetType(destinationCuNode);
			targetClass.bodyDeclarations().add(copiedNode);
		}

		private FieldDeclaration createNewFieldDeclarationNode(IField field, AST targetAst, CompilationUnit sourceCuNode) throws JavaModelException {
			FieldDeclaration fieldDeclaration= ASTNodeSearchUtil.getFieldDeclarationNode(field, sourceCuNode);
			if (fieldDeclaration.fragments().size() == 1)
				return (FieldDeclaration) ASTNode.copySubtree(targetAst, fieldDeclaration);
			VariableDeclarationFragment originalFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, sourceCuNode);
			VariableDeclarationFragment copiedFragment= (VariableDeclarationFragment) ASTNode.copySubtree(targetAst, originalFragment);
			FieldDeclaration newFieldDeclaration= targetAst.newFieldDeclaration(copiedFragment);
			if (fieldDeclaration.getJavadoc() != null)
				newFieldDeclaration.setJavadoc((Javadoc) ASTNode.copySubtree(targetAst, fieldDeclaration.getJavadoc()));
			newFieldDeclaration.setModifiers(fieldDeclaration.getModifiers());
			newFieldDeclaration.setType((Type) ASTNode.copySubtree(targetAst, fieldDeclaration.getType()));
			return newFieldDeclaration;
		}

		private void copyImportsToDestination(IImportContainer container, ASTRewrite rewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			//there's no special AST node for the container - we copy all imports
			IJavaElement[] importDeclarations= container.getChildren();
			for (int i= 0; i < importDeclarations.length; i++) {
				Assert.isTrue(importDeclarations[i] instanceof IImportDeclaration);//promised in API
				IImportDeclaration importDeclaration= (IImportDeclaration)importDeclarations[i];
				copyImportToDestination(importDeclaration, rewrite, sourceCuNode, destinationCuNode);
			}
		}

		private void copyImportToDestination(IImportDeclaration declaration, ASTRewrite targetRewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			ImportDeclaration sourceNode= ASTNodeSearchUtil.getImportDeclarationNode(declaration, sourceCuNode);
			ImportDeclaration copiedNode= (ImportDeclaration) ASTNode.copySubtree(getAST(targetRewrite), sourceNode);
			targetRewrite.markAsInserted(copiedNode);
			destinationCuNode.imports().add(copiedNode);
		}

		private void copyPackageDeclarationToDestination(IPackageDeclaration declaration, ASTRewrite targetRewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			PackageDeclaration sourceNode= ASTNodeSearchUtil.getPackageDeclarationNode(declaration, sourceCuNode);
			Initializer copiedNode= (Initializer) ASTNode.copySubtree(getAST(targetRewrite), sourceNode);
			targetRewrite.markAsInserted(copiedNode);
			destinationCuNode.types().add(copiedNode);
		}

		private void copyInitializerToDestination(IInitializer initializer, ASTRewrite targetRewrite, CompilationUnit destinationCuNode) throws JavaModelException {
			BodyDeclaration newInitializer= (BodyDeclaration) targetRewrite.createPlaceholder(getUnindentedSource(initializer), ASTRewrite.INITIALIZER);
			targetRewrite.markAsInserted(newInitializer);
			TypeDeclaration targetClass= getTargetType(destinationCuNode);
			targetClass.bodyDeclarations().add(newInitializer);
		}

		private void copyTypeToDestination(IType type, ASTRewrite targetRewrite, CompilationUnit destinationCuNode) throws JavaModelException {
			TypeDeclaration newType= (TypeDeclaration) targetRewrite.createPlaceholder(getUnindentedSource(type), ASTRewrite.TYPE_DECLARATION);
			targetRewrite.markAsInserted(newType);
			//always put on top level - we could create member types but that is wrong most of the time
			destinationCuNode.types().add(newType);
		}

		private void copyMethodToDestination(IMethod method, ASTRewrite targetRewrite, CompilationUnit destinationCuNode) throws JavaModelException {
			BodyDeclaration newMethod= (BodyDeclaration) targetRewrite.createPlaceholder(getUnindentedSource(method), ASTRewrite.METHOD_DECLARATION);
			targetRewrite.markAsInserted(newMethod);
			TypeDeclaration targetClass= getTargetType(destinationCuNode);
			targetClass.bodyDeclarations().add(newMethod);
		}

		private static String getUnindentedSource(ISourceReference sourceReference) throws JavaModelException {
			Assert.isTrue(sourceReference instanceof IJavaElement);
			String[] lines= Strings.convertIntoLines(sourceReference.getSource());
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
			return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed((IJavaElement) sourceReference));
		}

		private static AST getAST(ASTRewrite rewrite){
			return rewrite.getRootNode().getAST();
		}
		
		private TypeDeclaration getTargetType(CompilationUnit destinationCuNode) throws JavaModelException {
			return ASTNodeSearchUtil.getTypeDeclarationNode(getDestinationAsType(), destinationCuNode);
		}

		private IType getDestinationAsType() throws JavaModelException {
			IJavaElement destination= getJavaElementDestination();
			IType enclosingType= getEnclosingType(destination);
			if (enclosingType != null)
				return enclosingType;
			ICompilationUnit enclosingCu= getEnclosingCu(destination);
			Assert.isNotNull(enclosingCu);
			IType mainType= JavaElementUtil.getMainType(enclosingCu);
			Assert.isNotNull(mainType);
			return mainType;
		}

		private static ICompilationUnit getEnclosingCu(IJavaElement destination) {
			if (destination instanceof ICompilationUnit)
				return (ICompilationUnit) destination;
			return (ICompilationUnit)destination.getAncestor(IJavaElement.COMPILATION_UNIT);
		}

		private static IType getEnclosingType(IJavaElement destination) {
			if (destination instanceof IType)
				return (IType) destination;
			return (IType)destination.getAncestor(IJavaElement.TYPE);
		}
		
		public boolean canEnable() throws JavaModelException {
			if (! super.canEnable()) return false;
			for (int i= 0; i < fJavaElements.length; i++) {
				if (fJavaElements[i] instanceof IMember){
					IMember member= (IMember) fJavaElements[i];
					//we can copy some binary members, but not all
					if (member.isBinary() && member.getSourceRange() == null)
						return false;
				}
			}
			return true;
		}	
		
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			Assert.isNotNull(destination);
			if (!destination.exists())
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this operation");
			if (! (destination instanceof ICompilationUnit) && ! ReorgUtils2.isInsideCompilationUnit(destination))
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this operation");

			ICompilationUnit destinationCu= getDestinationCu(destination);
			Assert.isNotNull(destinationCu);
			if (destinationCu.isReadOnly())//the resource read-onliness is handled by validateEdit
				return RefactoringStatus.createFatalErrorStatus("The selected destination element cannot be modified");
				
			switch(destination.getElementType()){
				case IJavaElement.COMPILATION_UNIT:
					int[] types0= new int[]{IJavaElement.FIELD, IJavaElement.INITIALIZER, IJavaElement.METHOD};
					if (ReorgUtils2.hasElementsOfType(getJavaElements(), types0))
						return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this operation");				
					break;
				case IJavaElement.PACKAGE_DECLARATION:
					return RefactoringStatus.createFatalErrorStatus("Package declarations are not available as destinations");
				
				case IJavaElement.IMPORT_CONTAINER:
					if (ReorgUtils2.hasElementsNotOfType(getJavaElements(), IJavaElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this operation");
					break;
					
				case IJavaElement.IMPORT_DECLARATION:
					if (ReorgUtils2.hasElementsNotOfType(getJavaElements(), IJavaElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this operation");
					break;
				
				case IJavaElement.FIELD://fall thru
				case IJavaElement.INITIALIZER://fall thru
				case IJavaElement.METHOD://fall thru
					return verifyDestination(destination.getParent());
				
				case IJavaElement.TYPE:
					int[] types1= new int[]{IJavaElement.IMPORT_DECLARATION, IJavaElement.IMPORT_CONTAINER, IJavaElement.PACKAGE_DECLARATION};
					if (ReorgUtils2.hasElementsOfType(getJavaElements(), types1))
						return verifyDestination(destination.getParent());
					break;
			}
				
			return new RefactoringStatus();
		}
	}

	private static abstract class PackageFragmentRootsReorgPolicy extends ReorgPolicy {

		private IPackageFragmentRoot[] fPackageFragmentRoots;

		public IJavaElement[] getJavaElements(){
			return fPackageFragmentRoots;
		}

		public IResource[] getResources() {
			return new IResource[0];
		}

		public PackageFragmentRootsReorgPolicy(IPackageFragmentRoot[] roots){
			Assert.isNotNull(roots);
			fPackageFragmentRoots= roots;
		}
			
		public boolean canEnable() throws JavaModelException {
			if (! super.canEnable()) return false;
			for (int i= 0; i < fPackageFragmentRoots.length; i++) {
				if (! ReorgUtils2.isSourceFolder(fPackageFragmentRoots[i])) return false;
			}
			if (ReorgUtils2.containsLinkedResources(fPackageFragmentRoots)) 
				return false;					
			return true;
		}
	
		protected RefactoringStatus verifyDestination(IResource resource) {
			return RefactoringStatus.createFatalErrorStatus("Source folders can only be copied to Java projects");
		}
			
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this operation");
			if (! (javaElement instanceof IJavaProject))
				return RefactoringStatus.createFatalErrorStatus("Source folders can only be copied to Java projects");
			if (javaElement.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus("Source folders cannot be copied to read-only elements");
			if (ReorgUtils2.isPackageFragmentRoot((IJavaProject)javaElement))
				return RefactoringStatus.createFatalErrorStatus("Source folders cannot be copied or moved to projects that contain no source folders");
			return new RefactoringStatus();
		}
	
		protected IJavaProject getDestinationJavaProject(){
			return (IJavaProject) getJavaElementDestination();
		}
	
		protected IPackageFragmentRoot[] getPackageFragmentRoots(){
			return fPackageFragmentRoots;
		}
		public RefactoringStatus checkInput(IProgressMonitor pm, IReorgQueries reorgQueries) throws JavaModelException {
			 RefactoringStatus status= super.checkInput(pm, reorgQueries);
			 confirmOverwritting(reorgQueries);
			 return status;
		}

		private void confirmOverwritting(IReorgQueries reorgQueries) throws JavaModelException {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setPackageFragmentRoots(fPackageFragmentRoots);
			IJavaProject javaProject= getDestinationJavaProject();
			oh.confirmOverwritting(reorgQueries, javaProject);
			fPackageFragmentRoots= oh.getPackageFragmentRootsWithoutUnconfirmedOnes();
		}
	}

	private static abstract class PackagesReorgPolicy extends ReorgPolicy {
		private IPackageFragment[] fPackageFragments;
	
		public IJavaElement[] getJavaElements(){
			return fPackageFragments;
		}
		
		public IResource[] getResources() {
			return new IResource[0];
		}
		
		protected IPackageFragment[] getPackages(){
			return fPackageFragments;
		}

		public PackagesReorgPolicy(IPackageFragment[] packageFragments){
			Assert.isNotNull(packageFragments);
			fPackageFragments= packageFragments;
		}

		public boolean canEnable() throws JavaModelException {
			for (int i= 0; i < fPackageFragments.length; i++) {
				if (JavaElementUtil.isDefaultPackage(fPackageFragments[i]) || 
					fPackageFragments[i].isReadOnly())
					return false;
			}
			if (ReorgUtils2.containsLinkedResources(fPackageFragments))
				return false;
			return true;
		}

		protected RefactoringStatus verifyDestination(IResource resource) {
			return RefactoringStatus.createFatalErrorStatus("Packages can only be moved or copied to source folders or Java projects that do not have source folders");
		}
		
		protected IPackageFragmentRoot getDestinationAsPackageFragmentRoot() throws JavaModelException {
			return getDestinationAsPackageFragmentRoot(getJavaElementDestination());
		}
		
		private IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaElement javaElement) throws JavaModelException {
			if (javaElement == null)
				return null;
				
			if (javaElement instanceof IPackageFragmentRoot)
				return (IPackageFragmentRoot) javaElement;

			if (javaElement instanceof IPackageFragment){
				IPackageFragment pack= (IPackageFragment)javaElement;
				if (pack.getParent() instanceof IPackageFragmentRoot)
					return (IPackageFragmentRoot) pack.getParent();
			}

			if (javaElement instanceof IJavaProject)
				return ReorgUtils2.getCorrespondingPackageFragmentRoot((IJavaProject) javaElement);				
			return null;
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this operation");
			IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot(javaElement);
			if (! ReorgUtils2.isSourceFolder(destRoot))
				return RefactoringStatus.createFatalErrorStatus("Packages can only be moved or copied to source folders or Java projects that do not have source folders");
			return new RefactoringStatus();
		}
		
		public RefactoringStatus checkInput(IProgressMonitor pm, IReorgQueries reorgQueries) throws JavaModelException {
			RefactoringStatus refactoringStatus= super.checkInput(pm, reorgQueries);
			confirmOverwritting(reorgQueries);
			return refactoringStatus;
		}
		
		private void confirmOverwritting(IReorgQueries reorgQueries) throws JavaModelException {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setPackages(fPackageFragments);
			IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot();
			oh.confirmOverwritting(reorgQueries, destRoot);
			fPackageFragments= oh.getPackagesWithoutUnconfirmedOnes();
		}
	}

	private static class CopySubCuElementsPolicy extends SubCuElementReorgPolicy implements ICopyPolicy{
		CopySubCuElementsPolicy(IJavaElement[] javaElements){
			super(javaElements);
		}
			
		public IChange createChange(IProgressMonitor pm, INewNameQueries copyQueries) throws JavaModelException {
			try {					
				CompilationUnit sourceCuNode= createSourceCuNode();
				ICompilationUnit destinationCu= getDestinationCu();
				CompilationUnit destinationCuNode= AST.parseCompilationUnit(destinationCu, false);
				ASTRewrite rewrite= new ASTRewrite(destinationCuNode);
				IJavaElement[] javaElements= getJavaElements();
				for (int i= 0; i < javaElements.length; i++) {
					copyToDestination(javaElements[i], rewrite, sourceCuNode, destinationCuNode);
				}
				return addTextEditFromRewrite(destinationCu, rewrite);
			} catch (JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private CompilationUnit createSourceCuNode(){
			Assert.isTrue(getSourceCu() != null || getSourceClassFile() != null);
			Assert.isTrue(getSourceCu() == null || getSourceClassFile() == null);
			if (getSourceCu() != null)
				return AST.parseCompilationUnit(getSourceCu(), false);
			else
				return AST.parseCompilationUnit(getSourceClassFile(), false);
		}

		private IClassFile getSourceClassFile() {
			//all have a common parent, so all must be in the same classfile
			//we checked before that the array in not null and not empty
			return (IClassFile) getJavaElements()[0].getAncestor(IJavaElement.CLASS_FILE);
		}
			
		public boolean canEnable() throws JavaModelException {
			return 	super.canEnable() && 
					(getSourceCu() != null || getSourceClassFile() != null);
		}
			
		public IFile[] getAllModifiedFiles() {
			return ReorgUtils2.getFiles(new IResource[]{ReorgUtils2.getResource(getDestinationCu())});
		}
	}
	private static class CopyFilesFoldersAndCusPolicy extends FilesFoldersAndCusReorgPolicy implements ICopyPolicy{

		CopyFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
			super(files, folders, cus);
		}
				
		public IChange createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			IFile[] file= getFiles();
			IFolder[] folders= getFolders();
			ICompilationUnit[] cus= getCus();
			pm.beginTask("", cus.length + file.length + folders.length);
			NewNameProposer nameProposer= new NewNameProposer();
			CompositeChange composite= new CompositeChange();
			for (int i= 0; i < cus.length; i++) {
				composite.add(createChange(cus[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			for (int i= 0; i < file.length; i++) {
				composite.add(createChange(file[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			for (int i= 0; i < folders.length; i++) {
				composite.add(createChange(folders[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		private IChange createChange(ICompilationUnit unit, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null)
				return copyCuToPackage(unit, pack, nameProposer, copyQueries);
			IContainer container= getDestinationAsContainer();
			Assert.isNotNull(container);
			return copyFileToContainer(unit, container, nameProposer, copyQueries);
		}
	
		private static IChange copyFileToContainer(ICompilationUnit cu, IContainer dest, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IResource resource= ReorgUtils2.getResource(cu);
			return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
		}

		private IChange createChange(IResource resource, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IContainer dest= getDestinationAsContainer();
			return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
		}
			
		private static IChange createCopyResourceChange(IResource resource, NewNameProposer nameProposer, INewNameQueries copyQueries, IContainer destination) {
			INewNameQuery nameQuery;
			String name= nameProposer.createNewName(resource, destination);
			if (name == null)
				nameQuery= copyQueries.createNullQuery();
			else
				nameQuery= copyQueries.createNewResourceNameQuery(resource, name);
			return new CopyResourceChange(resource, destination, nameQuery);
		}

		private static IChange copyCuToPackage(ICompilationUnit cu, IPackageFragment dest, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
			IResource res= ReorgUtils2.getResource(cu);
			if (res != null && res.isLinked()){
				if (ResourceUtil.getResource(dest) instanceof IContainer)
					return copyFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest), nameProposer, copyQueries);
			}
		
			String newName= nameProposer.createNewName(cu, dest);
			Change simpleCopy= new CopyCompilationUnitChange(cu, dest, copyQueries.createStaticQuery(newName));
			if (newName == null || newName.equals(cu.getElementName()))
				return simpleCopy;
		
			try {
				IPath newPath= ResourceUtil.getResource(cu).getParent().getFullPath().append(newName);				
				INewNameQuery nameQuery= copyQueries.createNewCompilationUnitNameQuery(cu, newName);
				return new CreateCopyOfCompilationUnitChange(newPath, cu.getSource(), cu, nameQuery);
			} catch(CoreException e) {
				return simpleCopy; //fallback - no ui here
			}
		}
	}
	private static class CopyPackageFragmentRootsPolicy extends PackageFragmentRootsReorgPolicy implements ICopyPolicy{
		public CopyPackageFragmentRootsPolicy(IPackageFragmentRoot[] roots){
			super(roots);
		}
		
		public IChange createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			NewNameProposer nameProposer= new NewNameProposer();
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			pm.beginTask("", roots.length);
			CompositeChange composite= new CompositeChange();
			IJavaProject destination= getDestinationJavaProject();
			Assert.isNotNull(destination);
			for (int i= 0; i < roots.length; i++) {
				composite.add(createChange(roots[i], destination, nameProposer, copyQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}
		
		private IChange createChange(IPackageFragmentRoot root, IJavaProject destination, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IResource res= root.getResource();
			IProject destinationProject= destination.getProject();
			String newName= nameProposer.createNewName(res, destinationProject);
			INewNameQuery nameQuery;
			if (newName == null )
				nameQuery= copyQueries.createNullQuery();
			else
				nameQuery= copyQueries.createNewPackageFragmentRootNameQuery(root, newName);
			//TODO sounds wrong that this change works on IProjects
			//TODO fix the query problem
			return new CopyPackageFragmentRootChange(root, destinationProject, nameQuery,  null);
		}
	}
	private static class CopyPackagesPolicy extends PackagesReorgPolicy implements ICopyPolicy{

		public CopyPackagesPolicy(IPackageFragment[] packageFragments){
			super(packageFragments);
		}

		public IChange createChange(IProgressMonitor pm, INewNameQueries newNameQueries) throws JavaModelException {
			NewNameProposer nameProposer= new NewNameProposer();
			IPackageFragment[] fragments= getPackages();
			pm.beginTask("", fragments.length);
			CompositeChange composite= new CompositeChange();
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < fragments.length; i++) {
				composite.add(createChange(fragments[i], root, nameProposer, newNameQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		private IChange createChange(IPackageFragment pack, IPackageFragmentRoot destination, NewNameProposer nameProposer, INewNameQueries copyQueries) throws JavaModelException {
			String newName= nameProposer.createNewName(pack, destination);
			if (newName == null || JavaConventions.validatePackageName(newName).getSeverity() < IStatus.ERROR){
				INewNameQuery nameQuery;
				if (newName == null)
					nameQuery= copyQueries.createNullQuery();
				else
					nameQuery= copyQueries.createNewPackageNameQuery(pack, newName);
				return new CopyPackageChange(pack, destination, nameQuery);
			} else {
				if (destination.getResource() instanceof IContainer){
					IContainer dest= (IContainer)destination.getResource();
					IResource res= pack.getResource();
					INewNameQuery nameQuery= copyQueries.createNewResourceNameQuery(res, newName);
					return new CopyResourceChange(res, dest, nameQuery);
				}else
					return new NullChange();
			}	
		}
	}
	private static class NoCopyPolicy extends ReorgPolicy implements ICopyPolicy{
		public boolean canEnable() throws JavaModelException {
			return false;
		}
		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus("Copying is not available");
		}
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus("Copying is not available");
		}
		public IChange createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			return new NullChange();
		}
		public IResource[] getResources() {
			return new IResource[0];
		}
		public IJavaElement[] getJavaElements() {
			return new IJavaElement[0];
		}
	}
	private static class NewNameProposer{
		private final Set fAutoGeneratedNewNames= new HashSet(2);
			
		public String createNewName(ICompilationUnit cu, IPackageFragment destination){
			if (isNewNameOk(destination, cu.getElementName()))
				return null;
			if (! ReorgUtils2.isParentInWorkspaceOrOnDisk(cu, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 1)
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.cu.copyOf1", //$NON-NLS-1$
								cu.getElementName());
				else	
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.cu.copyOfMore", //$NON-NLS-1$
								new String[]{String.valueOf(i), cu.getElementName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return removeTrailingJava(newName);
				}
				i++;
			}
		}
		private static String removeTrailingJava(String name) {
			Assert.isTrue(name.endsWith(".java")); //$NON-NLS-1$
			return name.substring(0, name.length() - ".java".length()); //$NON-NLS-1$
		}
		public String createNewName(IResource res, IContainer destination){
			if (isNewNameOk(destination, res.getName()))
				return null;
			if (! ReorgUtils2.isParentInWorkspaceOrOnDisk(res, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 1)
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.resource.copyOf1", //$NON-NLS-1$
								res.getName());
				else
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.resource.copyOfMore", //$NON-NLS-1$
								new String[]{String.valueOf(i), res.getName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return newName;
				}
				i++;
			}	
		}
	
		public String createNewName(IPackageFragment pack, IPackageFragmentRoot destination){
			if (isNewNameOk(destination, pack.getElementName()))
				return null;
			if (! ReorgUtils2.isParentInWorkspaceOrOnDisk(pack, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 0)
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.package.copyOf1", //$NON-NLS-1$
								pack.getElementName());
				else
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.package.copyOfMore", //$NON-NLS-1$
								new String[]{String.valueOf(i), pack.getElementName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return newName;
				}
				i++;
			}	
		}
		private static boolean isNewNameOk(IPackageFragment dest, String newName) {
			return ! dest.getCompilationUnit(newName).exists();
		}
	
		private static boolean isNewNameOk(IContainer container, String newName) {
			return container.findMember(newName) == null;
		}

		private static boolean isNewNameOk(IPackageFragmentRoot root, String newName) {
			return ! root.getPackageFragment(newName).exists() ;
		}
	}

	private static class MovePackageFragmentRootsPolicy extends PackageFragmentRootsReorgPolicy implements IMovePolicy{
			
		MovePackageFragmentRootsPolicy(IPackageFragmentRoot[] roots){
			super(roots);
		}
		public IChange createChange(IProgressMonitor pm) throws JavaModelException {
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			pm.beginTask("", roots.length);
			CompositeChange composite= new CompositeChange();
			IJavaProject destination= getDestinationJavaProject();
			Assert.isNotNull(destination);
			for (int i= 0; i < roots.length; i++) {
				composite.add(createChange(roots[i], destination));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		private IChange createChange(IPackageFragmentRoot root, IJavaProject destination) {
			///XXX fix the query
			return new MovePackageFragmentRootChange(root, destination.getProject(), null);
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(javaElement);
			if (superStatus.hasFatalError())
				return superStatus;
			IJavaProject javaProject= getDestinationJavaProject();
			if (isParentOfAny(javaProject, getPackageFragmentRoots()))
				return RefactoringStatus.createFatalErrorStatus("The selected project cannot be the destination of this move operation.");
			return superStatus;
		}

		private static boolean isParentOfAny(IJavaProject javaProject, IPackageFragmentRoot[] roots) {
			for (int i= 0; i < roots.length; i++) {
				if (ReorgUtils2.isParentInWorkspaceOrOnDisk(roots[i], javaProject)) return true;
			}
			return false;
		}
		
		public boolean canEnable() throws JavaModelException {
			if (! super.canEnable()) return false;
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (roots[i].isReadOnly())	return  false;
			}
			return true;
		}

		public RefactoringStatus checkInput(IProgressMonitor pm, IReorgQueries reorgQueries) throws JavaModelException {
			try{
				RefactoringStatus status= super.checkInput(pm, reorgQueries);
				confirmMovingReadOnly(reorgQueries);
				return status;
			} catch(JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}		
	}
	
	private static class MovePackagesPolicy extends PackagesReorgPolicy implements IMovePolicy{

		MovePackagesPolicy(IPackageFragment[] packageFragments){
			super(packageFragments);
		}
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(javaElement);
			if (superStatus.hasFatalError())
				return superStatus;
			
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			if (isParentOfAny(root, getPackages()))
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this move operation.");
			return superStatus;
		}
		
		private static boolean isParentOfAny(IPackageFragmentRoot root, IPackageFragment[] fragments) {
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				if (ReorgUtils2.isParentInWorkspaceOrOnDisk(fragment, root)) return true;
			}
			return false;
		}

		public IChange createChange(IProgressMonitor pm) throws JavaModelException {
			IPackageFragment[] fragments= getPackages();
			pm.beginTask("", fragments.length);
			CompositeChange result= new CompositeChange();
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < fragments.length; i++) {
				result.add(createChange(fragments[i], root));
				pm.worked(1);
			}
			pm.done();
			return result;
		}

		private IChange createChange(IPackageFragment pack, IPackageFragmentRoot destination) {
			return new MovePackageChange(pack, destination);
		}
		
		public RefactoringStatus checkInput(IProgressMonitor pm, IReorgQueries reorgQueries) throws JavaModelException {
			try{
				RefactoringStatus status= super.checkInput(pm, reorgQueries);
				confirmMovingReadOnly(reorgQueries);
				return status;
			} catch(JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}		
	}
	
	private static class MoveFilesFoldersAndCusPolicy extends FilesFoldersAndCusReorgPolicy implements IMovePolicy{
		
		private boolean fUpdateReferences;
		private boolean fUpdateQualifiedNames;
		private QualifiedNameSearchResult fQualifiedNameSearchResult;
		private String fFilePatterns;
		private TextChangeManager fChangeManager;
		private final CodeGenerationSettings fSettings;

		MoveFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus, CodeGenerationSettings settings){
			super(files, folders, cus);
			Assert.isNotNull(settings);
			fUpdateReferences= true;
			fUpdateQualifiedNames= false;
			fQualifiedNameSearchResult= new QualifiedNameSearchResult();
			fSettings= settings;
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= new ParentChecker(getResources(), getJavaElements()).getCommonParent();
			if (destination.equals(commonParent)) 
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this move operation.");
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer != null && destinationAsContainer.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this move operation.");
			IPackageFragment destinationAsPackage= getDestinationAsPackageFragment();
			if (destinationAsPackage != null && destinationAsPackage.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this move operation.");
				
			return superStatus;
		}
		
		protected RefactoringStatus verifyDestination(IResource destination) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= getCommonParent();
			if (destination.equals(commonParent)) 
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this move operation.");
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer != null && destinationAsContainer.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this move operation.");
			IPackageFragment destinationAsPackage= getDestinationAsPackageFragment();
			if (destinationAsPackage != null && destinationAsPackage.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination of this move operation.");

			return superStatus;
		}
		
		private Object getCommonParent(){
			return new ParentChecker(getResources(), getJavaElements()).getCommonParent();
		}

		public IChange createChange(IProgressMonitor pm) throws JavaModelException {
			if (! fUpdateReferences){
				if (fUpdateQualifiedNames)
					return createMoveAndUpdateQualifiedNameChange(pm);
				return createSimpleMoveChange(pm);
			} else 
				return createReferenceUpdatingMoveChange(pm);
		}

		private IChange createReferenceUpdatingMoveChange(IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 2 + (fUpdateQualifiedNames ? 1 : 0)); //$NON-NLS-1$
			try{
				CompositeChange composite= new CompositeChange();
				//XX workaround for bug 13558
				//<workaround>
				if (fChangeManager == null){
					fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
					RefactoringStatus status= Checks.validateModifiesFiles(getAllModifiedFiles());;
					if (status.hasFatalError())
						fChangeManager= new TextChangeManager();
				}	
				//</workaround>
							
				addAllChildren(composite, new CompositeChange(RefactoringCoreMessages.getString("MoveRefactoring.reorganize_elements"), fChangeManager.getAllChanges())); //$NON-NLS-1$
						
				if (fUpdateQualifiedNames) {
					computeQualifiedNameMatches(new SubProgressMonitor(pm, 1));
					composite.addAll(fQualifiedNameSearchResult.getAllChanges());
				}
							
				IChange fileMove= createSimpleMoveChange(new SubProgressMonitor(pm, 1));
				if (fileMove instanceof ICompositeChange) {
					addAllChildren(composite, (ICompositeChange)fileMove);		
				} else{
					composite.add(fileMove);
				}
				return composite;
			} finally{
				pm.done();
			}
		}

		private TextChangeManager createChangeManager(IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 1);//$NON-NLS-1$
			try{
				if (! fUpdateReferences)
					return new TextChangeManager();
			
				IPackageFragment packageDest= getDestinationAsPackageFragment();
				if (packageDest != null){			
					MoveCuUpdateCreator creator= new MoveCuUpdateCreator(getCus(), packageDest, fSettings);
					return creator.createChangeManager(new SubProgressMonitor(pm, 1));
				} else 
					return new TextChangeManager();
			} finally {
				pm.done();
			}		
		}

		private IChange createMoveAndUpdateQualifiedNameChange(IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 2);
			IChange simpleCopyChange= createSimpleMoveChange(new SubProgressMonitor(pm, 1));
			CompositeChange result= new CompositeChange(RefactoringCoreMessages.getString("MoveRefactoring.reorganize_elements"), 2) { //$NON-NLS-1$
				public boolean isUndoable(){
					return false; 
				}
			};
			if (simpleCopyChange instanceof ICompositeChange) {
				addAllChildren(result, (ICompositeChange)simpleCopyChange);		
			} else {
				result.add(simpleCopyChange); 
			}
			computeQualifiedNameMatches(new SubProgressMonitor(pm, 1));
			result.addAll(fQualifiedNameSearchResult.getAllChanges());
			return result;
		}

		private IChange createSimpleMoveChange(IProgressMonitor pm) throws JavaModelException {
			CompositeChange result= new CompositeChange();
			IFile[] files= getFiles();
			IFolder[] folders= getFolders();
			ICompilationUnit[] cus= getCus();
			pm.beginTask("", files.length + folders.length + cus.length);
			for (int i= 0; i < files.length; i++) {
				result.add(createChange(files[i]));
				pm.worked(1);
			}
			for (int i= 0; i < folders.length; i++) {
				result.add(createChange(folders[i]));
				pm.worked(1);
			}
			for (int i= 0; i < cus.length; i++) {
				result.add(createChange(cus[i]));
				pm.worked(1);
			}
			pm.done();
			return result;
		}

		private IChange createChange(ICompilationUnit cu) throws JavaModelException{
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null)
				return moveCuToPackage(cu, pack);
			IContainer container= getDestinationAsContainer();
			Assert.isNotNull(container);
			return moveFileToContainer(cu, container);
		}

		private static IChange moveCuToPackage(ICompilationUnit cu, IPackageFragment dest) {
			//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
			IResource resource= ResourceUtil.getResource(cu);
			if (resource != null && resource.isLinked()){
				if (ResourceUtil.getResource(dest) instanceof IContainer)
					return moveFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest));
			}
			return new MoveCompilationUnitChange(cu, dest);
		}

		private static IChange moveFileToContainer(ICompilationUnit cu, IContainer dest) {
			return new MoveResourceChange(ResourceUtil.getResource(cu), dest);
		}

		private IChange createChange(IResource res) throws JavaModelException{
			return new MoveResourceChange(res, getDestinationAsContainer());
		}

		private static void addAllChildren(CompositeChange collector, ICompositeChange composite){
			collector.addAll(composite.getChildren());
		}

		private void computeQualifiedNameMatches(IProgressMonitor pm) throws JavaModelException {
			if (!fUpdateQualifiedNames)
				return;
			IPackageFragment destination= getDestinationAsPackageFragment();
			
			ICompilationUnit[] cus= getCus();
			pm.beginTask("", cus.length); //$NON-NLS-1$
			pm.subTask(RefactoringCoreMessages.getString("MoveRefactoring.scanning_qualified_names")); //$NON-NLS-1$
			for (int i= 0; i < cus.length; i++) {
				ICompilationUnit cu= cus[i];
				IType[] types= cu.getTypes();
				IProgressMonitor typesMonitor= new SubProgressMonitor(pm, 1);
				typesMonitor.beginTask("", types.length); //$NON-NLS-1$
				for (int j= 0; j < types.length; j++) {
					handleType(types[j], destination, new SubProgressMonitor(typesMonitor, 1));
				}
				typesMonitor.done();
			}
			pm.done();
		}

		private void handleType(IType type, IPackageFragment destination, IProgressMonitor pm) throws JavaModelException {
			fQualifiedNameSearchResult=
				QualifiedNameFinder.process(type.getFullyQualifiedName(),  destination.getElementName() + "." + type.getTypeQualifiedName(), //$NON-NLS-1$
						fFilePatterns, type.getJavaProject().getProject(), pm);
		}	
		
		public RefactoringStatus checkInput(IProgressMonitor pm, IReorgQueries reorgQueries) throws JavaModelException {
			try{
				pm.beginTask("", 2);
				confirmMovingReadOnly(reorgQueries);
				fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
				RefactoringStatus status= super.checkInput(new SubProgressMonitor(pm, 1), reorgQueries);
				return status;
			} catch (JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			} finally{
				pm.done();
			}
		}
		
		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}		
		
		public IFile[] getAllModifiedFiles() {
			List result= new ArrayList();
			result.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
			result.addAll(Arrays.asList(fQualifiedNameSearchResult.getAllFiles()));
			if (getDestinationAsPackageFragment() != null)
				result.addAll(Arrays.asList(ResourceUtil.getFiles(getCus())));
			return (IFile[]) result.toArray(new IFile[result.size()]);
		}
		public boolean hasAllInputSet() {
			return super.hasAllInputSet() && ! canUpdateReferences() && ! canUpdateQualifiedNames();
		}
		public boolean canUpdateReferences(){
			if (getCus().length == 0)
				return false;
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack == null || pack.isDefaultPackage())
				return false;
			Object commonParent= getCommonParent();
			if (JavaElementUtil.isDefaultPackage(commonParent))
				return false;
			return true;							
		}
		public boolean getUpdateReferences() {
			return fUpdateReferences;
		}
		public void setUpdateReferences(boolean update) {
			fUpdateReferences= update;
		}
		
		public boolean canEnableQualifiedNameUpdating() {
			return getCus().length > 0 && ! JavaElementUtil.isDefaultPackage(getCommonParent()) ;
		}
		
		public boolean canUpdateQualifiedNames() {
			IPackageFragment pack= getDestinationAsPackageFragment();
			return (canEnableQualifiedNameUpdating() && pack != null && !pack.isDefaultPackage());
		}

		public boolean getUpdateQualifiedNames() {
			return fUpdateQualifiedNames;
		}

		public void setUpdateQualifiedNames(boolean update) {
			fUpdateQualifiedNames= update;
		}

		public String getFilePatterns() {
			return fFilePatterns;
		}

		public void setFilePatterns(String patterns) {
			Assert.isNotNull(patterns);
			fFilePatterns= patterns;
		}
	}
	
	private static class MoveSubCuElementsPolicy extends SubCuElementReorgPolicy implements IMovePolicy{
		MoveSubCuElementsPolicy(IJavaElement[] javaElements){
			super(javaElements);
		}
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;
				
			Object commonParent= new ParentChecker(new IResource[0], getJavaElements()).getCommonParent();
			if (destination.equals(commonParent) || destination.getParent().equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus("The selected elements cannot be the destination of this move operation.");
			return superStatus;
		}

		private CompilationUnit createSourceCuNode(){
			return AST.parseCompilationUnit(getSourceCu(), false);
		}

		public IChange createChange(IProgressMonitor pm) throws JavaModelException {
			try {
				CompilationUnit sourceCuNode= createSourceCuNode();
				ICompilationUnit destinationCu= getDestinationCu();
				CompilationUnit destinationCuNode= getSourceCu().equals(destinationCu) ? sourceCuNode: AST.parseCompilationUnit(destinationCu, false);
				ASTRewrite targetRewrite= new ASTRewrite(destinationCuNode);
				IJavaElement[] javaElements= getJavaElements();
				for (int i= 0; i < javaElements.length; i++) {
					copyToDestination(javaElements[i], targetRewrite, sourceCuNode, destinationCuNode);
				}
				ASTRewrite sourceRewrite= getSourceCu().equals(destinationCu) ? targetRewrite: new ASTRewrite(sourceCuNode);
				ASTNodeDeleteUtil.markAsDeleted(javaElements, sourceCuNode, sourceRewrite);
				IChange targetCuChange= addTextEditFromRewrite(destinationCu, targetRewrite);
				if (getSourceCu().equals(destinationCu)){
					return targetCuChange;
				} else{
					CompositeChange result= new CompositeChange();
					result.add(targetCuChange);
					if (Arrays.asList(getJavaElements()).containsAll(Arrays.asList(getSourceCu().getTypes())))
						result.add(DeleteChangeCreator.createDeleteChange(null, new IResource[0], new ICompilationUnit[]{getSourceCu()}));
					else
						result.add(addTextEditFromRewrite(getSourceCu(), sourceRewrite));						
					return result;
				}
			} catch (JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		public boolean canEnable() throws JavaModelException {
			return super.canEnable() && getSourceCu() != null; //can move only elements from cus
		}

		public IFile[] getAllModifiedFiles() {
			return ReorgUtils2.getFiles(new IResource[]{ReorgUtils2.getResource(getSourceCu()), ReorgUtils2.getResource(getDestinationCu())});
		}
	}
		
	private static class NoMovePolicy extends ReorgPolicy implements IMovePolicy{
		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus("Moving is not available");
		}
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus("Moving is not available");
		}
		public IChange createChange(IProgressMonitor pm) {
			return new NullChange();
		}
		public boolean canEnable() throws JavaModelException {
			return false;
		}
		public IResource[] getResources() {
			return new IResource[0];
		}
		public IJavaElement[] getJavaElements() {
			return new IJavaElement[0];
		}
	}
	
	private static class ActualSelectionComputer {
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;

		public ActualSelectionComputer(IJavaElement[]  javaElements, IResource[] resources) {
			fJavaElements= javaElements;
			fResources= resources;
		}
		
		public IJavaElement[] getActualJavaElementsToReorg() throws JavaModelException {
			Set result= new HashSet();
			for (int i= 0; i < fJavaElements.length; i++) {
				IJavaElement element= fJavaElements[i];
				if (element == null)
					continue;
				if (ReorgUtils2.isDeletedFromEditor(element))
					continue;
				element= WorkingCopyUtil.getWorkingCopyIfExists(element);
				if (element instanceof IType) {
					IType type= (IType)element;
					ICompilationUnit cu= type.getCompilationUnit();
					if (cu != null && type.getDeclaringType() == null && cu.exists() && cu.getTypes().length == 1)
						result.add(cu);
					else
						result.add(type);
				} else  {
					result.add(element);
				}
			}
			return (IJavaElement[]) result.toArray(new IJavaElement[result.size()]);
		}
		
		public IResource[] getActualResourcesToReorg() {
			Set javaElementSet= new HashSet(Arrays.asList(fJavaElements));	
			Set result= new HashSet();
			for (int i= 0; i < fResources.length; i++) {
				if (fResources[i] == null)
					continue;
				IJavaElement element= JavaCore.create(fResources[i]);
				if (element == null || ! element.exists() || ! javaElementSet.contains(element))
					result.add(fResources[i]);
			}
			return (IResource[]) result.toArray(new IResource[result.size()]);

		}
	}
}