package edu.umich.srg.marketsim.testing;

import java.io.IOException;
import java.io.Writer;

public class NullWriter extends Writer {

  private static final NullWriter singleton = new NullWriter();

  private NullWriter() {}

  public static Writer get() {
    return singleton;
  }

  @Override
  public void close() throws IOException {}

  @Override
  public void flush() throws IOException {}

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {}

}
