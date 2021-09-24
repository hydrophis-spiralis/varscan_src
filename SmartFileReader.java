package net.sf.varscan;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * A class for bug-free file reading with the Java API contributed by Bina Technologies
 *
 * @version	2.4
 *
 * @author Daniel C. Koboldt https://sourceforge.net/u/dkoboldt
 *
 */
public class SmartFileReader extends FileReader {

	  public SmartFileReader(File file) throws FileNotFoundException {
		    super(file);
		  }

		  public SmartFileReader(FileDescriptor fd) {
		    super(fd);
		  }

		  public SmartFileReader(String fileName) throws FileNotFoundException {
		    super(fileName);
		  }

		  public boolean ready() throws IOException {
		    super.ready();
		    // Implemented because sun.nio.cs.StreamDecoder doesn't implement ready() properly.
		    return true;
		  }

}
