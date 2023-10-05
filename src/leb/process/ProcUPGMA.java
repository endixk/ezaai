package leb.process;

import java.util.List;
import java.util.LinkedList;

public class ProcUPGMA {
	
	private double[][] dmat;
	private final List<String>  leaves = new LinkedList<>();
	private final List<Double>  depths = new LinkedList<>();
	private final List<Integer> counts = new LinkedList<>();
	private int N;
	
	public ProcUPGMA(double[][] dmat, List<Integer> ids, List<String> labels, boolean useid) {
		this.dmat = dmat;
		if(useid) {
			for(int id : ids) {
				this.leaves.add(String.format("%d", id));
			}
		} else {
			this.leaves.addAll(labels);
		}
		
		N = labels.size();
		for(int i = 0; i < N; i++) {
			depths.add(.0);
			counts.add(1);
		}
	}
	
	private void iterate() {
		// find minimum pair
		int mi = -1, mj = -1;
		double md = 200.0;
		for(int i = 0; i < N-1; i++) {
			for(int j = i+1; j < N; j++) {
				if(dmat[i][j] < md) {
					md = dmat[i][j];
					mi = i;
					mj = j;
				}
			}
		}
		
		// compute new distance matrix
		double[][] umat = new double[N - 1][N - 1];
		int ni = 1;
		for(int i = 0; i < N; i++) {
			if(i - mi == 0 || i - mj == 0) continue;
			int nj = 1;
			for(int j = 0; j < N; j++) {
				if(j - mi == 0 || j - mj == 0) continue;
				if(i - j != 0) umat[ni][nj] = dmat[i][j];
				nj++;
			}
			double ad = (dmat[i][mi] * counts.get(mi) + dmat[i][mj] * counts.get(mj)) / (counts.get(mi) + counts.get(mj));
			umat[ni][0] = ad;
			umat[0][ni] = ad;
			ni++;
		}
		
		// update leaves
		String newLeaf = String.format("(%s:%f,%s:%f)", 
				leaves.get(mi), dmat[mi][mj]/2 - depths.get(mi), leaves.get(mj), dmat[mi][mj]/2 - depths.get(mj));
		leaves.remove(mj);
		leaves.remove(mi);
		leaves.add(0, newLeaf);
		
		// update depths
		double newDepth = dmat[mi][mj]/2;
		depths.remove(mj);
		depths.remove(mi);
		depths.add(0, newDepth);
		
		// update counts
		int newCount = counts.get(mi) + counts.get(mj);
		counts.remove(mj);
		counts.remove(mi);
		counts.add(0, newCount);
		
		// update distance matrix
		dmat = new double[N-1][N-1];
		for(int i = 0; i < N-1; i++) {
			System.arraycopy(umat[i], 0, dmat[i], 0, N - 1);
		}
		N--;
	}
	
	public String getTree() {
		while(N > 1) this.iterate();
		return this.leaves.get(0) + ";";
	}
}
