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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.Set;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

public class NLSRefactoring extends Refactoring {
	
	public static final String KEY= "${key}"; //$NON-NLS-1$
	public static final String PROPERTY_FILE_EXT= ".properties"; //$NON-NLS-1$
	private static final String fgLineDelimiter= System.getProperty("line.separator", "\n"); //$NON-NLS-2$ //$NON-NLS-1$

	private String fAccessorClassName= "Messages";  //$NON-NLS-1$
	
	private boolean fCreateAccessorClass= true;
	private String fProperyFileName= "test"; //simple name //$NON-NLS-1$
	private String fCodePattern;
	private ICompilationUnit fCu;
	private NLSLine[] fLines;
	private NLSSubstitution[] fNlsSubs;
	private IPath fPropertyFilePath;
	private String fAddedImport;
	private final CodeGenerationSettings fCodeGenerationSettings;
	
	private NLSRefactoring(ICompilationUnit cu, CodeGenerationSettings codeGenerationSettings){
		Assert.isNotNull(cu);
		Assert.isNotNull(codeGenerationSettings);
		fCu= cu;
		fCodeGenerationSettings= codeGenerationSettings;
	}
		
	public static NLSRefactoring create(ICompilationUnit cu, CodeGenerationSettings codeGenerationSettings){
		if (! isAvailable(cu))
			return null;
		return new NLSRefactoring(cu, codeGenerationSettings);
	}
	
	public static boolean isAvailable(ICompilationUnit cu){
		if (cu == null)
			return false;
		if (! cu.exists())
			return false;	
		return true;
	}
	
	public void setNlsSubstitutions(NLSSubstitution[] subs){
		Assert.isNotNull(subs);
		fNlsSubs= subs;
	}
	
	/**
	 * sets the import to be added
	 * @param decl must be a valid import declaration
	 * otherwise no import declaration will be added
	 * @see JavaConventions#validateImportDeclaration(java.lang.String)
	 */
	public void setAddedImportDeclaration(String decl){
		if (JavaConventions.validateImportDeclaration(decl).isOK())
			fAddedImport= decl;
		else
			fAddedImport= null;	
	}
	
	/**
	 * no validation is done
	 * @param pattern Example: "Messages.getString(${key})". Must not be <code>null</code>.
	 * should (but does not have to) contain NLSRefactoring.KEY (default value is $key$)
	 * only the first occurrence of this key will be used
	 */
	public void setCodePattern(String pattern){
		Assert.isNotNull(pattern);
		fCodePattern= pattern;
	}
	
	/**
	 * to show the pattern in the ui
	 */
	public String getCodePattern(){
		if (fCodePattern == null)
			return getDefaultCodePattern();
		return fCodePattern;
	}
	
	public String getDefaultCodePattern(){
		return fAccessorClassName + ".getString(" + KEY + ")"; //$NON-NLS-2$ //$NON-NLS-1$
	}
		
	public ICompilationUnit getCu() {
		return fCu;
	}
	
	public String getName() {
		return NLSMessages.getFormattedString("NLSrefactoring.compilation_unit", fCu.getElementName());//$NON-NLS-1$
	}
	
	/**
	 * sets the list of lines
	 * @param List of NLSLines
	 */
	public void setLines(NLSLine[] lines) {
		Assert.isNotNull(lines);
		fLines= lines;
	}
	
	/**
	 * no validation done here
	 * full path expected
	 * can be null - the default value will be used
	 * to ask what the default value is - use 
	 * getDefaultPropertyFileName to get the file name
	 * getDefaultPropertyPackageName to get the package name
	 */
	public void setPropertyFilePath(IPath path){
		fPropertyFilePath= path;
	}
	
	private IPath getPropertyFilePath(){
		if (fPropertyFilePath == null)
			return getDefaultPropertyFilePath();
		return fPropertyFilePath;	
	}
	
