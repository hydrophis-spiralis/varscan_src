/**
 * @(#)FpFilter.java
 *
 * Copyright (c) 2009-2010 Daniel C. Koboldt and Washington University in St. Louis
 *
 * COPYRIGHT
 */

package net.sf.varscan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import net.sf.varscan.SmartFileReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * A class for applying the false-positive filter to VarScan variant calls
 *
 * @version	2.4
 *
 * @author Daniel C. Koboldt https://sourceforge.net/u/dkoboldt
 *
 */
public class FpFilter {

	public FpFilter(String[] args)
	{
		// Set parameter defaults //

		double minRefReadPos = 0.10;
		double minRefDist3 = 0.10;
		double minVarReadPos = 0.10;
		double minVarDist3 = 0.10;
		double minStrandedness = 0.01;
		int minStrandReads = 5;

		int minRefAvgRL = 90;
		int minVarAvgRL = 90;
		double maxReadLenDiff = 0.25;

		double minVarFreq = 0.05;
		int minVarCount = 	4;
		int minVarCountLC = 	2;	// Min variant count for low-coverage sites
		double maxSomaticP = 1.00;
		int maxSomaticPdepth = 10;

		int maxVarMMQS = 100;
		int maxRefMMQS = 100;
		int maxMMQSdiff = 50;
		int minMMQSdiff = 0;
		int maxBAQdiff = 50;

		int minRefBaseQual = 15;
		int minVarBaseQual = 15;
		int minRefMapQual = 15;
		int minVarMapQual = 15;
		int maxMapQualDiff = 50;

		//		 Define the usage message //
		String usage = "USAGE: java -jar VarScan.jar fpfilter [variant file] [readcount file] OPTIONS\n" +
		"\tvariant file - A file of SNPs or indels in VarScan-native or VCF format\n" +
		"\treadcount file - The output file from bam-readcount for those positions\n" +
		"\t***For detailed filtering instructions, please visit http://varscan.sourceforge.net***\n" +
		"\n" +
		"\tOPTIONS:\n" +
		"\t--output-file\t\tOptional output file for filter-pass variants\n" +
		"\t--filtered-file\t\tOptional output file for filter-fail variants\n" +
		"\t--dream3-settings\tIf set to 1, optimizes filter parameters based on TCGA-ICGC DREAM-3 SNV Challenge results\n" +
		"\t--keep-failures\t\tIf set to 1, includes failures in the output file\n\n" +
		"\n" +
		"\tFILTERING PARAMETERS:\n" +
		"\t--min-var-count\t\tMinimum number of variant-supporting reads [" + minVarCount + "]\n" +
		"\t--min-var-count-lc\tMinimum number of variant-supporting reads when depth below somaticPdepth [" + minVarCountLC + "]\n" +
		"\t--min-var-freq\t\tMinimum variant allele frequency [" + minVarFreq + "]\n" +
		"\t--max-somatic-p\t\tMaximum somatic p-value [" + maxSomaticP + "]\n" +
		"\t--max-somatic-p-depth\tDepth required to test max somatic p-value [" + maxSomaticPdepth + "]\n" +
		"\t--min-ref-readpos\tMinimum average read position of ref-supporting reads [" + minRefReadPos + "]\n" +
		"\t--min-var-readpos\tMinimum average read position of var-supporting reads [" + minVarReadPos + "]\n" +
		"\t--min-ref-dist3\t\tMinimum average distance to effective 3' end (ref) [" + minRefDist3 + "]\n" +
		"\t--min-var-dist3\t\tMinimum average distance to effective 3' end (var) [" + minVarDist3 + "]\n" +
		"\t--min-strandedness\tMinimum fraction of variant reads from each strand [" + minStrandedness + "]\n" +
		"\t--min-strand-reads\tMinimum allele depth required to perform the strand tests [" + minStrandReads + "]\n" +
		"\t--min-ref-basequal\tMinimum average base quality for ref allele [" + minRefBaseQual + "]\n" +
		"\t--min-var-basequal\tMinimum average base quality for var allele [" + minVarBaseQual + "]\n" +
		"\t--max-basequal-diff\t\tMaximum average base quality diff (ref - var) [" + maxBAQdiff + "]\n" +
		"\t--min-ref-avgrl\t\tMinimum average trimmed read length for ref allele [" + minRefAvgRL + "]\n" +
		"\t--min-var-avgrl\t\tMinimum average trimmed read length for var allele [" + minVarAvgRL + "]\n" +
		"\t--max-rl-diff\t\tMaximum average relative read length difference (ref - var) [" + maxReadLenDiff + "]\n" +
		"\t--max-ref-mmqs\t\tMaximum mismatch quality sum of reference-supporting reads [" + maxRefMMQS + "]\n" +
		"\t--max-var-mmqs\t\tMaximum mismatch quality sum of variant-supporting reads [" + maxVarMMQS + "]\n" +
		"\t--min-mmqs-diff\t\tMinimum average mismatch quality sum (var - ref) [" + minMMQSdiff + "]\n" +
		"\t--max-mmqs-diff\t\tMaximum average mismatch quality sum (var - ref) [" + maxMMQSdiff + "]\n" +
		"\t--min-ref-mapqual\tMinimum average mapping quality for ref allele [" + minRefMapQual + "]\n" +
		"\t--min-var-mapqual\tMinimum average mapping quality for var allele [" + minVarMapQual + "]\n" +
		"\t--max-mapqual-diff\tMaximum average mapping quality (ref - var) [" + maxMapQualDiff + "]";


		String outFileName = "";
		String filteredFileName = "";

		// Adjust parameters based on user input //

		HashMap<String, String> params = VarScan.getParams(args);

		try
		{
			if(params.containsKey("output-file"))
				outFileName = params.get("output-file");

			if(params.containsKey("filtered-file"))
				filteredFileName = params.get("filtered-file");

			if(params.containsKey("dream3-settings"))
			{
				// Alter the parameter for optimal DREAM-3 performance //
				minVarCount = 3;
				minVarCountLC = 1;
				minStrandedness = 0;
				minVarBaseQual = 30;
				minRefReadPos = 0.20;
				minRefDist3 = 0.20;
				minVarReadPos = 0.15;
				minVarDist3 = 0.15;
				maxReadLenDiff = 0.05;
				maxMapQualDiff = 10;
				minRefMapQual = 20;
				minVarMapQual = 30;
				maxVarMMQS = 100;
				maxRefMMQS = 50;
			}

			if(params.containsKey("min-var-count"))
				 minVarCount = Integer.parseInt(params.get("min-var-count"));

			if(params.containsKey("min-var-count-lc"))
				 minVarCountLC = Integer.parseInt(params.get("min-var-count-lc"));

			if(params.containsKey("min-var-freq"))
				 minVarFreq = Double.parseDouble(params.get("min-var-freq"));

			if(params.containsKey("max-somatic-p"))
				 maxSomaticP = Double.parseDouble(params.get("max-somatic-p"));

			if(params.containsKey("max-somatic-p-depth"))
				 maxSomaticPdepth = Integer.parseInt(params.get("max-somatic-p-depth"));

			if(params.containsKey("min-ref-readpos"))
				 minRefReadPos = Double.parseDouble(params.get("min-ref-readpos"));

			if(params.containsKey("min-var-readpos"))
				 minVarReadPos = Double.parseDouble(params.get("min-var-readpos"));

			if(params.containsKey("min-ref-dist3"))
				 minRefDist3 = Double.parseDouble(params.get("min-ref-dist3"));

			if(params.containsKey("min-var-dist3"))
				 minVarDist3 = Double.parseDouble(params.get("min-var-dist3"));

			if(params.containsKey("min-strandedness"))
				 minStrandedness = Double.parseDouble(params.get("min-strandedness"));

			if(params.containsKey("min-strand-reads"))
				 minStrandReads = Integer.parseInt(params.get("min-strand-reads"));

			if(params.containsKey("min-ref-mapqual"))
				 minRefMapQual = Integer.parseInt(params.get("min-ref-mapqual"));

			if(params.containsKey("min-var-mapqual"))
				 minVarMapQual = Integer.parseInt(params.get("min-var-mapqual"));

			if(params.containsKey("min-ref-avgrl"))
				 minRefAvgRL = Integer.parseInt(params.get("min-ref-avgrl"));

			if(params.containsKey("min-var-avgrl"))
				 minVarAvgRL = Integer.parseInt(params.get("min-var-avgrl"));

			if(params.containsKey("max-rl-diff"))
				 maxReadLenDiff = Double.parseDouble(params.get("max-rl-diff"));

			if(params.containsKey("min-ref-basequal"))
				 minRefBaseQual = Integer.parseInt(params.get("min-ref-basequal"));

			if(params.containsKey("min-var-basequal"))
				 minVarBaseQual = Integer.parseInt(params.get("min-var-basequal"));

			if(params.containsKey("max-basequal-diff"))
				 maxBAQdiff = Integer.parseInt(params.get("max-basequal-diff"));

			if(params.containsKey("max-var-mmqs"))
				 maxVarMMQS = Integer.parseInt(params.get("max-var-mmqs"));

			if(params.containsKey("max-ref-mmqs"))
				 maxRefMMQS = Integer.parseInt(params.get("max-ref-mmqs"));

			if(params.containsKey("max-mmqs-diff"))
				 maxMMQSdiff = Integer.parseInt(params.get("max-mmqs-diff"));

			if(params.containsKey("min-mmqs-diff"))
				 minMMQSdiff = Integer.parseInt(params.get("min-mmqs-diff"));

			if(params.containsKey("max-mapqual-diff"))
				 maxMapQualDiff = Integer.parseInt(params.get("max-mapqual-diff"));
		}
		catch(Exception e)
		{
	    	System.err.println("Input Parameter Threw Exception: " + e.getLocalizedMessage());
	    	e.printStackTrace(System.err);
	    	System.exit(1);
		}

		// Print usage if -h or --help invoked //
		if(params.containsKey("help") || params.containsKey("h"))
		{
			System.err.println(usage);
			return;
		}
		// Print usage if -h or --help invoked //
		else if(args.length < 3)
		{
			System.err.println("ERROR: Input files not provided!\n");
			System.err.println(usage);
			return;
		}


	    // Define two-decimal-place format and statistics hash //
	    DecimalFormat twoDigits = new DecimalFormat("#0.00");
	    DecimalFormat threeDigits = new DecimalFormat("#0.000");

	    HashMap<String, Integer> stats = new HashMap<String, Integer>();
	    stats.put("numVariants", 0);
	    stats.put("numWithRC", 0);
	    stats.put("numWithReads1", 0);
	    stats.put("numPassFilter", 0);
	    stats.put("numFailFilter", 0);
	    stats.put("numFailNoRC", 0);
	    stats.put("numFailVarCount", 0);
	    stats.put("numFailVarFreq", 0);
	    stats.put("numFailStrand", 0);
	    stats.put("numFailSomaticP", 0);
	    stats.put("numFailRefReadPos", 0);
	    stats.put("numFailRefDist3", 0);
	    stats.put("numFailVarReadPos", 0);
	    stats.put("numFailVarDist3", 0);
	    stats.put("numFailVarMMQS", 0);
	    stats.put("numFailRefMMQS", 0);
	    stats.put("numFailMMQSdiff", 0);
	    stats.put("numFailMinMMQSdiff", 0);
	    stats.put("numFailRefMapQual", 0);
	    stats.put("numFailVarMapQual", 0);
	    stats.put("numFailRefBaseQual", 0);
	    stats.put("numFailVarBaseQual", 0);
	    stats.put("numFailMaxBAQdiff", 0);
	    stats.put("numFailMapQualDiff", 0);
	    stats.put("numFailReadLenDiff", 0);
	    stats.put("numFailRefAvgRL", 0);
	    stats.put("numFailVarAvgRL", 0);


	    try
	    {
	    	// Check for presence of files //
	    	File variantFile = new File(args[1]);
	    	File readcountFile = new File(args[2]);

	    	if(!variantFile.exists() || !readcountFile.exists())
	    	{
				System.err.println("ERROR: One of your input files is missing!\n");
				System.err.println(usage);
				return;
	    	}


	    	// Declare file-parsing variables //

	    	String line;
	    	int lineCounter = 0;
	    	boolean isVCF = false;

//	    	if(args[1].endsWith(".vcf"))
//	    		isVCF = true;

	    	HashMap<String, String> readcounts = new HashMap<String, String>();
	    	System.err.println("Loading readcounts from " + args[2] + "...");
	    	readcounts = loadReadcounts(args[2]);
	    	System.err.println("Parsing variants from " + args[1] + "...");

    		if(variantFile.exists())
    		{
    			BufferedReader in = new BufferedReader(new SmartFileReader(variantFile));

    			if(in.ready())
    			{
    				// Declare output files //
    				PrintStream outFile = null;
    				if(params.containsKey("output-file"))
    					outFile = new PrintStream( new FileOutputStream(outFileName) );

    				PrintStream filteredFile = null;
    				if(params.containsKey("filtered-file"))
    					filteredFile = new PrintStream( new FileOutputStream(filteredFileName) );

    				String vcfHeaderInfo = "";
    				vcfHeaderInfo = "##FILTER=<ID=VarCount,Description=\"Fewer than " + minVarCount + " variant-supporting reads\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=VarFreq,Description=\"Variant allele frequency below " + minVarFreq + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=VarReadPos,Description=\"Relative average read position < " + minVarReadPos + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=VarDist3,Description=\"Average distance to effective 3' end < " + minVarDist3 + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=VarMMQS,Description=\"Average mismatch quality sum for variant reads > " + maxVarMMQS + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=VarMapQual,Description=\"Average mapping quality of variant reads < " + minVarMapQual + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=VarBaseQual,Description=\"Average base quality of variant reads < " + minVarBaseQual + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=Strand,Description=\"Strand representation of variant reads < " + minStrandedness + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=RefMapQual,Description=\"Average mapping quality of reference reads < " + minRefMapQual + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=RefBaseQual,Description=\"Average base quality of reference reads < " + minRefBaseQual + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=MMQSdiff,Description=\"Mismatch quality sum difference (ref - var) > " + maxMMQSdiff + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=MapQualDiff,Description=\"Mapping quality difference (ref - var) > " + maxMapQualDiff + "\">";
    				vcfHeaderInfo += "\n##FILTER=<ID=ReadLenDiff,Description=\"Average supporting read length difference (ref - var) > " + maxReadLenDiff + "\">";


    	    		// Parse the infile line by line //

    	    		while ((line = in.readLine()) != null)
    	    		{
    	    			lineCounter++;

    	    			try
    	    			{
    		    			String[] lineContents = line.split("\t");
    		    			String chrom = lineContents[0];
    		    			String failReason = "";
    		    			boolean filterFlag = false;

    		    			if(chrom.equals("Chrom") || chrom.equals("chrom") || line.startsWith("#"))
    		    			{
        	    				if(line.startsWith("#"))
        	    					isVCF = true;

    		    				// Print header //
    		    				if(isVCF)
    		    				{
	    	    					if(params.containsKey("output-file"))
	    	    					{
	    	    						if(line.startsWith("#CHROM"))
	    	    							outFile.println(vcfHeaderInfo);

	    	    						outFile.println(line);
	    	    					}

	    	    					if(params.containsKey("filtered-file"))
	    	    					{
	    	    						if(line.startsWith("#CHROM"))
	    	    							filteredFile.println(vcfHeaderInfo);

	    	    						filteredFile.println(line);
	    	    					}
    		    				}
    		    				else
    		    				{
    		    					String filterHeader = "ref_reads\tvar_reads\tref_strand\tvar_strand\tref_basequal\tvar_basequal\tref_readpos\tvar_readpos\tref_dist3\tvar_dist3\tref_mapqual\tvar_mapqual\tmapqual_diff\tref_mmqs\tvar_mmqs\tmmqs_diff\tref_avg_rl\tvar_avg_rl\tavg_rl_diff\tfilter_status";
	    	    					if(params.containsKey("output-file"))
	    	    						outFile.println(line + "\t" + filterHeader);
	    	    					if(params.containsKey("filtered-file"))
	    	    						filteredFile.println(line + "\t" + filterHeader);
    		    				}

    		    			}
    		    			else
    		    			{
    		    				stats.put("numVariants", (stats.get("numVariants") + 1));
    		    				int position = Integer.parseInt(lineContents[1]);
    			    			String positionKey = chrom + "\t" + position;
    			    			boolean isIndel = false;

    			    			// Reference-allele values //
    							int refReads = 0;
    							double refMapQual = 0;
    							double refBaseQual = 0;
    							int refReadsPlus = 0;
    							int refReadsMinus = 0;
    							double refPos = 0;
    							double refSubs = 0;
    							double refMMQS = 0;
    							double refRL = 0;
    							double refDist3 = 0;

    							// Variant-allele values //
    							int varReads = 0;
    							double varMapQual = 0;
    							double varBaseQual = 0;
    							int varReadsPlus = 0;
    							int varReadsMinus = 0;
    							double varPos = 0;
    							double varSubs = 0;
    							double varMMQS = 0;
    							double varRL = 0;
    							double varDist3 = 0;

    							// Calculated values //
    							double varFreq = 0;
    							double varStrandedness = -1;
    							double refStrandedness = -1;
    							double mmqsDiff = 0;
    							double basequalDiff = 0;
    							double mapQualDiff = 0;
    							double avgReadLenDiff = 0;

    							// Parsed values //

    							String somaticStatus = "";
    							double somaticPvalue = 1.00;


//    			    			if(readcounts.containsKey(positionKey))
  //  			    			{
			    				try {
    			    				String refCounts = "";
    			    				String varCounts = "";
    			    				String ref = "";
    			    				String alt = "";

        	    					if(isVCF)
        	    					{
        	    						// VARSCAN VCF FORMAT //

        	    						// Parse the info field //
        	    						try {
            	    						String info = lineContents[7];
            	    						String[] infoContents = info.split(";");
            	    						for(int colCounter = 0; colCounter < infoContents.length; colCounter++)
            	    						{
            	    							String element = infoContents[colCounter];
            	    							String[] elementContents = element.split("=");
            	    							if(elementContents[0].equals("SS"))
            	    								somaticStatus = elementContents[1];
            	    							else if(elementContents[0].equals("GPV") && somaticStatus.equals("1"))
            	    								somaticPvalue = Double.parseDouble(elementContents[1]);
            	    							else if(elementContents[0].equals("SPV") && !somaticStatus.equals("1"))
            	    								somaticPvalue = Double.parseDouble(elementContents[1]);
            	    						}
        	    						}
        	    						catch (Exception e)
        	    						{
        	    							System.err.println("Somatic P-value ignored due to parsing exception for " + positionKey + ": " + e.getLocalizedMessage());
        	    						}


        	    						// Filter only applied to the first alt //
        	    						ref = lineContents[3];
        	    						String alts = lineContents[4];

        	    						String[] altContents = alts.split(",");
        	    						alt = altContents[0];
        	    						// Check for and convert indel //
        	    						if(ref.length() > 1)
        	    						{
        	    							isIndel = true;
        	    							// Deletion //
        	    							String thisVar = ref.replaceFirst(alt, "");
        	    							ref = alt;
        	    							alt = "-" + thisVar;
        	    						}
        	    						else if(alt.length() > 1)
        	    						{
        	    							// Insertion //
        	    							isIndel = true;
        	    							String thisVar = alt.replaceFirst(ref, "");
        	    							alt = "+" + thisVar;
        	    						}
        	    					}
        	    					else
        	    					{
        	    						// VARSCAN NATIVE OUTPUT FORMAT //
        	    						try {
	        	    						if(lineContents.length > 14)
	        	    						{
	            	    						somaticStatus = lineContents[12];
	            	    						if(somaticStatus.equals("Germline"))
	            	    						{
	            	    							somaticPvalue = Double.parseDouble(lineContents[13]);
	            	    						}
	            	    						else
	            	    						{
	            	    							somaticPvalue = Double.parseDouble(lineContents[14]);
	            	    						}
	        	    						}
        	    						}
        	    						catch (Exception e)
        	    						{
        	    							System.err.println("Somatic P-value ignored due to parsing exception for " + positionKey + ": " + e.getLocalizedMessage());
        	    						}



        	    						ref = lineContents[2];
        	    						String cns = lineContents[3];
        	    						if(ref.matches("-?\\d+(\\.\\d+)?"))
        	    						{
        	    							// Adjust file lists chr_start and chr_stop instead of position //
        	    							ref = lineContents[3];
        	    							cns = lineContents[4];
        	    						}

        	    						if(cns.length() > 1)
        	    						{
        	    							isIndel = true;
        	    							// CONVERT INDEL //
        	    							if(cns.contains("/"))
        	    							{
            	    							String[] indelContents = cns.split("/");
            	    							if(indelContents.length > 1)
            	    								alt = indelContents[1];
        	    							}
        	    							else
        	    							{
        	    								alt = cns;
        	    							}

        	    						}
        	    						else
        	    						{
        	    							// CONVERT SNV //
            	    						alt = VarScan.getVarAllele(ref, cns);

        	    						}

        	    					}

        	    					if(alt.length() > 0)
        	    					{
        	    						// Set a SNV style variant key as the default //
        	    						String varKey = chrom + "\t" + position + "\t" + alt;

        	    						// Determine if we have an indel. If so, muddle around until we find the right varKey //
        	    						if(ref.length() > 1 || alt.length() > 1)
        	    						{
        	    							isIndel = true;
        	    							int indelSize = 0;
        	    							if(ref.length() > 1)
        	    								indelSize = ref.length() - 1;
        	    							else
        	    								indelSize = alt.length() - 1;

        	    							if(readcounts.containsKey(varKey))
        	    							{
        	    								// Keep this perfect match //

        	    							}
        	    							else
        	    							{
        	    								// Shimmy back and forth out to size of indel to look for this //
            	    							for(int windowSize = indelSize + 1; windowSize >= 1; windowSize--)
            	    							{
                	    							String testKey1 = chrom + "\t" + (position - windowSize) + "\t" + alt;
                	    							String testKey2 = chrom + "\t" + (position + windowSize) + "\t" + alt;

                	    							if(readcounts.containsKey(testKey1))
                	    							{
                	    								varKey = testKey1;
                	    							}
                	    							else if(readcounts.containsKey(testKey2))
                	    							{
                	    								varKey = testKey2;
                	    							}

            	    							}
        	    							}


        	    						}


        	    						if(readcounts.containsKey(varKey))
        	    						{
        	    							stats.put("numWithRC", (stats.get("numWithRC") + 1));

        	    							// Try to split out values and proceed with filter comparison //
        	    							try {
        	    								String[] rcContents = readcounts.get(varKey).split("\t");
        	    								int readDepth = Integer.parseInt(rcContents[0]);
        	    								varCounts = rcContents[1];
            	    							String[] varContents = varCounts.split(":");

            	    							// Parse out variant allele values //

            	    							varReads = Integer.parseInt(varContents[1]);
            	    							varMapQual = Double.parseDouble(varContents[2]);
            	    							varBaseQual = Double.parseDouble(varContents[3]);
            	    							varReadsPlus = Integer.parseInt(varContents[5]);
            	    							varReadsMinus = Integer.parseInt(varContents[6]);
            	    							varPos = Double.parseDouble(varContents[7]);
            	    							varSubs = Double.parseDouble(varContents[8]);
            	    							varMMQS = Double.parseDouble(varContents[9]);
            	    							varRL = Double.parseDouble(varContents[12]);
            	    							varDist3 = Double.parseDouble(varContents[13]);

            	    							String refKey = chrom + "\t" + position + "\t" + ref;
            	    							if(readcounts.containsKey(refKey))
            	    							{
            	    								String[] rcContents2 = readcounts.get(refKey).split("\t");
            	    								int readDepth2 = Integer.parseInt(rcContents2[0]);

            	    								refCounts = rcContents2[1];
                	    							// Parse out reference allele values //
                	    							String[] refContents = refCounts.split(":");

                	    							refReads = Integer.parseInt(refContents[1]);
                	    							refMapQual = Double.parseDouble(refContents[2]);
                	    							refBaseQual = Double.parseDouble(refContents[3]);
                	    							refReadsPlus = Integer.parseInt(refContents[5]);
                	    							refReadsMinus = Integer.parseInt(refContents[6]);
                	    							refPos = Double.parseDouble(refContents[7]);
                	    							refSubs = Double.parseDouble(refContents[8]);
                	    							refMMQS = Double.parseDouble(refContents[9]);
                	    							refRL = Double.parseDouble(refContents[12]);
                	    							refDist3 = Double.parseDouble(refContents[13]);

                	    							// ONLY APPLY THESE FILTERS IF WE HAVE 2+ REF-SUPPORTING READS //

                	    							if(refReads >= 2)
                	    							{
                    	    							// Variant read position for ref-supporting reads //

                    	    							if(refPos < minRefReadPos)
                    	    							{
                    	    								if(failReason.length() > 0)
                    	    									failReason += ",";
                    	    								failReason += "RefReadPos";
                    	    								stats.put("numFailRefReadPos", (stats.get("numFailRefReadPos") + 1));
                    	    							}

                    	    							if(refDist3 < minRefDist3)
                    	    							{
                    	    								if(failReason.length() > 0)
                    	    									failReason += ",";
                    	    								failReason += "RefDist3";
                    	    								stats.put("numFailRefDist3", (stats.get("numFailRefDist3") + 1));
                    	    							}

                    	    							// Look at ref average mapping quality //

                    	    							if(refReads > 0 && refMapQual < minRefMapQual)
                    	    							{
                    	    								if(failReason.length() > 0)
                    	    									failReason += ",";
                    	    								failReason += "RefMapQual";
                    	    								stats.put("numFailRefMapQual", (stats.get("numFailRefMapQual") + 1));

                    	    							}

                    	    							// Look at ref MMQS //

                    	    							if(refMMQS > maxRefMMQS)
                    	    							{
                    	    								if(failReason.length() > 0)
                    	    									failReason += ",";
                    	    								failReason += "RefMMQS";
                    	    								stats.put("numFailRefMMQS", (stats.get("numFailRefMMQS") + 1));
                    	    							}

                    	    							// Look at ref average read length //

                    	    							if(!isIndel && refReads > 0 && refRL < minRefAvgRL)
                    	    							{
                    	    								if(failReason.length() > 0)
                    	    									failReason += ",";
                    	    								failReason += "RefAvgRL";
                    	    								stats.put("numFailRefAvgRL", (stats.get("numFailRefAvgRL") + 1));
                    	    							}
                	    							}


            	    							}


            	    							// APPLY VARIANT-READ-BASED FILTERS //
            	    							int totalDepth = refReads + varReads;

            	    							// SOMATIC P-VALUE FILTER //

            	    							// Only apply if we met sufficient depth //
            	    							if(totalDepth >= maxSomaticPdepth)
            	    							{
            	    								// Only apply if a p-value specified and a valid value parsed //
            	    								if(maxSomaticP != 1 && somaticPvalue != 1)
            	    								{
            	    									if(somaticPvalue > maxSomaticP)
            	    									{
                    	    								if(failReason.length() > 0)
                    	    									failReason += ",";
                    	    								failReason += "SomaticP";
                    	    								stats.put("numFailSomaticP", (stats.get("numFailSomaticP") + 1));
            	    									}
            	    								}
            	    							}


            	    							if(varReads < minVarCount)
            	    							{
            	    								// Pass this site if variant-supporting read count meets min for low-cov //
            	    								if(totalDepth < maxSomaticPdepth && varReads >= minVarCountLC)
            	    								{
            	    									// PASS//

            	    								}
            	    								else
            	    								{
                	    								if(failReason.length() > 0)
                	    									failReason += ",";
                	    								failReason += "VarCount";
                	    								stats.put("numFailVarCount", (stats.get("numFailVarCount") + 1));
            	    								}
            	    							}

            	    							// Compute variant allele frequency //

            	    							varFreq = (double) varReads / (double) readDepth;

            	    							if(varFreq < minVarFreq)
            	    							{
            	    								if(failReason.length() > 0)
            	    									failReason += ",";
            	    								failReason += "VarFreq";
            	    								stats.put("numFailVarFreq", (stats.get("numFailVarFreq") + 1));
            	    							}

            	    							// Variant read position //
            	    							if(varPos < minVarReadPos)
            	    							{
            	    								if(failReason.length() > 0)
            	    									failReason += ",";
            	    								failReason += "VarReadPos";
            	    								stats.put("numFailVarReadPos", (stats.get("numFailVarReadPos") + 1));
            	    							}

            	    							if(varDist3 < minVarDist3)
            	    							{
            	    								if(failReason.length() > 0)
            	    									failReason += ",";
            	    								failReason += "VarDist3";
            	    								stats.put("numFailVarDist3", (stats.get("numFailVarDist3") + 1));
            	    							}


            	    							// Look at variant MMQS and MMQS diff //

            	    							if(varMMQS > maxVarMMQS)
            	    							{
            	    								if(failReason.length() > 0)
            	    									failReason += ",";
            	    								failReason += "VarMMQS";
            	    								stats.put("numFailVarMMQS", (stats.get("numFailVarMMQS") + 1));
            	    							}

            	    							// Apply minimum average mapqual checks //


            	    							if(varReads > 0 && varMapQual < minVarMapQual)
            	    							{
            	    								if(failReason.length() > 0)
            	    									failReason += ",";
            	    								failReason += "VarMapQual";
            	    								stats.put("numFailVarMapQual", (stats.get("numFailVarMapQual") + 1));
            	    							}

            	    							// Apply minimum average basequal checks //

            	    							if(refReads > 0 && refBaseQual < minRefBaseQual)
            	    							{
            	    								if(failReason.length() > 0)
            	    									failReason += ",";
            	    								failReason += "RefBaseQual";
            	    								stats.put("numFailRefBaseQual", (stats.get("numFailRefBaseQual") + 1));

            	    							}

            	    							if(!isIndel && varReads > 0 && varBaseQual < minVarBaseQual)
            	    							{
            	    								if(failReason.length() > 0)
            	    									failReason += ",";
            	    								failReason += "VarBaseQual";
            	    								stats.put("numFailVarBaseQual", (stats.get("numFailVarBaseQual") + 1));
            	    							}

            	    							if(!isIndel && varReads > 0 && varRL < minVarAvgRL)
            	    							{
            	    								if(failReason.length() > 0)
            	    									failReason += ",";
            	    								failReason += "VarAvgRL";
            	    								stats.put("numFailVarAvgRL", (stats.get("numFailVarAvgRL") + 1));
            	    							}

            	    							// IF we have enough reads supporting the variant, check strand //

            	    							if(refReads >= minStrandReads && refReads > 0)
            	    							{
            	    								refStrandedness = (double) refReadsPlus / (double) refReads;
            	    							}

            	    							if(varReads >= minStrandReads && varReads > 0)
            	    							{
            	    								varStrandedness = (double) varReadsPlus / (double) varReads;

        	    									if(varStrandedness < minStrandedness || (1 - varStrandedness) < minStrandedness)
        	    									{
                	    								if(failReason.length() > 0)
                	    									failReason += ",";
                	    								failReason += "Strand";
                	    								stats.put("numFailStrand", (stats.get("numFailStrand") + 1));
//                    	    								System.err.println(positionKey + "\t" + refReadsPlus + "\t" + refReadsMinus + "\t" + refStrandedness + "\t" + varReadsPlus + "\t" + varReadsMinus + "\t" + varStrandedness);
        	    									}

            	    							}



            	    							// REF AND VAR COMPARISONS: REQUIRE 2+ READS FOR EACH ALLELE //

            	    							if(refReads >= 2 && varReads >= 2)
            	    							{
            	    								stats.put("numWithReads1", (stats.get("numWithReads1") + 1));
            	    								// Only make difference comparisons if we had reference reads //

            	    								basequalDiff = refBaseQual - varBaseQual;

                	    							if(basequalDiff > maxBAQdiff)
                	    							{
                	    								if(failReason.length() > 0)
                	    									failReason += ",";
                	    								failReason += "MaxBAQdiff";
                	    								stats.put("numFailMaxBAQdiff", (stats.get("numFailMaxBAQdiff") + 1));
                	    							}

                	    							mmqsDiff = varMMQS - refMMQS;

                	    							if(mmqsDiff > maxMMQSdiff)
                	    							{
                	    								if(failReason.length() > 0)
                	    									failReason += ",";
                	    								failReason += "MMQSdiff";
                	    								stats.put("numFailMMQSdiff", (stats.get("numFailMMQSdiff") + 1));
                	    							}

                	    							if(minMMQSdiff > 0 && mmqsDiff < minMMQSdiff)	// Min MMQS diff; only apply if set above 0
                	    							{
                	    								if(failReason.length() > 0)
                	    									failReason += ",";
                	    								failReason += "MinMMQSdiff";
                	    								stats.put("numFailMinMMQSdiff", (stats.get("numFailMinMMQSdiff") + 1));
                	    							}

                	    							// Compare average mapping qualities //

                	    							mapQualDiff = refMapQual - varMapQual;

//                    	    							System.err.println(refReads + "/" + readDepth + "\t" + refCounts + "\tMapqual diff: " + refMapQual + " - " + varMapQual + " = " + mapQualDiff);

                	    							if(mapQualDiff > maxMapQualDiff)
                	    							{
                	    								if(failReason.length() > 0)
                	    									failReason += ",";
                	    								failReason += "MapQualDiff";
                	    								stats.put("numFailMapQualDiff", (stats.get("numFailMapQualDiff") + 1));
                	    							}

                	    							avgReadLenDiff = (refRL - varRL) / refRL;

                	    							if(avgReadLenDiff > maxReadLenDiff)
                	    							{
                	    								if(failReason.length() > 0)
                	    									failReason += ",";
                	    								failReason += "ReadLenDiff";
                	    								stats.put("numFailReadLenDiff", (stats.get("numFailReadLenDiff") + 1));
                	    							}

            	    							}

        	    							}
        	    							catch (Exception e)
        	    							{
        	    								System.err.println("Exception thrown while processing readcounts at " + positionKey + ": " + e.getLocalizedMessage());
        	    								e.printStackTrace(System.err);
        	    								return;
        	    							}

        	    							// Check to see if it failed any filters. If so, mark it for failure //

        	    							if(failReason.length() > 0)
        	    								filterFlag = true;


        	    						}
        	    						else
        	    						{
        	    							// Unable to get readcounts for ref or var allele //
        	    							failReason = "NoReadCounts";
        	    							stats.put("numFailNoRC", (stats.get("numFailNoRC") + 1));
        	    							filterFlag = true;
        	    						}


        	    					}	// End of else for non-VCF output

			    				}
			    				catch(Exception e)
			    				{
			    					System.err.println("Exception thrown while filtering at " + positionKey + ": " + e.getLocalizedMessage());
			    					e.printStackTrace(System.err);
			    					return;
			    				}


//    			    			}

    			    			// Prepare filter output line //

    			    			String filterColumns = "";
    			    			filterColumns = refReads + "\t" + varReads + "\t" + threeDigits.format(refStrandedness) + "\t" + threeDigits.format(varStrandedness);
    			    			filterColumns += "\t" + refBaseQual + "\t" + varBaseQual + "\t" + refPos + "\t" + varPos;
    			    			filterColumns += "\t" + refDist3 + "\t" + varDist3 + "\t" + refMapQual + "\t" + varMapQual;
    			    			filterColumns += "\t" + twoDigits.format(mapQualDiff) + "\t" + refMMQS + "\t" + varMMQS + "\t" + twoDigits.format(mmqsDiff);
    			    			filterColumns += "\t" + refRL + "\t" + varRL + "\t" + twoDigits.format(avgReadLenDiff);

    			    			if(filterFlag)
    							{
    			    				// VARIANT FAILED THE FILTER //
    								filterColumns += "\t" + failReason;
    								stats.put("numFailFilter", (stats.get("numFailFilter") + 1));
    							}
    			    			else
    			    			{
    			    				// VARIANT PASSED THE FILTER
    			    				filterColumns += "\t" + "PASS";
    			    				stats.put("numPassFilter", (stats.get("numPassFilter") + 1));
    			    			}

    			    			// Prepare new VCF line //

    			    			if(isVCF)
    			    			{
    			    				String newVCFline = "";

    			    				for(int colCounter = 0; colCounter < lineContents.length; colCounter++)
    			    				{
    			    					if(newVCFline.length() > 0)
    			    						newVCFline += "\t";

    			    					if(filterFlag && colCounter == 6)
   			    							newVCFline += failReason;
    			    					else
    			    						newVCFline += lineContents[colCounter];
    			    				}
    			    				// Print the output line if it passed, or if the user specified to keep failures //

    			    				if(params.containsKey("output-file") && (!filterFlag || params.containsKey("keep-failures")))
    		    						outFile.println(newVCFline);

    			    				if(filterFlag && params.containsKey("filtered-file"))
    		    						filteredFile.println(newVCFline);
    			    			}
    			    			else
    			    			{
    			    				// Print the output line if it passed, or if the user specified to keep failures //
    			    				if(params.containsKey("output-file") && (!filterFlag || params.containsKey("keep-failures")))
    		    						outFile.println(line + "\t" + filterColumns);

    			    				// If it failed and the user specified a failure file, print it //
    			    				if(filterFlag && params.containsKey("filtered-file"))
    		    						filteredFile.println(line + "\t" + filterColumns);

    			    			}

    		    			}	// End of else for non-header line //
    	    			}
    	    			catch(Exception e)
    	    			{
    		    				System.err.println("Parsing Exception on line:\n" + line + "\n" + e.getLocalizedMessage());
    		    				e.printStackTrace(System.err);
    		    				return;
    	    			}

    	    		}

    	    		// Report summary of results //

    	    		System.err.println(stats.get("numVariants") + " variants in input file");
    	    		System.err.println(stats.get("numWithRC") + " had a bam-readcount result");
    	    		System.err.println(stats.get("numWithReads1") + " had reads1>=2");
    	    		System.err.println(stats.get("numPassFilter") + " passed filters");
    	    		System.err.println(stats.get("numFailFilter") + " failed filters");
    	    		System.err.println("\t" + stats.get("numFailNoRC") + " failed because no readcounts were returned");
    	    		System.err.println("\t" + stats.get("numFailVarCount") + " failed minimim variant count < " + minVarCount);
    	    		System.err.println("\t" + stats.get("numFailVarFreq") + " failed minimum variant freq < " + minVarFreq);
    	    		System.err.println("\t" + stats.get("numFailSomaticP") + " failed somatic P-value > " + maxSomaticP + " when depth >= " + maxSomaticPdepth);
    	    		System.err.println("\t" + stats.get("numFailStrand") + " failed minimum strandedness < " + minStrandedness);
    	    		System.err.println("\t" + stats.get("numFailRefReadPos") + " failed minimum reference readpos < " + minRefReadPos);
    	    		System.err.println("\t" + stats.get("numFailVarReadPos") + " failed minimum variant readpos < " + minVarReadPos);
    	    		System.err.println("\t" + stats.get("numFailRefDist3") + " failed minimum reference dist3 < " + minRefDist3);
    	    		System.err.println("\t" + stats.get("numFailVarDist3") + " failed minimum variant dist3 < " + minVarDist3);
    	    		System.err.println("\t" + stats.get("numFailRefMMQS") + " failed maximum reference MMQS > " + maxRefMMQS);
    	    		System.err.println("\t" + stats.get("numFailVarMMQS") + " failed maximum variant MMQS > " + maxVarMMQS);
    	    		System.err.println("\t" + stats.get("numFailMMQSdiff") + " failed maximum MMQS diff (var - ref) > " + maxMMQSdiff);
    	    		System.err.println("\t" + stats.get("numFailMapQualDiff") + " failed maximum mapqual diff (ref - var) > " + maxMapQualDiff);
    	    		System.err.println("\t" + stats.get("numFailRefMapQual") + " failed minimim ref mapqual < " + minRefMapQual);
    	    		System.err.println("\t" + stats.get("numFailVarMapQual") + " failed minimim var mapqual < " + minVarMapQual);
    	    		System.err.println("\t" + stats.get("numFailRefBaseQual") + " failed minimim ref basequal < " + minRefBaseQual);
    	    		System.err.println("\t" + stats.get("numFailVarBaseQual") + " failed minimim var basequal < " + minVarBaseQual);
    	    		System.err.println("\t" + stats.get("numFailReadLenDiff") + " failed maximum RL diff (ref - var) > " + maxReadLenDiff);

    	    		in.close();

    			}
    			else
    			{
    				System.err.println("Unable to open SNVs file for reading");
    			}

    			in.close();
    		}
    		else
    		{
    			// Infile doesn't exist //
    		}
	    }
	    catch(Exception e)
	    {
	    	System.err.println("Error Parsing SNV File: " + e.getMessage() + "\n" + e.getLocalizedMessage());
	    	e.printStackTrace(System.err);
	    }


	}


