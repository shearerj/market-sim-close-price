package utils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
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

		result = MathUtils.logn(number, base);
		assertTrue(result==1);//normal test case

		//log base 10 of 10 is 1
		number = 10;
		base = 10;
		result = logn(number, base);
		assertTrue(result==1);
		
		//boundary cases of number <= 0, as ln is undefined for those values
		//these ensure -1 is given where log n is undefined
		number = 0;
		base = 10;
		result = logn(number, base);
		assertTrue(result==-1);
		number=-999;
		result = logn(number,base);
		assertTrue(result==-1);
		

		//Pathological test cases like
		//number = .000000000000001; wouldn't apply as number is of type int
		
		
		//shows that logn function provided allows use of negative base values
		number = 10;
		base = -1; 
		result = logn(number, base);
		// 10/-1=-10, log =0, hence result is 0
		//it appears that in general, if the base is negative while the number is positive
		// result returned will be 0
		assertTrue(result==0);
		
		number=-2;
		result = logn(number,base);
		//as number provided is already negative, would not enter loop, returning -1
		assertTrue(result==-1);
		
		//show -1 is given if and only if starting number is already negative or zero
		number = 0;
		base = 2;
		result = logn(number, base);
		assertTrue(result==-1);
		number=-98;
		result = logn(number,base);
		assertTrue(result==-1);
		
		number = 5; base = 3;
		//if using double answer should be  0.69897000434
		result = logn(number,base);
		//however integer math forces result to be rounded to 1
		assertTrue(result==1);
		
		//7 log 9 is   0.88562187458
		number = 7; 
		base = 9;
		result = logn(number,base);
		assertTrue(result!=1);
		//as 7 > 0, 7/9 = 0.7778.. gets cut to 0 
		//stopping the incrementing of log at 0
		assertTrue(result==0);
		
		//62 log e is  4.127134385,
		//this is rounded due to repeated integer division to 3
		number =62; 
		base =3;//rounding as input is integer base
		result = logn(number,base);
		assertTrue(result!=4);
		//62/3=20.666, this is cut/truncated to 20, 
		//20/3 cut to 6, 6/3 is 2, 2/3 is cut to 0
		//resulting in result of -1+1(4) = 3
		//example of cases in which answer differs from double variant by more than 1 
		assertTrue(result==3);
		
		
		
		//39 log 37 is  1.591064607 
		number =39; 
		base =37;
		result = logn(number,base);
		assertTrue(result!=2);
		// results of repeated integer divisions, result in  1.59 being truncated to 1
		assertTrue(result==1);
	}

	@Test
	public void testingQuantizeInt()
	{ 	
		//standard input
		int n = 1001;
		int quanta= 10;
		int q = quantize(n, quanta);
		assertTrue(q==1000);
		
		//used to verify function rounds to positive infinity for negative numbers
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
		//verifies that  negative numbers below middle round up to quanta closer to 0
		n = -34.99999; 
		qresult = quantize(n, quanta);
		assertTrue(qresult==-30);
		
		//in cases where the double n is closer to the lower quanta, quantize rounds down to lower quanta
		n=-35.999999;
		qresult = quantize(n, quanta);
		assertTrue(qresult==-40);

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
		
		//testing valid results using repeated values 
		// for variables
		num=0; 
		lower = 0;
		upper = 1;
		b = bound(num,lower, upper);
		assertTrue(b==0);
		
		num=0; lower = 1; upper =0;
		b = bound(num,lower, upper);
		assertTrue(b!=0);
		
		num=0; lower = 0; upper =1;
		b = bound(num,lower, upper);
		assertTrue(b==0);
		
		//verifying case when all three inputs provided are same
		num = 0;
		lower = 0;
		upper = 0;
		b = bound(num,lower, upper);
		assertTrue(b==0);
		
		//testing with negative numbers and large values provided
		num = -999999899;
		//noting that this is the max length int num can have before IDE throws an error 
		lower = -88;
		upper = -875;
		b = bound(num,lower, upper);
		assertTrue(b==-88);
		
		//testing using inputs for largest integer values possible
		int numLargest =  2147483647; //testing with likely largest number storable in Java x32
		num = numLargest;
		lower = -num; //using smallest number possible in Java
		upper = num;
		b = bound(num,lower, upper);
		assertTrue(b==num);
		upper = -numLargest;
		b = bound(num,lower, upper);
		assertTrue(b==upper);
		
		
	}

	
}
