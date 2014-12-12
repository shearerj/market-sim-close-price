package utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/** Like a FileWriter, but will only create a file if it's written to */
public class LazyFileWriter extends Writer {

	private final File file;
	private final boolean append;
	private FileWriter writer;
	
	private LazyFileWriter(File file, boolean append) {
		this.file = checkNotNull(file);
		this.append = append;
	}
	
	public static LazyFileWriter create(File file, boolean append) {
		return new LazyFileWriter(file, append);
	}
	
	public static LazyFileWriter create(File file) {
		return new LazyFileWriter(file, false);
	}
	
	public static LazyFileWriter create(String filename, boolean append) {
		return new LazyFileWriter(new File(filename), append);
	}
	
	public static LazyFileWriter create(String filename) {
		return create(filename, false);
	}

	@Override
	public void close() throws IOException {
		if (writer != null)
			writer.close();
	}

	@Override
	public void flush() throws IOException {
		if (writer != null)
			writer.flush();
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		if (writer == null) {
			file.getParentFile().mkdirs();
			writer = new FileWriter(file, append);
		}
		writer.write(cbuf, off, len);
	}

}
