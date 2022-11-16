package leb.util.seq;

public class DnaSeqDomain {
	
	// Domain for DNA sequence
	private String title = null;
	private String sequence = null;
	
	public DnaSeqDomain() {}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSequence() {
		return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
	
	public Integer length() {
		if (sequence==null) return null;
		return sequence.length();
	}
}