	private IPath getDefaultPropertyFilePath(){
		IPath cuName= new Path(fCu.getElementName());
		return ResourceUtil.getResource(fCu).getFullPath()
						  .removeLastSegments(cuName.segmentCount())
						  .append(fProperyFileName + PROPERTY_FILE_EXT);
	}
	
	public String getDefaultPropertyFileName(){
		return getDefaultPropertyFilePath().lastSegment();	
	}
	
	/**
	 * returns "" in case of JavaModelException caught during calculation
	 */
	public String getDefaultPropertyPackageName(){
		IPath path= getDefaultPropertyFilePath();
		IResource res= ResourcesPlugin.getWorkspace().getRoot().findMember(path.removeLastSegments(1));
		IJavaElement je= JavaCore.create(res);
		if (je instanceof IPackageFragment)
			return je.getElementName();
		else	
			return ""; //$NON-NLS-1$	
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		if (NLSHolder.create(fCu).getSubstitutions().length == 0)	{
			String message= NLSMessages.getFormattedString("NLSRefactoring.no_strings", fCu.getElementName());//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		return new RefactoringStatus();
	}
	
	/**
	 * @see Refactoring#checkFinalConditions(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(NLSMessages.getString("NLSrefactoring.checking"), 7); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkIfAnythingToDo());
			if (result.hasFatalError())	
				return result;
			pm.worked(1);
			
			result.merge(validateModifiesFiles());
			if (result.hasFatalError())	
				return result;
			pm.worked(1);
			
			result.merge(checkCodePattern());
			pm.worked(1);
			result.merge(checkForDuplicateKeys());
			pm.worked(1);
			result.merge(checkForKeysAlreadyDefined());
			pm.worked(1);
			result.merge(checkKeys());
			pm.worked(1);
			if (!propertyFileExists() && willModifyPropertyFile()){
				String msg= NLSMessages.getFormattedString("NLSrefactoring.will_be_created", getPropertyFilePath().toString()); //$NON-NLS-1$
				result.addInfo(msg);
			}
			pm.worked(1);	
			return result;
		} finally {
			pm.done();
		}	
	}

	private IFile[] getAllFilesToModify(){
		List files= new ArrayList(2);
		if (willModifySource()){
			IFile file= ResourceUtil.getFile(fCu);
			if (file != null)
				files.add(file);
		}	
		
		if (willModifyPropertyFile() && propertyFileExists())
			files.add(getPropertyFile());
			
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}
	
	private RefactoringStatus validateModifiesFiles(){
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	//should stop checking if fatal error
	private RefactoringStatus checkIfAnythingToDo() throws JavaModelException{
		if (willCreateAccessorClass())
			return null;
		if (willModifyPropertyFile())
			return null;
		if (willModifySource())
			return null;	
			
		RefactoringStatus result= new RefactoringStatus();
		result.addFatalError(NLSMessages.getString("NLSrefactoring.nothing_to_do")); //$NON-NLS-1$
		return result;
	}
	
	private boolean propertyFileExists(){
		return Checks.resourceExists(getPropertyFilePath());
	}
	
	private RefactoringStatus checkCodePattern(){
		String pattern= getCodePattern();
		RefactoringStatus result= new RefactoringStatus();
		if ("".equals(pattern.trim())) //$NON-NLS-1$
			result.addError(NLSMessages.getString("NLSrefactoring.pattern_empty")); //$NON-NLS-1$
		if (pattern.indexOf(KEY) == -1){
			String msg= NLSMessages.getFormattedString("NLSrefactoring.pattern_does_not_contain", KEY); //$NON-NLS-1$
			result.addWarning(msg);
		}
		if (pattern.indexOf(KEY) != pattern.lastIndexOf(KEY)){
			String msg= NLSMessages.getFormattedString("NLSrefactoring.Only_the_first_occurrence_of", KEY);//$NON-NLS-1$
			result.addWarning(msg);
		}
		return result;	
	}

	private RefactoringStatus checkForKeysAlreadyDefined() throws JavaModelException {
		if (! propertyFileExists())
			return null;
		RefactoringStatus result= new RefactoringStatus();
		PropertyResourceBundle bundle= getPropertyBundle();
		if (bundle == null)
			return null;
		for (int i= 0; i< fNlsSubs.length; i++){
			String s= getBundleString(bundle, fNlsSubs[i].key);
			if (s != null){
				if (! hasSameValue(s, fNlsSubs[i])){
					String[] args= {fNlsSubs[i].key, s, fNlsSubs[i].value.getValue()};
					String msg= NLSMessages.getFormattedString("NLSrefactoring.already_exists", args);	 //$NON-NLS-1$
					result.addFatalError(msg);
				}
				else{
					fNlsSubs[i].putToPropertyFile= false;
					String[] args= {fNlsSubs[i].key, s};
					String msg= NLSMessages.getFormattedString("NLSrefactoring.already_in_bundle", args); //$NON-NLS-1$
					result.addWarning(msg);
				}	
			}
		}
		return result;
	}
	
	private boolean hasSameValue(String val, NLSSubstitution sub){
		return (val.equals(sub.value.getValue()));
	}
	
	/**
	 * returns <code>null</code> if not defined
	 */
	private String getBundleString(PropertyResourceBundle bundle, String key){
		try{
			return bundle.getString(key);
		} catch (MissingResourceException e){
			return null;	
		}
	}
	
	private PropertyResourceBundle getPropertyBundle() throws JavaModelException{
		InputStream is= getPropertyFileInputStream();
		if (is == null)
			return null;
		try{
			PropertyResourceBundle result= new PropertyResourceBundle(is);
			return result;
		} catch (IOException e1){	
			return null;
		}finally {
			try{
				is.close();
			} catch (IOException e){
				throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
			}
		}	
	}
	
	private InputStream getPropertyFileInputStream() throws JavaModelException{
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(getPropertyFilePath());
		
		try{
			return file.getContents();
		} catch(CoreException e){
			throw new JavaModelException(e, IJavaModelStatusConstants.CORE_EXCEPTION);
		}
	}
	
	private RefactoringStatus checkForDuplicateKeys() {
		Map map= new HashMap();//String (key) -> Set of NLSSubstitution
		for (int i= 0; i < fNlsSubs.length; i++) {
			NLSSubstitution sub= fNlsSubs[i];
			String key= sub.key;
			if (!map.containsKey(key)){
			 	map.put(key, new HashSet());
			}
			((Set)map.get(key)).add(sub);		
		}
		
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= map.keySet().iterator(); iter.hasNext();) {
			Set subs= (Set)map.get(iter.next());
			result.merge(checkForDuplicateKeys(subs));
		}
		return result;
	}
	
