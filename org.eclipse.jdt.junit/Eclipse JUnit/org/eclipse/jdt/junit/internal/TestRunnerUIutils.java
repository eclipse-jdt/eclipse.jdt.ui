package org.eclipse.jdt.junit.internal;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.NumberFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;


public class TestRunnerUIutils {
	
	private TestRunnerUIutils() {
	}
	
	protected static ITextEditor openInEditor(IJavaElement javaElement) {
		if (javaElement == null || !javaElement.exists()) {
			// can not goTo File
			Display.getCurrent().beep();
			return null;
		}		
		IEditorPart editor= null;
		try {	
			editor= EditorUtility.openInEditor(javaElement, false);			
		} catch (CoreException e){
			return null;
		}						
		if(!(editor instanceof ITextEditor)) {
			// can not goTo File
			Display.getCurrent().beep();
			return null;
		}			
		return (ITextEditor) editor;
	}	

	public static IType getType(IJavaProject jproject, String str) {
		if (jproject == null || str == null) return null;
		String pathStr= str.replace('.', '/') + ".java"; //$NON-NLS-1$
		IJavaElement jelement= null;
		try {
			jelement= jproject.findElement(new Path(pathStr));
		} catch (JavaModelException e) {
			// an illegal path -> no element found
		}
		IType resType= null;
		if (jelement instanceof ICompilationUnit) {
			String simpleName= Signature.getSimpleName(str);
			resType= ((ICompilationUnit)jelement).getType(simpleName);
		} else if (jelement instanceof IClassFile) {
			try {
				resType= ((IClassFile)jelement).getType();
			} catch (JavaModelException e) {
				// Fall through and return null.
			}
		}
		return resType;
	}

	/**
	 * Returns the formatted string of the elapsed time.
	 */
	protected static String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}

	protected static String filterStack(String stackTrace) {	
		IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
		if (!store.getBoolean(PreferencePage.PLUGIN_INIT_DONE))
			store.setValue(PreferencePage.DO_FILTER_STACK, true);
			
		if (!store.getBoolean(PreferencePage.DO_FILTER_STACK) || stackTrace == null) 
			return stackTrace;
			
		StringWriter stringWriter= new StringWriter();
		PrintWriter printWriter= new PrintWriter(stringWriter);
		StringReader stringReader= new StringReader(stackTrace);
		BufferedReader bufferedReader= new BufferedReader(stringReader);		
		String line;
		try {	
			while ((line= bufferedReader.readLine()) != null) {
				if (!filterLine(line))
					printWriter.println(line);
			}
		} catch (Exception IOException) {
			return stackTrace; // return the stack unfiltered
		}
		return stringWriter.toString();
	}
	
	protected static boolean filterLine(String line) {
		IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();	
		if (!store.getBoolean(PreferencePage.PLUGIN_INIT_DONE)) {
			String[] patterns= PreferencePage.fgFilterPatterns;
			for (int i= 0; i < patterns.length; i++)
				if (line.indexOf(patterns[i]) > 0)
					return true;
		}		
		else
			for (int i= 0; i < store.getInt(PreferencePage.NOF_STACK_FILTER_ENTRIES); i++)
				if (line.indexOf(store.getString(PreferencePage.STACK_FILTER_ENTRY_ + i)) > 0)
					return true;
					
		return false;
	}
	
	protected static Image createImage(String fName, Class clazz) {
		try {
			return (new Image(Display.getCurrent(), clazz.getResourceAsStream(fName)));
		}
		catch (Exception e) {
			String msg= "warning: could not load image!";
			Status status= new Status(IStatus.WARNING, JUnitPlugin.getPluginID(), IStatus.OK, msg, e);
			JUnitPlugin.getDefault().getLog().log(status);
			return new Image(Display.getCurrent(), 1, 1);
		}
	}

}

