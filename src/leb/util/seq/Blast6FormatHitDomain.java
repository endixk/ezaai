package leb.util.seq;

public class Blast6FormatHitDomain {
	public String query = null;
	public String target = null;
	
	public double identity = 0;
	public int alignmentLength = 0;
	
	public int mismatch = 0;
	public int gap = 0;
	
	public int startInQuery = 0;
	public int endInQuery = 0;
	
	public int startInTarget = 0;
	public int endInTarget = 0;
	
	public double evalue = 0;
	public double bitScore = 0;

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public double getIdentity() {
		return identity;
	}

	public void setIdentity(double identity) {
		this.identity = identity;
	}

	public int getAlignmentLength() {
		return alignmentLength;
	}

	public void setAlignmentLength(int alignmentLength) {
		this.alignmentLength = alignmentLength;
	}

	public void setMismatch(int mismatch) {
		this.mismatch = mismatch;
	}

	public void setGap(int gap) {
		this.gap = gap;
	}

	public int getStartInQuery() {
		return startInQuery;
	}

	public void setStartInQuery(int startInQuery) {
		this.startInQuery = startInQuery;
	}

	public int getEndInQuery() {
		return endInQuery;
	}

	public void setEndInQuery(int endInQuery) {
		this.endInQuery = endInQuery;
	}

	public void setStartInTarget(int startInTarget) {
		this.startInTarget = startInTarget;
	}

	public void setEndInTarget(int endInTarget) {
		this.endInTarget = endInTarget;
	}

	public void setEvalue(double evalue) {
		this.evalue = evalue;
	}

	public void setBitScore(double bitScore) {
		this.bitScore = bitScore;
	}
	
	
}