	/**
	 * all elements in the parameter must be NLSSubstitutions with
	 * the same key
	 */
	private RefactoringStatus checkForDuplicateKeys(Set subs){
		if (subs.size() <= 1)
			return null;
		
		NLSSubstitution[] toTranslate= getEntriesToTranslate(subs);
		if (toTranslate.length <= 1)
			return null;
		
		for (int i= 0; i < toTranslate.length; i++) {
			toTranslate[i].putToPropertyFile= (i == 0);
		}

		String value= toTranslate[0].value.getValue();
		for (int i= 0; i < toTranslate.length; i++) {
			NLSSubstitution each= toTranslate[i];
			if (! hasSameValue(value, each)){
				String msg= NLSMessages.getFormattedString("NLSrefactoring.duplicated", each.key);//$NON-NLS-1$
				return RefactoringStatus.createFatalErrorStatus(msg);
			}
		}
		String[] args= {toTranslate[0].key, value};
		String msg= NLSMessages.getFormattedString("NLSrefactoring.reused", args); //$NON-NLS-1$
		return RefactoringStatus.createWarningStatus(msg);
	}
	
	private static NLSSubstitution[] getEntriesToTranslate(Set subs){
		List result= new ArrayList(subs.size());
		for (Iterator iter= subs.iterator(); iter.hasNext();) {
			NLSSubstitution each= (NLSSubstitution) iter.next();
			if (each.task == NLSSubstitution.TRANSLATE)
				result.add(each);
		}
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}
		
