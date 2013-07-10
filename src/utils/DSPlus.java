package utils;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;

public class DSPlus extends DescriptiveStatistics {

	private static final long serialVersionUID = 4016329311188099666L;
	protected static final Median median = new Median();

	public DSPlus() {
		super();
	}

	public DSPlus(int window) {
		super(window);
		// TODO Auto-generated constructor stub
	}

	public DSPlus(double[] initialDoubleArray) {
		super(initialDoubleArray);
	}

	public DSPlus(DescriptiveStatistics original) {
		super(original);
	}

	public double getRMSD(DescriptiveStatistics other) {
		double rmsd = 0;
		double[] x1 = this.getValues();
		double[] x2 = other.getValues();
		int len = Math.min(x1.length, x2.length);
		int n = 0; // count number of non-NaN values

		// iterate through number of elements in shorter array
		for (int i = 0; i < len; i++) {
			if (!Double.isNaN(x1[i]) && !Double.isNaN(x2[i])) {
				rmsd += Math.pow(x1[i] - x2[i], 2);
				n++;
			}
		}
		return Math.sqrt(rmsd / n);
	}

	// FIXME This currently has very bad behavior with nans. Should fix, but
	// it's not clear what the proper fix should be. One would be making nans
	// positive or negative infinity...
	public double getMedian() {
		return median.evaluate(getValues());
	}

}
