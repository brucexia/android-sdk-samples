package com.affectiva.cameradetectordemo.convexhull;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Fast implementation of the Convex Hull algorithm
 * Original code from https://code.google.com/p/convex-hull/source/browse/Convex%20Hull/src/algorithms/FastConvexHull.java
 * @author xurei <olivier.bourdoux@gmail.com>
 */
public class FastConvexHull implements ConvexHullAlgorithm {
	
	@Override
	public ArrayList<PointF> execute(ArrayList<? extends PointF> PointFs) {
		@SuppressWarnings("unchecked")
		ArrayList<PointF> xSorted = (ArrayList<PointF>) PointFs.clone();
		Collections.sort(xSorted, new XCompare());
		if (xSorted.size() < 3)
			return xSorted;
		
		int n = xSorted.size();
		
		PointF[] lUpper = new PointF[n];
		
		lUpper[0] = xSorted.get(0);
		lUpper[1] = xSorted.get(1);
		
		int lUpperSize = 2;
		
		for (int i = 2; i < n; i++) {
			lUpper[lUpperSize] = xSorted.get(i);
			lUpperSize++;
			
			while (lUpperSize > 2 && !rightTurn(lUpper[lUpperSize - 3], lUpper[lUpperSize - 2], lUpper[lUpperSize - 1])) {
				// Remove the middle PointF of the three last
				lUpper[lUpperSize - 2] = lUpper[lUpperSize - 1];
				lUpperSize--;
			}
		}
		
		PointF[] lLower = new PointF[n];
		
		lLower[0] = xSorted.get(n - 1);
		lLower[1] = xSorted.get(n - 2);
		
		int lLowerSize = 2;
		
		for (int i = n - 3; i >= 0; i--) {
			lLower[lLowerSize] = xSorted.get(i);
			lLowerSize++;
			
			while (lLowerSize > 2 && !rightTurn(lLower[lLowerSize - 3], lLower[lLowerSize - 2], lLower[lLowerSize - 1])) {
				// Remove the middle PointF of the three last
				lLower[lLowerSize - 2] = lLower[lLowerSize - 1];
				lLowerSize--;
			}
		}
		
		ArrayList<PointF> result = new ArrayList<PointF>();
		
		for (int i = 0; i < lUpperSize; i++) {
			result.add(lUpper[i]);
		}
		
		for (int i = 1; i < lLowerSize - 1; i++) {
			result.add(lLower[i]);
		}
		
		return result;
	}
	
	private boolean rightTurn(PointF a, PointF b, PointF c) {
		//Since there is a multiplication, the values are converted to longs to avoid integer overflows
		return ((long) (b.x - a.x)) * ((long) (c.y - a.y)) - ((long) (b.y - a.y)) * ((long) (c.x - a.x)) > 0;
	}
	
	private class XCompare implements Comparator<PointF> {
		@Override
		public int compare(PointF o1, PointF o2) {
			return (new Float(o1.x)).compareTo(new Float(o2.x));
		}
	}
}

