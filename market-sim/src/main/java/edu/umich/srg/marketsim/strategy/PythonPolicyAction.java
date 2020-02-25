package edu.umich.srg.marketsim.strategy;

import org.python.core.PyInstance;  
import org.python.util.PythonInterpreter;  

import com.google.gson.JsonArray;


public class PythonPolicyAction  
{  

   private PythonInterpreter interpreter = null;
   
   //private final JsonArray state;

   public PythonPolicyAction() {
	   PythonInterpreter.initialize(System.getProperties(),  
               System.getProperties(), new String[0]);
	   this.interpreter = new PythonInterpreter();
   } 
   
   public double getPolicyAction(JsonArray state) {
	   return 0;
   }

   void execfile( final String fileName )  
   {  
      this.interpreter.execfile(fileName);  
   }  

   PyInstance createClass( final String className, final String opts )  
   {  
      return (PyInstance) this.interpreter.eval(className + "(" + opts + ")");  
   }  

   public static void main( String gargs[] )  
   {  
      PythonPolicyAction ie = new PythonPolicyAction();  

      ie.execfile("hello.py");  

      PyInstance hello = ie.createClass("Hello", "None");  

      hello.invoke("run");  
   }  
} 