package data;

import java.io.IOException;
import java.io.File;
//import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Scanner;

import com.google.common.collect.PeekingIterator;

public abstract class MarketDataParser {
	protected Scanner scanner; 
	protected LinkedList<OrderDatum> orderDatumList;
	
	public MarketDataParser(String pathName) throws IOException {
//		scanner = new Scanner(Paths.get(pathName));
		scanner = new Scanner(new File(pathName));
		orderDatumList = new LinkedList<OrderDatum>();
	}
	
	abstract public PeekingIterator<OrderDatum> getIterator();
}
