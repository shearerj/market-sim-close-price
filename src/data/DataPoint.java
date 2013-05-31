package data;

public class DataPoint {
	public String key;
	public double value;

	//
	//Constructors
	//
	DataPoint(){
		key = "";
		value = 0;
	}
	
	DataPoint(String _key, double _value) {
		key = _key;
		value = _value;
	}

}
