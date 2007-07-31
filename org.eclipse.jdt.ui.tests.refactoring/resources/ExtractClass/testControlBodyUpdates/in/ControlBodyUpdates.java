package p;

import java.util.ArrayList;

public class ControlBodyUpdates {
	private ArrayList<Integer> arraylists;
	Integer[] ints;
	int fNewStart;

	private void foo() {
		for (int i = 0; i < arraylists.get(0); i++)
			arraylists = null;
		for (int intValue : ints)
			arraylists = new ArrayList(arraylists.size());
		while (ints.length != 0)
			arraylists = null;
		if (arraylists == null)
			arraylists = null;
		int[] newRange= new int[8];
		for (int i = 0; i < arraylists.get(0); i++) {
			arraylists = null;
		}
		for (int intValue : ints) {
			arraylists = new ArrayList(arraylists.size());
		}
		while (ints.length != 0) {
			arraylists = null;
		}
		if (arraylists == null) {
			arraylists = null;
		}
		if (newRange[0] > 0)
			fNewStart= newRange[0]-1;	// line number start at 0!
		else
			fNewStart= 0;
		if (newRange[0] > 0) {
			fNewStart= newRange[0]-1;	// line number start at 0!
		} else {
			fNewStart= 0;
		}
	}
}