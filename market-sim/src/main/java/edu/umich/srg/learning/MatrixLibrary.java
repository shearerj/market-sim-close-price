package edu.umich.srg.learning;

import com.google.gson.JsonArray;

import java.lang.Math;

public class MatrixLibrary {
	
	//weightMtx.forEach(null);

	
	public double [][] jsonToMtx(JsonArray matrixJson, int r, int c) {
		double [][] mtx = new double[r][c];
		JsonArray rowJson = new JsonArray();
		for(int i=0; i<r; i++) {
			rowJson = matrixJson.get(i).getAsJsonArray();
			for(int j=0; j<c; j++) {
				mtx[i][j] = rowJson.get(j).getAsDouble();
			}
		}
		return mtx;
	}
	
	public double [][] jsonToVector(JsonArray vectorJson, int c) {
		double [][] vec = new double[1][c];
		for(int j=0; j<c; j++) {
			vec[0][j] = vectorJson.get(j).getAsDouble();
		}
		return vec;
	}
	
	
	public JsonArray vectorToJson(double [][] vec, int c) {
		JsonArray vecJson = new JsonArray();
		for(int j=0; j<c; j++) {
			vecJson.add(vec[0][j]);
		}
		return vecJson;
	}
	
	//Multiply the input by the transpose of the weight matrix, then add bias
	public double [][] nnLinear(double [][] input, double [][] weight, double [][] bias, int in_dim, int w_dim, int shared_dim) {
		double [][] output = new double [in_dim][w_dim];
		for(int r=0; r < in_dim; r++) {
			for(int c=0; c < w_dim; c++) {
				double entry = 0;
				for(int s=0; s < shared_dim; s++) {
					entry += input[r][s] * weight[c][s];
				}
				entry += bias[0][c];
				output[r][c] = entry;
			}
		}
		return output;
	}
	
	//Multiply the input by the transpose of the weight matrix, then add bias
	//Add ReLu function to make more efficient.
	public double [][] nnLinearRelu(double [][] input, double [][] weight, double [][] bias, int in_dim, int w_dim, int shared_dim) {
		double [][] output = new double [in_dim][w_dim];
		double entry;
		for(int r=0; r < in_dim; r++) {
			for(int c=0; c < w_dim; c++) {
				entry = 0;
				for(int s=0; s < shared_dim; s++) {
					entry += input[r][s] * weight[c][s];
				}
				entry += bias[0][c];
				if (entry<0) {entry = 0;}
				output[r][c] = entry;
			}
		}
		return output;
	}
	
	//Multiply the input by the transpose of the weight matrix, then add bias
	//Add tanh function to make more efficient.
	public double [][] nnLinearTanh(double [][] input, double [][] weight, double [][] bias, int in_dim, int w_dim, int shared_dim) {
		double [][] output = new double [in_dim][w_dim];
		double entry, numerator, denominator;
		for(int r=0; r < in_dim; r++) {
			for(int c=0; c < w_dim; c++) {
				entry = 0;
				for(int s=0; s < shared_dim; s++) {
					entry += input[r][s] * weight[c][s];
				}
				entry += bias[0][c];
				numerator = Math.exp(entry) - Math.exp(-1 * entry);
				denominator = Math.exp(entry) + Math.exp(-1 * entry);
				output[r][c] = numerator / denominator;
			}
		}
		return output;
	}
	
	public double [][] nnReLu(double [][] input, int in_r, int in_c) {
		double [][] output = new double [in_r][in_c];
		double entry;
		for(int  r=0; r < in_r; r++) {
			for(int c=0; c < in_c; c++) {
				entry = input[r][c];
				if (entry < 0) {
					output[r][c] = 0;
				}
				else {
					output[r][c] = entry;
				}
			}
		}
		return output;
	}
	
	public double [][] nnTanh (double [][] input, int in_r, int in_c) {
		double [][] output = new double [in_r][in_c];
		double entry, numerator, denominator;
		for(int  r=0; r < in_r; r++) {
			for(int c=0; c < in_c; c++) {
				entry = input[r][c];
				numerator = Math.exp(entry) - Math.exp(-1 * entry);
				denominator = Math.exp(entry) + Math.exp(-1 * entry);
				output[r][c] = numerator / denominator;
			}
		}
		return output;
	}
	
	public double [][] scaleAdd (double [][] A, double [][] B, double alpha, int r, int c) {
		double[][] output = new double[r][c];
		for(int i=0; i<r; i++) {
			for(int j=0; j<c; j++) {
				output[i][j] = alpha * (A[i][j] + B [i][j]);
			}
		}
		return output;
	}
	
	public double [][] norm (double [][] input, int c) {
		double[][] output = new double[1][c];
		double sum =0;
		for(int j=0; j<c; j++) {
			sum += input[0][j] * input[0][j];
		}
		double norm = Math.sqrt(sum);
		for(int j=0; j<c; j++) {
			output[0][j] = input[0][j] / norm;
		}
		return output;
	}
	
	public double [][] clip (double [][] input, int c, double lower, double upper) {
		double[][] output = new double[1][c];
		for(int j=0; j<c; j++) {
			if(input[0][j] < lower) {
				output[0][j] = lower;
			}
			else if(input[0][j] > upper) {
				output[0][j] = upper;
			}
			else {
				output[0][j] = input[0][j];
			}
		}
		return output;
	}
	
}