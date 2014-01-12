package data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

interface OrderParser {
	List<OrderDatum> orderDataList = new ArrayList<OrderDatum>();
	
	void process(File inputFile) throws FileNotFoundException;

	OrderDatum parseAddOrder(Scanner lineScanner);

	OrderDatum parseDeleteOrder(Scanner lineScanner);

	OrderDatum parseModifyOrder(Scanner lineScanner);

	OrderDatum parseImbalanceOrder(Scanner lineScanner);

	File readFile(String filename) throws IOException;

	List<OrderDatum> getOrderDataList();
	
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
