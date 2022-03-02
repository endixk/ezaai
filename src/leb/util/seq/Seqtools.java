package leb.util.seq;

import org.biojava3.core.sequence.DNASequence;
import org.biojava3.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava3.core.sequence.compound.AmbiguityRNACompoundSet;
import org.biojava3.core.sequence.compound.AminoAcidCompound;
import org.biojava3.core.sequence.compound.DNACompoundSet;
import org.biojava3.core.sequence.compound.NucleotideCompound;
import org.biojava3.core.sequence.compound.RNACompoundSet;
import org.biojava3.core.sequence.template.CompoundSet;
import org.biojava3.core.sequence.template.Sequence;
import org.biojava3.core.sequence.transcription.TranscriptionEngine;

public class Seqtools {
	public static String translate_CDS(String dna_seq, int TRANSLATION_TABLE, boolean ambiguous) {
		if (dna_seq == null)
			return null;
		String protein_seq = null;
		try {
            DNASequence dnaSequence = null;
            CompoundSet<NucleotideCompound> dnaCompound = null;
            CompoundSet<NucleotideCompound> rnaCompound = null;
            
		    if(ambiguous){ // set ambiguous DNA compound
		        dnaCompound = AmbiguityDNACompoundSet.getDNACompoundSet();
		        rnaCompound = AmbiguityRNACompoundSet.getDNACompoundSet(); //biojava3 typing error
		    }
		    else{ // else 
		        dnaCompound = DNACompoundSet.getDNACompoundSet();
		        rnaCompound = RNACompoundSet.getRNACompoundSet();
		    }
            
		    dnaSequence = new DNASequence(dna_seq, dnaCompound);
		    
			TranscriptionEngine.Builder b = new TranscriptionEngine.Builder();
			b.table(TRANSLATION_TABLE).initMet(true).trimStop(true);
			
			// ambiguous or rigid compound sets
			b.dnaCompounds(dnaCompound).rnaCompounds(rnaCompound);
			
			TranscriptionEngine engine = b.build();
			
			Sequence<NucleotideCompound> rna = engine.getDnaRnaTranslator()
					.createSequence(dnaSequence);
			Sequence<AminoAcidCompound> protein = engine
					.getRnaAminoAcidTranslator().createSequence(rna);
			protein_seq = protein.getSequenceAsString();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return protein_seq;
	}
}
