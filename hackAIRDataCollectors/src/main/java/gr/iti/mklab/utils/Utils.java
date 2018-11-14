package gr.iti.mklab.utils;

import java.util.Date;

public class Utils {

	public static void outputMessage(String message, boolean verbose) {
		if (verbose) {
			System.out.println(new Date() + " - " + message);
		}
	}
}
