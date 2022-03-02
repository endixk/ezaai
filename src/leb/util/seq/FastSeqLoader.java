package leb.util.seq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

enum FastSeqType {FASTA, FASTQ;}
public class FastSeqLoader {
	
	private BufferedReader br = null; //no get setter	
	private String line = null; //no get setter
	
	private FastSeqType fastSeqType = null;
	private Integer sumReadLength;
	private Integer seqCount;
	private Integer minLen;
	private Integer maxLen;
	private boolean hasNextSeq = false;
	private boolean ignoreEmptySeq = false;
	
	
	public static List<DnaSeqDomain> importFileToDomainList(String fileName){				
		List<DnaSeqDomain> list = new ArrayList<>();
		FastSeqLoader fsl = new FastSeqLoader();
		fsl.loadSeqFile(fileName);
		while(fsl.hasNextSeq()){
			DnaSeqDomain domain = fsl.nextSeq();
			if(domain!=null) list.add(domain);
		}
		
		try {
			fsl.br.ready();
			fsl.br.close();
		} catch (IOException e) {}
		return list;		
	}
	
	public FastSeqType loadSeqFile(File file){
		hasNextSeq = true;
		setSumReadLength(0);
		setSeqCount(0);
		minLen = Integer.MAX_VALUE;
		maxLen = Integer.MIN_VALUE;
		
		try {
			br = new BufferedReader(new FileReader(file));
			
			String firstLine = null;
			while((line = br.readLine())!=null){
				if(line.trim().length()==0) continue; // continue with empty line 
				else {
					firstLine = line;
//					br.reset(); // BufferedReader reset to first line
					break;
				}
			}
			if(firstLine==null){
				fastSeqType = null;
				throw new NullPointerException();	
			}			
			if(firstLine.startsWith("@")) {
				fastSeqType = FastSeqType.FASTQ;				
			}else if(firstLine.startsWith(">")) {
				fastSeqType = FastSeqType.FASTA;
			}
			else {
				fastSeqType = null;
				throw new NullPointerException();				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fastSeqType;	
	}

	public FastSeqType loadSeqFile(String fileName){		
		return loadSeqFile(new File(fileName));
	}
	
	public boolean hasNextSeq(){
		if(hasNextSeq){
			try {
				br.mark(100000000);
				if(br.readLine()!=null) {
					br.reset();
					return true;
				}else {					
					return false;
				}				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return hasNextSeq;				
	}
	
	public DnaSeqDomain nextSeq(){	
		DnaSeqDomain domain;
		if(fastSeqType==FastSeqType.FASTA) domain = nextFastaSeq();
		else if(fastSeqType==FastSeqType.FASTQ) domain = nextFastqSeq();		
		else {
			hasNextSeq = false;
			return null;
		}		
		if(domain==null) hasNextSeq = false;
		return domain;
	}
	
	private DnaSeqDomain nextFastaSeq(){
		
		String title;
		StringBuilder sbSeq = new StringBuilder();		
		DnaSeqDomain domain = new DnaSeqDomain();
				
		if(line==null){
			IOUtils.closeQuietly(br); 
			return null;
		}else if(line.startsWith(">")) {			
			title = line.substring(1);
		}
		else {
			throw new NullPointerException();
		}
		
		try {
			while ((line=br.readLine()) != null) {		
				if(line.startsWith(">")){
					break;
				}else {
					sbSeq.append(line.trim());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		String seq = sbSeq.toString();
		if(seq.length()==0) {
			if(!ignoreEmptySeq){										
				throw new NullPointerException("Empty Sequence with title : " + title);	
			}			
		}else{
			domain.setTitle(title);
			domain.setSequence(seq);			
		}							
		if(domain!=null) setVariables(domain);
		
		return domain;
	}
	
	private DnaSeqDomain nextFastqSeq() {

		String title;
		StringBuilder sbSeq = new StringBuilder();
		StringBuilder sbQual = new StringBuilder();
		DnaSeqDomain domain = new DnaSeqDomain();
				
		if(line==null){
			IOUtils.closeQuietly(br); 
			return null;
		}else if(line.startsWith("@")) {			
			title = line.substring(1);
		}else {
			throw new NullPointerException();
		}
		
		try {
			while(!(line=br.readLine()).startsWith("+")){
				sbSeq.append(line.trim());
			}
			while(sbQual.length()!=sbSeq.length()){
	              line=br.readLine();          
	              sbQual.append(line.trim());
	              if(sbQual.length()>sbSeq.length()){
	                throw new NullPointerException();            
	              }
			}
			while((line=br.readLine())!=null){
				if(line.trim().length()==0) continue;
				if(line.startsWith("@")) break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		String seq = sbSeq.toString();
		String qual = sbQual.toString();
		
		if(seq.length()==0 || qual.length()==0) {
			if(!ignoreEmptySeq){										
				throw new NullPointerException("Empty Sequence with title : " + title);	
			}							
		}
		
		domain.setTitle(title);
		domain.setSequence(seq);
		domain.setQuality(qual);
		
		if(domain!=null) setVariables(domain);
		return domain;
	}
	
	private void setVariables(DnaSeqDomain domain){		
		sumReadLength += domain.getSequence().length();
		seqCount++;
		if(minLen > domain.getSequence().length()) minLen = domain.getSequence().length();
		if(maxLen < domain.getSequence().length()) maxLen = domain.getSequence().length();
	}
	
	public void setSumReadLength(Integer sumReadLength) {
		this.sumReadLength = sumReadLength;
	}

	public void setSeqCount(Integer seqCount) {
		this.seqCount = seqCount;
	}
}
