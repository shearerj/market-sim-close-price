package data;

import event.*;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;

public interface TransactionObject {

	public abstract ArrayList<DataPoint> compute(ArrayList<PQTransaction> transactions);
	
	public abstract ArrayList<DataPoint> multiMarketData();

}
