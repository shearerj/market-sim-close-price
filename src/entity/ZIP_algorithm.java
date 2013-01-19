package entity;


import java.util.Random;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shiva
 */
public class ZIP_algorithm {    
    private double tau0;
    private double delta0;    
    private double alpha1, alpha0;
    private double mu2;
    private double lambda;
    private double p1,p0;
    private double q1,q0;
    
    
    private double gamma;
    private double beta;
    private double c_A;
    private double c_R;
    
    private Random rand_R, rand_A;
    
    private boolean buy;
    
    private boolean DEBUG_ADVANCED_ENB = true;
    private boolean DEBUG_BASIC_ENB = false;

    public ZIP_algorithm(double gamma, double beta, double c_A, double c_R, 
            boolean buy, double lambda, double p1) {
        if(DEBUG_BASIC_ENB)
            DEBUG_ADVANCED_ENB = true;
        
        this.gamma = gamma;
        this.beta = beta;
        this.c_A = c_A;
        this.c_R = c_R;
        this.buy = buy;
        this.lambda = lambda;
        this.p1 = p1;
        p0 = 0.0;
        q1 = 0.0;
        q0 = 0.0;
        
        rand_R = new Random();
        rand_A = new Random();
        
        if(DEBUG_BASIC_ENB){
            System.out.println("Gamma = "+gamma);
            System.out.println("Beta = "+beta);
            System.out.println("c_A = "+c_A);
            System.out.println("c_R = "+c_R);
            
        }
    }
    
    private double getMu(){//Get Mu from the main update functon
        return ((p1-alpha1)/lambda) - 1.0;
    }
    
    private void updateAlpha(){
        alpha0 = alpha1;
        alpha1 = gamma*alpha0 + (1.0 - gamma)*delta0;
    }
    
    private void updateDelta(){
        delta0 = beta*(tau0 - p0);
    }
    
    private void updateTau(double q0){
        if(buy)
            tau0 = getR_inc()*q0 + getA_inc();
        else
            tau0 = (getR_inc() - c_R)*q0 + (getA_inc() - c_A);
    }
    
    /**
     * Returns the values for increase best price by default. To decrease best
     * price, simply subtract 'c'.
     * @return U[1, 1+c]
     */
    private double getR_inc(){
        return 1.0 + rand_R.nextDouble()*c_R;
    }

    /**
     * Returns the values for increase best price by default. To decrease best
     * price, simply subtract 'c'.
     * @return U[0, c]
     */
    private double getA_inc(){
        return rand_A.nextDouble()*c_A;
    }
    
    public double update(int p_old, int q){
        p0 = p1;
        p1 = (double)p_old;
        q0 = q1;
        q1 = (double) q;        
        updateTau(q0);
        updateDelta();
        updateAlpha();
        double mu =  getMu();
        
        if(DEBUG_ADVANCED_ENB){
            if(buy)
                System.out.println("Buying Price Adjustment:");
            else
                System.out.println("Selling Price Adjustment:");
            System.out.println("\t q0 = "+q0);
            System.out.println("\t q1 = "+q1);
            System.out.println("\t p0 = "+p0);
            System.out.println("\t p1 = "+p1);
            System.out.println("\t Tau = "+tau0);
            System.out.println("\t Delta = "+delta0);
            System.out.println("\t Alpha0 = "+alpha0);
            System.out.println("\t Alpha1 = "+alpha1);
            System.out.println("\t Mu = "+mu);
        }
        return mu;
    }
}
