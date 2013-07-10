package utils;

import java.util.Arrays;

public class StringUtils {

	public static String delimit(String delimiter, String... strings) {
		return delimit(delimiter, Arrays.asList(strings));
	}

	public static String delimit(String delimiter, Iterable<String> strings) {
		StringBuilder sb = new StringBuilder();
		for (String string : strings)
			if (string != null && !string.isEmpty())
				sb.append(string).append(delimiter);
		return sb.substring(0, sb.length() - delimiter.length());
	}

}
