package utils;


import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.Test;

public class MathUtilsTest  extends MathUtils
{
	
	
	
	
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
		
		
		//shows that logn function provided allows use of negative base values
		number = 10;
		base = -1; 
		result = logn(number, base);
		assertTrue(result!=-1);
		number=-2;
		result = logn(number,base);
		assertTrue(result==-1);
		
		number = 0;
		base = 2;
		result = logn(number, base);
		assertTrue(result==-1);
		number=-98;
		result = logn(number,base);
		assertTrue(result==-1);
		
		number = 5; base =3;
		//if using double answer should be  0.69897000434
		result = logn(number,base);
		assertTrue(result==1);
		
		//7 log 9 is  0.69897000434
		number =7; 
		base =9;
		result = logn(number,base);
		assert(result==1);
		
		//62 log e is  4.127134385
		number =62; 
		base =3;//rounding as input is integer base
		result = logn(number,base);
		assert(result==4);
		
		//39 log 37 is  1.591064607 
		number =39; 
		base =37;
		result = logn(number,base);
		assert(result==2);
		
	}

	@Test
	public void testingQuantizeInt()
	{ 	
		//standard input
		int n = 1001;
		int quanta= 10;
		int q = quantize(n, quanta);
		assert(q==1000);
		
		//to verify function rounds down for negative numbers
		n = -32; 
		q = quantize(n, quanta);
		assert(q==-30);
		
		//verifies that when provided a value that is a multiple of the quanta
		//the program will round to the same value
		n=0;
		q = quantize(n, quanta);
		assert(q==0);
	}
	
	@Test
	public void testingQuantizeDouble()
	{ 	
		//standard form of input for decimal variant of quantize function
		double n = 100.003;
		double quanta= 10;
		double qresult = quantize(n, quanta);
		assert(qresult==100);
		
		//verifies that n is not rounded incorrectly
		//verifies that  negative numbers below middle round down to negative quanta
		n = -34.99999; 
		qresult = quantize(n, quanta);
		assert(qresult==-30);

		//checking that  when provided a value that is a multiple of the quanta
		//the program will round to the same value
		n=-0.000000000000000000;
		qresult = quantize(n, quanta);
		assert(qresult==0);
		//assert(n==0)// also can be used to check floating point accuracy

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
		assert(b==1);
		
		//testing valid results using repeated values 
		// for variables
		num=0; 
		lower = 0;
		upper = 1;
		b = bound(num,lower, upper);
		assert(b==0);
		
		num=0; lower = 1; upper =0;
		b = bound(num,lower, upper);
		assert(b==0);
		
		num=0; lower = 0; upper =1;
		b = bound(num,lower, upper);
		assert(b==0);
		
		//verifying case when all three inputs provided are same
		num = 0;
		lower = 0;
		upper = 0;
		b = bound(num,lower, upper);
		assert(b==0);
		
		//testing with negative numbers and large values provided
		num = -999999899;
		//noting that this is the max length int num can have before IDE throws an error 
		lower = -88;
		upper = -875;
		b = bound(num,lower, upper);
		assert(b==-88);
		
		//testing using inputs for largest integer values possible
		int numLargest =  2147483647; //testing with likely largest number storable in Java x32
		num = numLargest;
		lower = -num; //using smallest number possible in Java
		upper = num;
		b = bound(num,lower, upper);
		assert(b==num);
		upper = -numLargest;
		b = bound(num,lower, upper);
		assertTrue(b==upper);
		
		
	}

	
}
