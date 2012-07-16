package systemmanager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The ConfigProperties class holds configuration values read at startup.
 * 
 * The class is implemented as a kind of Singleton class (like the
 * Java Math class. The is declared final and all methods are
 * declared static. This means that the class cannot be extended.
 * 
 * This, in effect, achieves an implementation based on the Singleton pattern.
 * You cannot create any instance of classes like Math, and can only call the static
 * methods directly in the existing final class.
 * 
 * It also enables all objects to get at configuration information, without passing the
 * reference to the class.
 */
public class ConfigProperties {

	private Properties properties;

	public ConfigProperties(String f) throws IOException {
		init(f);
	}

	/**
	 * Converts the specified file into accessible configuration members.
	 *
	 * @param fName the name of the config file
	 * @throws IOException
	 */
	public void init(String fName) throws IOException {
		properties = new Properties();
		InputStream is = new BufferedInputStream(new FileInputStream(fName));
		properties.load(is);
	}

	/**
	 * Gets the value for the specified key.
	 *
	 * @param key the value for the specified key
	 * @return the value for the key, or null if not found
	 */
	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public void print(Log l) {
//		if (l != null)
//			l.log(Log.ERROR, AB3DConsts.SYS_MAN, "Properties::print" + properties);
			// TODO - logging
//		else
			System.out.println("Properties::print" + properties);
	}
}
