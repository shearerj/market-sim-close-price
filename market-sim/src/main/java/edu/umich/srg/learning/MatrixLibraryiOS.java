package edu.umich.srg.learning;

import com.google.gson.JsonArray;

import no.uib.cipr.matrix.DenseMatrix;

import java.lang.Math;

public class MatrixLibraryiOS {
	
	//weightMtx.forEach(null);

	
	public DenseMatrix jsonToMtx(JsonArray matrixJson, int r, int c) {
		DenseMatrix mtx = new DenseMatrix(r,c);
		JsonArray rowJson = new JsonArray();
		for(int i=0; i<r; i++) {
			rowJson = matrixJson.get(i).getAsJsonArray();
			for(int j=0; j<c; j++) {
				mtx.add(i, j, rowJson.get(j).getAsDouble());
			}
		}
		return mtx;
	}
	
	public DenseMatrix jsonToVector(JsonArray vectorJson, int c) {
		DenseMatrix vec = new DenseMatrix(1,c);
		for(int j=0; j<c; j++) {
			vec.add(0, j, vectorJson.get(j).getAsDouble());
		}
		return vec;
	}
	
	
	public JsonArray vectorToJson(DenseMatrix vec, int c) {
		JsonArray vecJson = new JsonArray();
		for(int j=0; j<c; j++) {
			vecJson.add(vec.get(0,j));
		}
		return vecJson;
	}
	
	public DenseMatrix nnLinear(DenseMatrix input, DenseMatrix weight, DenseMatrix bias) {
		DenseMatrix output = new DenseMatrix(1,weight.numRows());
		DenseMatrix transWeight = new DenseMatrix(weight.numColumns(), weight.numRows());
		output = (DenseMatrix) input.mult(weight.transpose(transWeight), output);
		return (DenseMatrix) output.add(bias);
	}
	
	public DenseMatrix nnReLu(DenseMatrix input) {
		DenseMatrix output = new DenseMatrix(input);
		double entry;
		for(int  r=0; r < input.numRows(); r++) {
			for(int c=0; c < input.numColumns(); c++) {
				entry = input.get(r, c);
				if (entry < 0) {
					output.set(r,c,0);
				}
				else {
					output.set(r,c,entry);
				}
			}
		}
		return output;
	}
	
	public DenseMatrix nnTanh (DenseMatrix input) {
		DenseMatrix output = new DenseMatrix(input);
		double entry, numerator, denominator;
		for(int  r=0; r < input.numRows(); r++) {
			for(int c=0; c < input.numColumns(); c++) {
				entry = input.get(r, c);
				numerator = Math.exp(entry) - Math.exp(-1 * entry);
				denominator = Math.exp(entry) + Math.exp(-1 * entry);
				output.set(r, c, numerator / denominator);
			}
		}
		return output;
	}
	
}