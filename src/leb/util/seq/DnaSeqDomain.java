package leb.util.seq;

public class DnaSeqDomain implements Comparable<DnaSeqDomain> {
	
	// Domain for DNA sequence
	private String title = null;
	private String sequence = null;
	private String quality = null;
	private int index = -1; // index (1 is base)
	
	public DnaSeqDomain() {};
	public DnaSeqDomain(String title, String sequence)
	{
		this.title=title;
		this.sequence=sequence;
	}
	
	public int compareTo(DnaSeqDomain o) {
	    if (!(o instanceof DnaSeqDomain))
	        throw new ClassCastException("A DnaSeqDomain object expected.");
	    if (this.index < o.index) return 1;
	    if (this.index > o.index) return -1;
		return 0;
	}
	
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

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void printAsLine() {
		System.out.println(index+"\t"+title+"\t"+sequence);
	}
	
	public void printAsLineNoSequence() {
		int length=-1;
		if (sequence!=null) length = sequence.length();
		System.out.println(index+"\t"+title+"\t"+length+" bp");
	}
	
	public Integer length() {
		if (sequence==null) return null;
		return sequence.length();
	}
}
