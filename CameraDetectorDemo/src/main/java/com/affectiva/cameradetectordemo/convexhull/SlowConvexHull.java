package com.affectiva.cameradetectordemo.convexhull;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Slow implementation of the Convex Hull algorithm
 * Original code from https://code.google.com/p/convex-hull/source/browse/Convex%20Hull/src/algorithms/SlowConvexHull.java
 * @author xurei <olivier.bourdoux@gmail.com>
 */
public class SlowConvexHull implements ConvexHullAlgorithm {
	
	@Override
	public ArrayList<PointF> execute(ArrayList<? extends PointF> PointFs) {
		int n = PointFs.size();
		
		ArrayList<LineSegment> edges = new ArrayList<LineSegment>();
		
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i == j)
					continue;
				
				boolean valid = true;
				
				for (int k = 0; k < n; k++) {
					if (k == i || k == j)
						continue;
					
					if (!rightOfLine(PointFs.get(k), new LineSegment(PointFs.get(i), PointFs.get(j)))) {
						valid = false;
						break;
					}
				}
				
				if (valid) {
					edges.add(new LineSegment(PointFs.get(i), PointFs.get(j)));
				}
			}
		}
		
		return sortedVertexList(edges);
	}
	
	private boolean rightOfLine(PointF p, LineSegment line) {
		return ((long) (line.p2.x - line.p1.x)) * ((long) (p.y - line.p1.y)) - ((long) (line.p2.y - line.p1.y)) * ((long) (p.x - line.p1.x)) < 0;
	}
	
	private boolean leftOfLine(PointF p, LineSegment line) {
		return ((long) (line.p2.x - line.p1.x)) * ((long) (p.y - line.p1.y)) - ((long) (line.p2.y - line.p1.y)) * ((long) (p.x - line.p1.x)) > 0;
	}
	
	private ArrayList<PointF> sortedVertexList(ArrayList<LineSegment> lines) {
		@SuppressWarnings("unchecked")
		ArrayList<LineSegment> xSorted = (ArrayList<LineSegment>) lines.clone();
		Collections.sort(xSorted, new XCompare());
		
		int n = xSorted.size();
		
		LineSegment baseLine = new LineSegment(xSorted.get(0).p1, xSorted.get(n - 1).p1);
		
		ArrayList<PointF> result = new ArrayList<PointF>();
		
		result.add(xSorted.get(0).p1);
		
		for (int i = 1; i < n; i++) {
			if (leftOfLine(xSorted.get(i).p1, baseLine)) {
				result.add(xSorted.get(i).p1);
			}
		}
		
		result.add(xSorted.get(n - 1).p1);
		
		for (int i = n - 2; i > 0; i--) {
			if (rightOfLine(xSorted.get(i).p1, baseLine)) {
				result.add(xSorted.get(i).p1);
			}
		}
		
		return result;
	}
	
	private class LineSegment {
		public PointF p1, p2;
		
		public LineSegment(PointF p1, PointF p2) {
			this.p1 = p1;
			this.p2 = p2;
		}
		
		@Override
		public String toString() {
			return p1.toString() + "," + p2.toString() + "\n";
		}
	}
	
	private class XCompare implements Comparator<LineSegment> {
		@Override
		public int compare(LineSegment o1, LineSegment o2) {
			return (new Float(o1.p1.x)).compareTo(new Float(o2.p1.x));
		}
	}
}
