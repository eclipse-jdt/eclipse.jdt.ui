package org.eclipse.jdt.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.RuleBasedPartitioner;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.internal.ui.text.JavaColorManager;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.java.JavaCodeScanner;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocScanner;


/**
 * Tools required to configure a Java text viewer. 
 * The color manager and all scanner exist only one time, i.e.
 * the same instances are returned to all clients. Thus, clients
 * share those tools.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class JavaTextTools {
	
	private class PreferenceListener implements IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			if (affectsBehavior(event))
				adaptToPreferenceChange(event);
		}
	};
		
	/** The color manager */
	private JavaColorManager fColorManager;
	/** The Java source code scanner */
	private JavaCodeScanner fCodeScanner;
	/** The JavaDoc scanner */
	private JavaDocScanner fJavaDocScanner;
	/** The Java partitions scanner */
	private JavaPartitionScanner fPartitionScanner;	
	
	/** The preference store */
	private IPreferenceStore fPreferenceStore;
	/** The preference change listener */
	private PreferenceListener fPreferenceListener= new PreferenceListener();

	
	/**
	 * Creates a new Java text tools collection.
	 */
	public JavaTextTools(IPreferenceStore store) {
		fPreferenceStore= store;
		fPreferenceStore.addPropertyChangeListener(fPreferenceListener);
		
		fColorManager= new JavaColorManager(store);
		fCodeScanner= new JavaCodeScanner(fColorManager);
		fJavaDocScanner= new JavaDocScanner(fColorManager);
		fPartitionScanner= new JavaPartitionScanner();
	}
	
	/**
	 * Disposes all the individual tools of this tools collection.
	 */
	public void dispose() {
		
		fCodeScanner= null;
		fJavaDocScanner= null;
		fPartitionScanner= null;
		
		if (fColorManager != null) {
			fColorManager.dispose();
			fColorManager= null;
		}
		
		if (fPreferenceStore != null) {
			fPreferenceStore.removePropertyChangeListener(fPreferenceListener);
			fPreferenceStore= null;
			fPreferenceListener= null;
		}
	}
	
	/**
	 * Returns the color manager which is used to manage
	 * any Java-specific colors needed for such things like syntax highlighting.
	 *
	 * @return the color manager to be used for Java text viewers
	 */
	public IColorManager getColorManager() {
		return fColorManager;
	}
	
	/**
	 * Returns a scanner which is configured to scan Java source code.
	 *
	 * @return a Java source code scanner
	 */
	public RuleBasedScanner getCodeScanner() {
		return fCodeScanner;
	}
	
	/**
	 * Returns a scanner which is configured to scan JavaDoc compliant comments.
	 * Notes that the start sequence "/**" and the corresponding end sequence
	 * are part of the JavaDoc comment.
	 *
	 * @return a JavaDoc scanner
	 */
	public RuleBasedScanner getJavaDocScanner() {
		return fJavaDocScanner;
	}
	
	/**
	 * Returns a scanner which is configured to scan 
	 * Java-specific partitions, which are multi-line comments,
	 * JavaDoc comments, and regular Java source code.
	 *
	 * @return a Java partition scanner
	 */
	public RuleBasedScanner getPartitionScanner() {
		return fPartitionScanner;
	}
	
	/**
	 * Factory method for creating a Java-specific document partitioner
	 * using this object's partitions scanner. This method is a 
	 * convenience method.
	 *
	 * @return a newly created Java document partitioner
	 */
	public IDocumentPartitioner createDocumentPartitioner() {
		
		String[] types= new String[] {
			JavaPartitionScanner.JAVA_DOC,
			JavaPartitionScanner.JAVA_MULTILINE_COMMENT
		};
		
		return new RuleBasedPartitioner(getPartitionScanner(), types);
	}
	
	/**
	 * Returns the names of the document position categories used by the document
	 * partitioners created by this object to manage their partition information.
	 * If the partitioners don't use document position categories, the returned
	 * result is <code>null</code>.
	 *
	 * @return the partition managing position categories or <code>null</code> 
	 * 			if there is none
	 */
	public String[] getPartitionManagingPositionCategories() {
		return new String[] { RuleBasedPartitioner.CONTENT_TYPES_CATEGORY };
	}
	
	/**
	 * Determines whether the preference change encoded by the given event
	 * changes the behavior of one its contained components.
	 * 
	 * @param event the event to be investigated
	 * @return <code>true</code> if event causes a behavioral change
	 */
	public boolean affectsBehavior(PropertyChangeEvent event) {
		return fColorManager.affectsBehavior(event);
	}
	
	/**
	 * Adapts the behavior of the contained components to the change
	 * encoded in the given event.
	 * 
	 * @param event the event to whch to adapt
	 */
	protected void adaptToPreferenceChange(PropertyChangeEvent event) {
		fColorManager.adaptToPreferenceChange(event);
		fCodeScanner.colorManagerChanged();
		fJavaDocScanner.colorManagerChanged();
	}
}