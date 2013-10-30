package utils;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
//import MathUtils.java;
/*
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
*/
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.Test;

public class MathTest extends MathUtils {
	
	
	
	
	
	@Test
	public void testingLogn() {
		
		//standard inputs for logn
		int number= 10;
		int base = 10;
		int result = logn(number, base);
		assertTrue(result==1);//normal test case
		//log base 10 of 10 is 1
		
		number = 0;
		base = 10;
		result = logn(number, base);
		assertTrue(result==-1);
		number=-999;
		result = logn(number,base);
		assertTrue(result==-1);
		//boundry cases of number <= 0, as ln is undefined for those values
		//ensure -1 is given where log n is undefined

		//pathalogical test cases like
		//number = .000000000000001; wouldnt apply as number is of type int
		
		
		
	}

	@Test
	public void testingQuantizeInt()
	{ 	
		//standard input
		int n = 1001;
		int quanta= 10;
		int q = quantize(n, quanta);
		assertTrue(q==1000);
		
		//to verify function rounds down for negative numbers
		n = -32; 
		q = quantize(n, quanta);
		assertTrue(q==-30);
		
		//verifies that when provided a value that is a multiple of the quanta
		//the program will round to the same value
		n=0;
		q = quantize(n, quanta);
		assertTrue(q==0);
	}
	
	@Test
	public void testingQuantizeDouble()
	{ 	
		//standard form of input for decimal variant of quantize function
		double n = 100.003;
		double quanta= 10;
		double qresult = quantize(n, quanta);
		assertTrue(qresult==100);
		
		//verifies that n is not rounded incorrectly
		//verifies that  negative numbers below middle round down to negative quanta
		n = -34.99999; 
		qresult = quantize(n, quanta);
		assertTrue(qresult==-30);

		//checking that  when provided a value that is a multiple of the quanta
		//the program will round to the same value
		n=-0.000000000000000000;
		qresult = quantize(n, quanta);
		assertTrue(qresult==0);
		//assertTrue(n==0)// also can be used to check floating point accuracy

	}

	@Test
	public void testingBound()
	{ 
		//testing basic functionality of bound function
		//verifies minimizes num and upper
		//maximizes result and lower bound
		int num = 0;
		int lower = 1;
		int upper = 2;
		int b = bound(num,lower, upper);
		assertTrue(b==1);
		
	}

	
}
