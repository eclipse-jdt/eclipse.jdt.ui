package org.eclipse.jdt.ui.leaktest;



public class ProfileException extends Exception {

	public ProfileException(String msg, Throwable e) {
		super(msg, e);
	}

	public ProfileException(Throwable e) {
		super(e);
	}

	public ProfileException(String msg) {
		super(msg);
	}

}