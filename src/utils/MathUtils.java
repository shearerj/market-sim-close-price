package utils;

public class MathUtils {

	/**
	 * Quantize "n" in increments of "quanta". If n is halfway between quanta it
	 * will be rounded towards positive infinity. e.g. quantize(5, 10) = 10 but
	 * quantize(-5, 10) = 0
	 */
	public static int quantize(int n, int quanta) {
		return quanta * (int) Math.round(n / (double) quanta);
	}

	public static double quantize(double n, double quanta) {
		// Floor instead of round to prevent the case in round which converts
		// NaN and Inf to 0
		return quanta * Math.floor(n / quanta + .5d);
	}
	
	public static int bound(int num, int lower, int upper) {
		return Math.max(Math.min(num, upper), lower);
	}

}
