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
		//boolean i = false;
		//assertFalse(i);
		
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
		
		
		//fail("Not yet implemented");
	}

	@Test
	public void testingQuantizeInt()
	{ 	int n = 1001;
		int quanta= 10;
		int q = quantize(n, quanta);
		assertTrue(q==1000);
		
		n = -32; 
		q = quantize(n, quanta);
		assertTrue(q==-30);
		
		n=0;
		q = quantize(n, quanta);
		assertTrue(q==0);
	}
	
	@Test
	public void testingQuantizeDouble()
	{ 	double n = 100.003;
		double quanta= 10;
		double qresult = quantize(n, quanta);
		assertTrue(qresult==100);
		
		n = -34.99999; 
		qresult = quantize(n, quanta);
		assertTrue(qresult==-30);
		
		n=-0.12345;
		qresult = quantize(n, quanta);
		assertTrue(qresult==0);
	}

	@Test
	public void testingBound()
	{ 
		int num = 0;
		int lower = 1;
		int upper = 2;
		int b = bound(num,lower, upper);
		assertTrue(b==1);
		
	}

	
}