	private RefactoringStatus checkKeys() {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fNlsSubs.length; i++)
			result.merge(checkKey(fNlsSubs[i].key));
		return result;
	}	
	
	public static RefactoringStatus checkKey(String key){
		RefactoringStatus result= new RefactoringStatus();
		
		if (key == null)
			result.addFatalError(NLSMessages.getString("NLSrefactoring.null")); //$NON-NLS-1$

		if (key.startsWith("!") || key.startsWith("#")){ //$NON-NLS-1$ //$NON-NLS-2$
			RefactoringStatusContext context= new JavaStringStatusContext(key, new SourceRange(0, 0));
			result.addWarning(NLSMessages.getString("NLSRefactoring.warning"), context); //$NON-NLS-1$
		}	
			
		if ("".equals(key.trim())) //$NON-NLS-1$
			result.addFatalError(NLSMessages.getString("NLSrefactoring.empty")); //$NON-NLS-1$
		
		//feature in resource bundle - does not work properly if keys have ":"
		for (int i= 0; i < NLSHolder.UNWANTED_STRINGS.length; i++){
			if (key.indexOf(NLSHolder.UNWANTED_STRINGS[i]) != -1){
				String[] args= {key, NLSHolder.UNWANTED_STRINGS[i]};
				String msg= NLSMessages.getFormattedString("NLSrefactoring.should_not_contain", args); //$NON-NLS-1$
				result.addError(msg);
			}
		}
		return result;
	}
	
	private boolean willCreateAccessorClass() throws JavaModelException{
		if (!fCreateAccessorClass)
			return false;
		if (NLSSubstitution.countItems(fNlsSubs, NLSSubstitution.TRANSLATE) == 0)
			return false;
		if (getPackage().getCompilationUnit(getAccessorCUName()).exists())
			return false;
		if (typeNameExistsInPackage(getPackage(), fAccessorClassName))
			return false;
		return (! Checks.resourceExists(getAccessorCUPath()));
	}
	
	private boolean willModifySource(){
		if (NLSSubstitution.countItems(fNlsSubs, NLSSubstitution.SKIP) != fNlsSubs.length)
			return true;
		if (willAddImportDeclaration())
			return true;
		return false;		
	}
	
	private boolean willModifyPropertyFile(){
		return NLSSubstitution.countItems(fNlsSubs, NLSSubstitution.TRANSLATE) > 0;
	}
	
	private boolean willAddImportDeclaration(){
		if (fAddedImport == null)
			return false;
		if ("".equals(fAddedImport.trim())) //$NON-NLS-1$
			return false;	
		if (getCu().getImport(fAddedImport).exists())
			return false;
		if (NLSSubstitution.countItems(fNlsSubs, NLSSubstitution.TRANSLATE) == 0)	
			return false;
		return true;
		//XXX could	avoid creating the import if already imported on demand
	}
	
	// --- changes
	
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try{
			pm.beginTask("", 3); //$NON-NLS-1$
			final ValidationStateChange result= new ValidationStateChange();
			
			if (willModifySource())
				result.add(createSourceModification());
			pm.worked(1);
			
			if (willModifyPropertyFile())
				result.add(createPropertyFile());
			pm.worked(1);
			
			if (willCreateAccessorClass())
				result.add(createAccessorCU(new SubProgressMonitor(pm, 1)));
			else	
				pm.worked(1);
			
			return result;
		} finally {
			pm.done();
		}	
	}

	//---- modified source files
			
	private Change createSourceModification() throws CoreException{
		String message= NLSMessages.getFormattedString("NLSRefactoring.externalize_strings", //$NON-NLS-1$
							fCu.getElementName());
		TextChange change= new CompilationUnitChange(message, fCu); 
		for (int i= 0; i < fNlsSubs.length; i++){
			addNLS(fNlsSubs[i], change);
		}
		if (willAddImportDeclaration())
			addImportDeclaration(change);
		return change;
	}
	
	private void addImportDeclaration(TextChange builder) throws JavaModelException{
		IImportContainer importContainer= getCu().getImportContainer();
		int start;
		if (!importContainer.exists()){
			String packName= ((IPackageFragment)getCu().getParent()).getElementName();
			IPackageDeclaration packageDecl= getCu().getPackageDeclaration(packName);
			if (!packageDecl.exists())
				start= 0;
			else{
				ISourceRange sr= packageDecl.getSourceRange();
				start= sr.getOffset() + sr.getLength() - 1;
			}	
		} else{
			ISourceRange sr= importContainer.getSourceRange();
			start= sr.getOffset() + sr.getLength() - 1;
		}	
			
		String newImportText= fgLineDelimiter + "import " + fAddedImport + ";"; //$NON-NLS-2$ //$NON-NLS-1$
		String name= NLSMessages.getFormattedString("NLSrefactoring.add_import_declaration", fAddedImport); //$NON-NLS-1$
		TextChangeCompatibility.addTextEdit(builder, name, new InsertEdit(start + 1, newImportText));
	}

	private void addNLS(NLSSubstitution sub, TextChange builder){
		TextRegion position= sub.value.getPosition();
		String resourceGetter= createResourceGetter(sub.key);
		String text= NLSMessages.getFormattedString("NLSrefactoring.extrenalize_string", sub.value.getValue()); //$NON-NLS-1$
		if (sub.task == NLSSubstitution.TRANSLATE){
			TextChangeCompatibility.addTextEdit(builder, text, new ReplaceEdit(position.getOffset(), position.getLength(), resourceGetter));
		}	
		if (sub.task != NLSSubstitution.SKIP){
			NLSElement element= sub.value;
			String[] args= {text, element.getValue()};
			String name= NLSMessages.getFormattedString("NLSrefactoring.add_tag", args); //$NON-NLS-1$
			TextChangeCompatibility.addTextEdit(builder, name, createAddTagChange(element));
		}	
	}
	
	//XXX performance improvement oportunities here
	private NLSLine findLine(NLSElement element){
		for(int i= 0; i < fLines.length; i++){
			NLSElement[] lineElements= fLines[i].getElements();
			for (int j= 0; j < lineElements.length; j++){
				if (lineElements[j].equals(element))
					return fLines[i];
			}		
		}
		return null;
	}
	
	private int computeIndexInLine(NLSElement element, NLSLine line){
		for (int i= 0; i < line.size(); i++){
			if (line.get(i).equals(element))
				return i;
		}
		Assert.isTrue(false, "element not found in line"); //$NON-NLS-1$
		return -1;
	}
	
	private int computeTagIndex(NLSElement element){
		NLSLine line= findLine(element);
		Assert.isNotNull(line, "line not found for:" + element); //$NON-NLS-1$
		return computeIndexInLine(element, line) + 1; //tags are 1 based
	}
		
	private String createTagText(NLSElement element) {
		return " " + NLSElement.createTagText(computeTagIndex(element)); //$NON-NLS-1$
	}
	
	private TextEdit createAddTagChange(NLSElement element){
		int offset= element.getTagPosition().getOffset(); //to be changed
		String text= createTagText(element);
		return new InsertEdit(offset, text);
	}
	
	private String createResourceGetter(String key){
		//we just replace the first occurrence of KEY in the pattern
		StringBuffer buff= new StringBuffer(fCodePattern);
		int i= fCodePattern.indexOf(KEY);
		if (i != -1)
			buff.replace(i, i + KEY.length(), "\"" + key + "\""); //$NON-NLS-2$ //$NON-NLS-1$
		return buff.toString();
	}

	//---- resource bundle file
	
	private Change createPropertyFile() throws JavaModelException{
		if (! propertyFileExists())
			return new CreateTextFileChange(getPropertyFilePath(), createPropertyFileSource(), "8859_1", "txt"); //$NON-NLS-1$ //$NON-NLS-2$
			
		String name= NLSMessages.getFormattedString("NLSrefactoring.Append_to_property_file", getPropertyFilePath().toString()); //$NON-NLS-1$
		TextChange tfc= new TextFileChange(name, getPropertyFile());
		
		StringBuffer old= new StringBuffer(getOldPropertyFileSource());

		if (needsLineDelimiter(old))
			TextChangeCompatibility.addTextEdit(tfc, NLSMessages.getString("NLSRefactoring.add_line_delimiter"), new InsertEdit(old.length(), fgLineDelimiter));
		
		for (int i= 0; i < fNlsSubs.length; i++){
			if (fNlsSubs[i].task == NLSSubstitution.TRANSLATE){
				if (fNlsSubs[i].putToPropertyFile){
					String entry= createEntry(fNlsSubs[i].value, fNlsSubs[i].key).toString();
					String message= NLSMessages.getFormattedString("NLSRefactoring.add_entry", //$NON-NLS-1$
										fNlsSubs[i].key);
					TextChangeCompatibility.addTextEdit(tfc, message, new InsertEdit(old.length(), entry));
				}	
			}	
		}	
		return tfc;
	}

	private IFile getPropertyFile() {
		return ((IFile)ResourcesPlugin.getWorkspace().getRoot().findMember(getPropertyFilePath()));
	}
	
	private String createPropertyFileSource() throws JavaModelException{
		StringBuffer sb= new StringBuffer();
		sb.append(getOldPropertyFileSource());
		if (needsLineDelimiter(sb))
			sb.append(fgLineDelimiter);
		for (int i= 0; i < fNlsSubs.length; i++){
			if (fNlsSubs[i].task == NLSSubstitution.TRANSLATE){
				if (fNlsSubs[i].putToPropertyFile)		
					sb.append(createEntry(fNlsSubs[i].value, fNlsSubs[i].key).toString());
			}	
		}	
		return sb.toString();
	}
	
	//heuristic only
	private static boolean needsLineDelimiter(StringBuffer sb){
		if (sb.length() == 0)
			return false;
		String s= sb.toString();
		int lastDelimiter= s.lastIndexOf(fgLineDelimiter);
		if (lastDelimiter == -1)
			return true;
		if ("".equals(s.substring(lastDelimiter).trim())) //$NON-NLS-1$
			return false;
		return true;	
	}
	
	private String getOldPropertyFileSource() throws JavaModelException{
		if (! propertyFileExists())
			return ""; //$NON-NLS-1$
		
		//must read the whole contents - don't want to lose comments etc.
		InputStream is= getPropertyFileInputStream();
		String s= NLSUtil.readString(is);
		return s == null ? "": s; //$NON-NLS-1$
	}
	
	private StringBuffer createEntry(NLSElement element, String key){
		StringBuffer sb= new StringBuffer();
		sb.append(key)
		  .append("=") //$NON-NLS-1$
		  .append(convertToPropertyValue(element.getValue()))
		  .append(fgLineDelimiter);
		return sb;
	}
	
	/*
	 * see 21.6.7 of langspec-1.0
	 * @see java.util.Properties#load(InputStream)
	 */
	private static String convertToPropertyValue(String v){
		int firstNonWhiteSpace=findFirstNonWhiteSpace(v);
		if (firstNonWhiteSpace == 0)
			return v;	
		return escapeEachChar(v.substring(0, firstNonWhiteSpace), '\\') + 
				escapeCommentChars(v.substring(firstNonWhiteSpace));
	}
	
	private static StringBuffer escapeCommentChars(String string) {
		StringBuffer sb= new StringBuffer(string.length() + 5);
		for (int i= 0; i < string.length(); i++) {
			char c= string.charAt(i);
			switch (c) {
				case '!':
					sb.append("\\!"); //$NON-NLS-1$
				case '#':
					sb.append("\\#"); //$NON-NLS-1$
				default:
					sb.append(c);
			}
		}
		return sb;
	}

	private static String escapeEachChar(String s, char escapeChar){
		char[] chars= new char[s.length() * 2];
		
		for (int i= 0; i < s.length(); i++){
			chars[2*i]= escapeChar;
			chars[2*i + 1]= s.charAt(i);
		}
		return new String(chars);
	}
	
	/**
	 * returns the length if only whitespaces
	 */
	private static int findFirstNonWhiteSpace(String s){
		for (int i= 0; i < s.length(); i++){
			if (! Character.isWhitespace(s.charAt(i)))
				return i;
		}		
		return s.length();
	}

	// ------------ accessor class creation

	private IPackageFragment getPackage(){
		 return (IPackageFragment)fCu.getParent();
	}
		
	private static boolean typeNameExistsInPackage(IPackageFragment pack, String name) throws JavaModelException{
		return Checks.findTypeInPackage(pack, name) != null;
	}
	
	public void setCreateAccessorClass(boolean create){
		fCreateAccessorClass= create;
	}
	
	public boolean getCreateAccessorClass(){
		return fCreateAccessorClass;
	}
	
	public String getAccessorClassName(){
		return fAccessorClassName;
	}
	
	public void setAccessorClassName(String name){
		fAccessorClassName= name;
		Assert.isNotNull(name);
	}
	
	private String getAccessorCUName(){
		return fAccessorClassName + ".java"; //$NON-NLS-1$
	}
	
	private Change createAccessorCU(IProgressMonitor pm) throws CoreException {
		return new CreateTextFileChange(getAccessorCUPath(), createAccessorCUSource(pm), "java");	 //$NON-NLS-1$
	} 
		
	private IPath getAccessorCUPath(){
		IPath cuName= new Path(fCu.getElementName());
		return ResourceUtil.getResource(fCu).getFullPath()
						  .removeLastSegments(cuName.segmentCount())
						  .append(getAccessorCUName());
	}
	
	//--bundle class source creation
	private String createAccessorCUSource(IProgressMonitor pm) throws CoreException {
		return CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, getUnformattedSource(pm), 0, null, null, fCu.getJavaProject());
	}

	private String getUnformattedSource(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCu= null;
		try{
			newCu= WorkingCopyUtil.getNewWorkingCopy(getPackage(), getAccessorCUName());
			String comment= CodeGeneration.getTypeComment(newCu, fAccessorClassName, fgLineDelimiter);//$NON-NLS-1$
			newCu.getBuffer().setContents(CodeGeneration.getCompilationUnitContent(newCu, comment, createClass().toString(), fgLineDelimiter)); //$NON-NLS-1$
			addImportsToAccessorCu(newCu, pm);
			return newCu.getSource();
		} finally{
			if (newCu != null)
				newCu.destroy();
		}
	}

	private void addImportsToAccessorCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		ImportsStructure is= new ImportsStructure(newCu, fCodeGenerationSettings.importOrder, fCodeGenerationSettings.importThreshold, true);
		is.addImport("java.util.MissingResourceException"); //$NON-NLS-1$
		is.addImport("java.util.ResourceBundle"); //$NON-NLS-1$
		is.create(false, pm);
	}

	private StringBuffer createClass() throws CoreException{
		String ld= fgLineDelimiter; //want shorter name
		StringBuffer b= new StringBuffer();
		b.append("public class ").append(fAccessorClassName).append(" {").append(ld) //$NON-NLS-2$ //$NON-NLS-1$
		 .append(ld)
		 .append("private static final String ") //$NON-NLS-1$
		 .append(getBundleStringName())
		 .append("= \"") //$NON-NLS-1$
		 .append(getResourceBundleName()).append("\";").append(NLSElement.createTagText(1)).append(ld) //$NON-NLS-1$
		 .append(ld)
		 .append("private static final ResourceBundle ") //$NON-NLS-1$
		 .append(getResourceBundleConstantName())
		 .append("= ResourceBundle.getBundle(") //$NON-NLS-1$
		 .append(getBundleStringName())
		 .append(");") //$NON-NLS-1$
		 .append(ld)
		 .append(ld)
		 .append(createConstructor())
		 .append(ld)
		 .append(createGetStringMethod())
		 .append("}").append(ld); //$NON-NLS-1$
		return b;
	}
	
	private static String getBundleStringName(){
		return "BUNDLE_NAME";	//$NON-NLS-1$
	}
	
	private static String getResourceBundleConstantName(){
		return "RESOURCE_BUNDLE";//$NON-NLS-1$
	}
	
	private String createConstructor() throws CoreException{
		String constructorBody= CodeGeneration.getMethodBodyContent(fCu, fAccessorClassName, fAccessorClassName, true, "", fgLineDelimiter); //$NON-NLS-1$
		if (constructorBody == null)
			constructorBody= ""; //$NON-NLS-1$
		return createNewConstructorComment() + "private " + fAccessorClassName + "(){" +  //$NON-NLS-2$//$NON-NLS-1$
				fgLineDelimiter + constructorBody + fgLineDelimiter + '}';
	}
	
	private String createNewConstructorComment() throws CoreException {
		if (fCodeGenerationSettings.createComments){
			String comment= CodeGeneration.getMethodComment(fCu, fAccessorClassName, fAccessorClassName, new String[0], new String[0], null, null, fgLineDelimiter);
			if (comment == null)
				return ""; //$NON-NLS-1$
			return comment + fgLineDelimiter;
		}else
			return "";//$NON-NLS-1$
	}

	private String createGetStringMethod() throws CoreException{
		String bodyStatement = 	new StringBuffer()
		.append("try {").append(fgLineDelimiter) //$NON-NLS-1$
		.append("return ") //$NON-NLS-1$
		.append(getResourceBundleConstantName())
		.append(".getString(key);").append(fgLineDelimiter) //$NON-NLS-1$
		.append("} catch (MissingResourceException e) {").append(fgLineDelimiter) //$NON-NLS-1$
		.append("return '!' + key + '!';").append(fgLineDelimiter) //$NON-NLS-1$
		.append("}").toString(); //$NON-NLS-1$
		
		String methodBody= CodeGeneration.getMethodBodyContent(fCu, fAccessorClassName, "getString", false, bodyStatement, fgLineDelimiter); //$NON-NLS-1$
		if (methodBody == null)
			methodBody= "";  //$NON-NLS-1$
		return createNewGetStringMethodComment() + "public static String getString(String key) {" //$NON-NLS-1$
						+ fgLineDelimiter + methodBody + fgLineDelimiter + '}';
	}

	private String createNewGetStringMethodComment() throws CoreException {
		if (fCodeGenerationSettings.createComments){
			String comment= CodeGeneration.getMethodComment(fCu, fAccessorClassName, "getString", new String[]{"key"}, new String[0], "QString;", null, fgLineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (comment == null)
				return "";//$NON-NLS-1$
			return comment + fgLineDelimiter;
		}else
			return "";//$NON-NLS-1$
	}

	
	//together with the .properties extension
	private String getPropertyFileName() {
		return getPropertyFilePath().lastSegment();
	}
	
	//extension removed
	private String getPropertyFileSimpleName() {
		String fileName= getPropertyFileName();
		return fileName.substring(0, fileName.indexOf(PROPERTY_FILE_EXT));
	}
	
	
	private String getResourceBundleName() {
		//remove filename.properties
		IResource res= ResourcesPlugin.getWorkspace().getRoot().findMember(getPropertyFilePath().removeLastSegments(1));
		if (res != null && res.exists()){
			IJavaElement el= JavaCore.create(res);
			if (el instanceof IPackageFragment){
				IPackageFragment p= (IPackageFragment)el;
				if (p.isDefaultPackage())
					return getPropertyFileSimpleName();
				return p.getElementName() + "." + getPropertyFileSimpleName(); //$NON-NLS-1$
			}
		}
		//XXX can we get here?
		IPackageFragment pack= getPackage();
		if (pack.isDefaultPackage())
			return fProperyFileName;
		return pack.getElementName() + "." + fProperyFileName; //$NON-NLS-1$
	}	
}
