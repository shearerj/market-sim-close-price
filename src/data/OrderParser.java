package data;

import java.io.FileNotFoundException;
import java.util.List;

public interface OrderParser {
		
	/*
	 * Processes the file pointed to by fileName and returns a list of OrderDatums
	 */
	abstract List<OrderDatum> process(String fileName) throws FileNotFoundException;
	
	/**
	 * @param args
	 * @throws IOException
	 */

	/*
	 * public static void main(String[] args) throws IOException { // TODO
	 * Auto-generated method stub if (args.length < 2) {
	 * System.err.println("Usage: <filename> (nyse | nasdaq)"); return; }
	 * 
	 * int i = 0; File inputFile = new File(args[i++]).getCanonicalFile();
	 * 
	 * String type = args[i++]; if (type.equals("nyse")){
	 * processNYSE(inputFile); } else if (type.equals("nasdaq")){
	 * processNasdaq(inputFile); } }
	 */

}
