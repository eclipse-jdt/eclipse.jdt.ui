package experiments;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;

/*
 * The following should be cofigurable:
 * .change field access modifier. default: leave unchanged
 * .create accessor
 *    .create setter
 *    .create getter
 *    .create both (default)
 *  .getter and setter access modifiers (default: same as field)
 *  .getter and setter names (default: getX, setX, where X is getField().getElementName())
 *  .use asymetric assignment (no default, must set in constructor)
 */

//XXX should be a subclass of FieldRefactoring
public class AbstractFieldRefactoring extends Refactoring {

	public static final int PRIVATE= 0;
	public static final int DEFAULT= 1;
	public static final int PROTECTED= 2;
	public static final int PUBLIC= 3;
	
	private boolean fCreateGetter= true;
	private boolean fCreateSetter= true;
	private int fGetterModifier;
	private int fSetterModifier;
	private String fGetterName;
	private String fSetterName;
	private boolean fAsymetricAssignment;
	
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	private IField fField;

	public AbstractFieldRefactoring(ITextBufferChangeCreator changeCreator, IField field, boolean useAsymetricAssignement){
		fField= field;
		fTextBufferChangeCreator= changeCreator;
		Assert.isNotNull(changeCreator, "change creator"); //$NON-NLS-1$
		Assert.isTrue(field.exists(), "field must exist");
		setDefaults();
		correctScope();
	}
		
	public final IField getField() {
		return fField;
	}
	
	/* XXX: copied from RenameField
	 * narrow down the scope
	 */ 
	private void correctScope(){
		if (getField().isBinary())
			return;
		try{
			//only the declaring compilation unit
			if (Flags.isPrivate(getField().getFlags()))
				setScope(SearchEngine.createJavaSearchScope(new IResource[]{getResource(getField())}));
		} catch (JavaModelException e){
			//do nothing
		}
	}
	
	// --- framework methods 
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		//if (fCreateGetter)
		//	result.merge(checkGetterName());
		//if (fCreateSetter)
		//	result.merge(checkSetterName());	
		return result;
	}

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		return new RefactoringStatus();
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		return new NullChange();
	}

	public String getName() {
		return "Abstract Field: " + getField().getElementName();
	}
	
	//-- customize methods
		
	public void setCreateGetter(boolean create){
		fCreateSetter= create;
		Assert.isTrue(fCreateGetter || fCreateSetter, "must create getter or setter");
	}
	
	public void setCreateSetter(boolean create){
		fCreateSetter= create;
		Assert.isTrue(fCreateGetter || fCreateSetter, "must create getter or setter");
	}
	
	public boolean getCreateGetter(){
		return fCreateGetter;
	}
	
	public boolean getCreateSetter(){
		return fCreateSetter;
	}	
	
	public void setGetterModifier(int modifier){
		Assert.isTrue(fCreateGetter, "setting getter modifier not possible");
		Assert.isTrue(modifier == PRIVATE || modifier == DEFAULT || modifier == PROTECTED || modifier == PUBLIC, "incorrect acces modifier");
		fGetterModifier= modifier;
	}

	public void setSetterModifier(int modifier){
		Assert.isTrue(fCreateSetter, "setting setter modifier not possible");
		Assert.isTrue(modifier == PRIVATE || modifier == DEFAULT || modifier == PROTECTED || modifier == PUBLIC, "incorrect acces modifier");
		fSetterModifier= modifier;
	}
	
	public int getGetterModifier() {
		return fGetterModifier;
	}
	
	public int getSetterModifier() {
		return fSetterModifier;
	}
	
	public String getGetterName() {
		return fGetterName;
	}

	public void setGetterName(String getterName) {
		Assert.isNotNull(getterName);
		fGetterName = getterName;
	}

	public String getSetterName() {
		return fSetterName;
	}
	
	public void setSetterName(String setterName) {
		Assert.isNotNull(setterName);
		fSetterName = setterName;
	}
	
	//---
	private void setDefaults(){
		fCreateGetter= true;
		fCreateSetter= true;
		fGetterName= "get" + upperCaseFirstChar(getField().getElementName());
		fSetterName= "set" + upperCaseFirstChar(getField().getElementName());
		try{
			fGetterModifier= getFlagConst(getField().getFlags());
			fSetterModifier= getFlagConst(getField().getFlags());
		}catch (JavaModelException e){
			//XXX 
		}	
	}
	
	private static int getFlagConst(int flag){
		if (Flags.isPrivate(flag))
			return PRIVATE;
		if (Flags.isProtected(flag))
			return PROTECTED;
		if (Flags.isPublic(flag))
			return PUBLIC;
		return DEFAULT;
	}
	
	private static String upperCaseFirstChar(String s){
		if ("".equals(s))
			return s;
		if (Character.isUpperCase(s.charAt(0)))
			return s;
		return Character.toLowerCase(s.charAt(0)) + 
			   (s.length() == 1 ? "": s.substring(1));	
	}
}