	/**
	 * Loads snvs to be filtered
	 *
	 * @param	filename	Path to file of snvs
	 * @return	snvs		HashMap of SNV positions (chrom\tposition\alt)
	 */
	static HashMap<String, String> loadReadcounts(String filename)
	{
    	HashMap<String, String> readcounts = new HashMap<String, String>();

	    try
	    {
	    	// Declare file-parsing variables //

	    	String line;

    		File infile = new File(filename);
    		if(infile.exists())
    		{
    			BufferedReader in = new BufferedReader(new SmartFileReader(infile));

    			if(in.ready())
    			{
    				while ((line = in.readLine()) != null)
    				{
    					String[] lineContents = line.split("\t");
						try {
							String chrom = lineContents[0];
	    					String position = lineContents[1];
//	    					String snvKey = chrom + "\t" + position;

	    					// Start resultLine as depth //


	    					for(int colCounter = 4; colCounter < lineContents.length; colCounter++)
	    					{
	    						String[] alleleContents = lineContents[colCounter].split(":");
	    						String thisAllele = alleleContents[0];
	    						int thisReads = Integer.parseInt(alleleContents[1]);

	    						if(thisAllele.equals("N") || thisAllele.equals("="))
	    						{
	    							// Skip non-informative alleles //
	    						}
	    						else if(thisReads == 0)
	    						{
	    							// Skip empty results //
	    						}
	    						else
	    						{
		    						String rcLine = lineContents[3];
		    						rcLine = rcLine + "\t" + lineContents[colCounter];
		    						// Save the readcount result line //
		    						String snvKey = chrom + "\t" + position + "\t" + thisAllele;
			    					readcounts.put(snvKey, rcLine);
	    						}


	    					}


						}
						catch (Exception e)
						{
							// If we encounter an issue, print a warning //
							System.err.println("Warning: Exception thrown while loading bam-readcount: " + e.getMessage());
							System.err.println("Attempting to continue, but please double-check file format and completeness");
						}


    				}
    			}
    			else
    			{
    				System.err.println("Unable to open SNVs file for reading");
    			}

    			in.close();
    		}
	    }
	    catch(Exception e)
	    {
	    	System.err.println("Error Parsing SNV File: " + e.getLocalizedMessage());
	    }

	    return(readcounts);
	}


}