package com.mcd.gdw.daas.driver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.abac.ABaC;
import com.mcd.gdw.daas.mapreduce.TMSHaviXmlMapper;
import com.mcd.gdw.daas.mapreduce.TMSHaviXmlReducer;
import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;

/**
 * @author mc41946 This MapReduce job extracts the Offer Redemption Data for US
 *         Mobile Stores. It uses TMSHaviXmlMapper and TMSHaviXmlReducer to
 *         implement the Map and Reduce steps, respectively. When you run this
 *         class you must supply it with three parameters: -c config.xml,-t
 *         POS_XML and -d WORK:840 The output will be stored in Pipe seperated
 *         file format.
 */

public class TMSHaviXmlDriver extends Configured implements Tool {

	private ArrayList<String> workTerrCodeList = new ArrayList<String>();
	// private String createJobDetails = "FALSE";
	private final static String HAVI_EXTRACT_JOBGROUP_NAME = "HAVI Offer Redemption Extract";
	private final static String HAVI_EXTRACT_HISTORY_JOBGROUP_NAME = "HAVI History Offer Redemption Extract";
	//private final static String HAVI_OUTPUT_FILE_NAME_PREFIX = "DGCID_HGS_Redemption_840_";
	private final static String HAVI_OUTPUT_FILE_NAME_PREFIX = "DGCID"+DaaSConstants.SPLCHARTILDE_DELIMITER+"HGS"+DaaSConstants.SPLCHARTILDE_DELIMITER+"Redemption";
	private String fileSeperator = "/";
	private String fileNameSeperator = "_";
	private final static String PREFIX_FILENAME = "DGCID_HGS_Redemption";
	private int jobSeqNbr = 1;
	private int jobGroupId = 0;
	int jobId = 0;
	int totalRecordsInOutputFile = -1;
	private Set<String> terrCodes=new HashSet<String> ();
	private static final  String JOB_SUCCESSFUL_CD  = "SUCCESSFUL";

	// private String terrCd="";

	public static void main(String[] args) throws Exception {

		int retval = ToolRunner.run(new Configuration(),
				new TMSHaviXmlDriver(), args); 

		//System.out.println(" return value : " + retval);
	}

	@Override
	public int run(String[] argsall) throws Exception {

		String configXmlFile = "";
		String fileType = "";
		String terrDate = "";
		// String terrDateFile = "";
		String owshFltr = "*";
		String[] args;
		String haviOrderFilter = "";

		GenericOptionsParser gop = new GenericOptionsParser(argsall);

		args = gop.getRemainingArgs();

		for (int idx = 0; idx < args.length; idx++) {
			if (args[idx].equals("-c") && (idx + 1) < args.length) {
				configXmlFile = args[idx + 1];
			}

			if (args[idx].equals("-t") && (idx + 1) < args.length) {
				fileType = args[idx + 1];
			}

			if (args[idx].equals("-d") && (idx + 1) < args.length) {
				terrDate = args[idx + 1];
			}
			if (args[idx].equals("-owshfltr") && (idx + 1) < args.length) {
				owshFltr = args[idx + 1];
			}
			if (args[idx].equals("-h") && (idx + 1) < args.length) {
				haviOrderFilter = args[idx + 1];
			}

		}

		if (configXmlFile.length() == 0 || fileType.length() == 0
				|| terrDate.length() == 0) {
			System.err.println("Invalid parameters");
			System.err
					.println("Usage: TMSHaviXMLDriver -c config.xml -t filetype -d territoryDateParms -owshfltr ownershipFilter");
			System.exit(8);
		}

		DaaSConfig daasConfig = new DaaSConfig(configXmlFile, fileType);

		if (daasConfig.configValid()) {

			runMrTMSHaviExtract(daasConfig, fileType, getConf(), terrDate,
					owshFltr, haviOrderFilter);

		} else {
			System.err.println("Invalid config.xml and/or filetype");
			System.err.println("Config File = " + configXmlFile);
			System.err.println("File Type   = " + fileType);
			System.exit(8);
		}

		return (0);
	}

	/**
	 * Method which runs Map Reduce job.
	 * 
	 * @param daasConfig
	 *            : Configuration object which loads ABaC, teradata and Hadoop
	 *            Paths
	 * @param fileType
	 *            : File type for this job. POS_XML
	 * @param hdfsConfig
	 *            : Map Reduce Configuration Object
	 * @param terrDate
	 *            : Parameter for generating Input Path i.e. either Work or Gold
	 *            Layer
	 * @param owshFltr
	 *            : Ownership filter
	 * @param haviOrderFilter
	 * @throws Exception
	 *             :
	 */
	private void runMrTMSHaviExtract(DaaSConfig daasConfig, String fileType,
			Configuration hdfsConfig, String terrDate, String owshFltr,
			String haviOrderFilter) throws Exception {

		Job job;
		ArrayList<Path> requestedPaths = null;
		SimpleDateFormat sdf;
		Calendar dt;
		String subDir = "";

		// M-1939 Hadoop- Offer Redemption Extract changes for US Mobile Stores:
		// Havi Extract is done only on STLD files
		ArrayList<String> subTypeList = new ArrayList<String>();
		subTypeList.add("STLD");

		ABaC abac = null;
		try {

			hdfsConfig.set(DaaSConstants.JOB_CONFIG_PARM_OWNERSHIP_FILTER,
					owshFltr);
			String jobTitle = "";
			// M-1939 Hadoop- Offer Redemption Extract changes for US Mobile
			// Stores: Update Job Title based on extract.
			if (!terrDate.toUpperCase().startsWith("WORK")) {
				jobTitle = HAVI_EXTRACT_HISTORY_JOBGROUP_NAME;
			} else {
				jobTitle = HAVI_EXTRACT_JOBGROUP_NAME;
			}

			System.out.println("\nCreate TMS Havi File Format\n");

			//AWS START
			//FileSystem fileSystem = FileSystem.get(hdfsConfig);
			FileSystem fileSystem = HDFSUtil.getFileSystem(daasConfig, hdfsConfig);
			//AWS END 
			sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			dt = Calendar.getInstance();
			subDir = sdf.format(dt.getTime());

			// M-1939 : Define output path based on input path i.e. Work or Gold
			// Layer.
			Path outPath = null;
			if (terrDate.toUpperCase().startsWith("WORK")) {
				outPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR
						+ daasConfig.hdfsWorkSubDir() + Path.SEPARATOR
						+ "TMSHaviExtract");
			} else {
				outPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR
						+ daasConfig.hdfsWorkSubDir() + Path.SEPARATOR
						+ "TMSHaviExtract" + Path.SEPARATOR + subDir);
			}
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, outPath,
					daasConfig.displayMsgs());

			/*
			 * M-1939 : Add Input Paths based on the Parameter. Path for Work
			 * Layer: WORK:840 Path for History Extract:
			 * 840:20120701,840:2012-07-05:2012-07-08,250:2012-08-01
			 */
			if (terrDate.toUpperCase().startsWith("WORK")) {
				String[] workParts = (terrDate + ":").split(":");
				String filterTerrCodeList = workParts[1];

				if (filterTerrCodeList.length() > 0) {
					System.out
							.println("Work Layer using only the following Territory Codes:");
					String[] parts = filterTerrCodeList.split(",");
					for (String addTerrCode : parts) {
						System.out.println("    " + addTerrCode);
						workTerrCodeList.add(addTerrCode);
					}
				}
				requestedPaths = getVaildFilePaths(daasConfig, fileSystem,
						fileType, subTypeList);
				System.out
						.println("Total number of Input Paths from Work Layer : "
								+ requestedPaths.size());
				terrCodes=new HashSet<String>(workTerrCodeList);
			} else {
				requestedPaths = getVaildFilePaths(daasConfig, fileSystem,
						fileType, terrDate, subTypeList);
				System.out
						.println("Total number of Input Paths from Gold Layer : "
								+ requestedPaths.size());
				ArrayList<String> goldTerrCodes=goldTerrCodeList(daasConfig, fileSystem,
						terrDate, subTypeList);
				terrCodes=new HashSet<String>(goldTerrCodes);
			}

			// M-1939 : Parameters required for Havi Extract.
			String filterCriteria = "FALSE";
			Configuration conf = getConf();
			String[] temp = null;
			if (haviOrderFilter.length() > 0) {
				// New Input Output TRUE~Store~Order
				temp = haviOrderFilter.split("~");
				if (temp.length > 1) {
					filterCriteria = temp[0].toUpperCase();

					conf.set("FILTER_BY_STORE", temp[1].toUpperCase());
					if (temp.length == 3) {
						conf.set("FILTER_BY_ORDER", temp[2].toUpperCase());
						System.out.println(" FILTER_BY_STORE " + filterCriteria
								+ " - " + temp[1].toUpperCase() + " - "
								+ temp[2].toUpperCase());
					} else {
						System.out.println(" FILTER_BY_STORE " + filterCriteria
								+ " - " + temp[1].toUpperCase());
					}

				}
			}
			conf.set("USE_FILTER_BY_STORE_ORDER", filterCriteria);
			// conf.set("mapred.job.queue.name", queuename);

			// M-1939 : Job Parameters.
			job = Job.getInstance(conf, jobTitle);
			abac = new ABaC(daasConfig);
			job.setJarByClass(TMSHaviXmlDriver.class);
			job.setMapperClass(TMSHaviXmlMapper.class);
			job.setReducerClass(TMSHaviXmlReducer.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(Text.class);
			job.setOutputKeyClass(NullWritable.class);
			job.setOutputValueClass(Text.class);

			for (Path addPath : requestedPaths) {
				FileInputFormat.addInputPath(job, addPath);
			}
			TextOutputFormat.setOutputPath(job, outPath);
			Date current_timeStamp = new Date();
			SimpleDateFormat extractedDateFormat = new SimpleDateFormat(
					"yyyyMMdd");			
			String extractedDate = extractedDateFormat
					.format(current_timeStamp);
	        Iterator<String> it = terrCodes.iterator();
			
			while(it.hasNext()){
				String terrCd = it.next();
				
				MultipleOutputs.addNamedOutput(job,HAVI_OUTPUT_FILE_NAME_PREFIX+DaaSConstants.SPLCHARTILDE_DELIMITER+terrCd+DaaSConstants.SPLCHARTILDE_DELIMITER+extractedDate,TextOutputFormat.class, Text.class, Text.class);
				
			}
			//this prevents the creation of part* files when using MultiOutputformat
			LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
			jobGroupId = abac.createJobGroup(jobTitle);
			jobId = abac.createJob(jobGroupId, jobSeqNbr, job.getJobName());

			if (!job.waitForCompletion(true)) {
				System.err
						.println("Error occured in MapReduce process, stopping");
				System.exit(8);
			}

			// M-1939 : Merge Path. Location where output files are merged into
			// single file.
			Path mergeOutPath = null;
		
		
			Iterator<String> iterator = terrCodes.iterator();

			
			
			while(iterator.hasNext()){
				String terrCd = iterator.next();
				
			
			String filePrefix=HAVI_OUTPUT_FILE_NAME_PREFIX+DaaSConstants.SPLCHARTILDE_DELIMITER+terrCd+DaaSConstants.SPLCHARTILDE_DELIMITER+extractedDate;
			String mergeOutDir = daasConfig.hdfsRoot() + Path.SEPARATOR
					+ daasConfig.hdfsWorkSubDir() + Path.SEPARATOR
					+ "TMSHaviExtract_Merge"+Path.SEPARATOR+terrCd;
			mergeOutPath = new Path(mergeOutDir);
			HDFSUtil.createHdfsSubDirIfNecessary(fileSystem, mergeOutPath, true);	

			// M-1939 :Merge output files into single file and rename it.
			String filename = mergeOutputandRenameFile(conf, outPath,
					mergeOutPath, mergeOutDir,filePrefix,daasConfig);

			System.out.println("Total number of lines in output file : "
					+ totalRecordsInOutputFile);
			
			//System.out.println("Job Id : " + jobId);

			// M-1939 : Populate Abac tables.
			abac.insertExecutionTargetFile(jobId, 1, filename,
					"Offer Redemption Extract", "Pipe Separated File",
					totalRecordsInOutputFile);
				
			}

		} catch (Exception ex) {
			System.err.println("Error occured in MapReduce process:");
			ex.printStackTrace(System.err);
			System.exit(8);
		} finally {
			if (abac != null) {
				System.out
						.println("Inside Finally block to close abac connections : ");
				abac.closeJob(jobId, DaaSConstants.JOB_SUCCESSFUL_ID,
						JOB_SUCCESSFUL_CD);
				abac.closeJobGroup(jobGroupId, DaaSConstants.JOB_SUCCESSFUL_ID,
						JOB_SUCCESSFUL_CD);
				abac.dispose();

			}
		}

	}

	/**
	 * This Method gets valid File Paths from the Work layer. -d WORK:840 gets
	 * all the files from WorkLayer whose terrcode is 840.
	 * 
	 * @param daasConfig
	 *            :Configuration object which loads ABaC, teradata and Hadoop
	 *            Paths
	 * @param fileSystem
	 *            Hadoop FileSystem
	 * @param fileType
	 *            FileType for the paths i.e. POS_XML
	 * @param subTypeCodes
	 *            STLD files for Offer Redemption Extract. Other codes are
	 *            ignored.
	 * @return
	 */
	private ArrayList<Path> getVaildFilePaths(DaaSConfig daasConfig,
			FileSystem fileSystem, String fileType,
			ArrayList<String> subTypeCodes) {

		ArrayList<Path> retPaths = new ArrayList<Path>();
		String filePath;
		boolean useFilePath;
		boolean removeFilePath;
		String[] fileNameParts;
		String fileTerrCode;

		Path listPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR
				+ daasConfig.hdfsWorkSubDir() + Path.SEPARATOR
				+ daasConfig.fileSubDir() + Path.SEPARATOR + "step1");

		try {
			FileStatus[] fstus = fileSystem.listStatus(listPath);

			for (int idx = 0; idx < fstus.length; idx++) {
				filePath = HDFSUtil.restoreMultiOutSpecialChars(fstus[idx]
						.getPath().getName());

				useFilePath = false;
				for (int idxCode = 0; idxCode < subTypeCodes.size(); idxCode++) {
					if (filePath.startsWith(subTypeCodes.get(idxCode))) {
						useFilePath = true;
					}
				}

				if (useFilePath && workTerrCodeList.size() > 0) {
					fileNameParts = filePath.split("~");
					fileTerrCode = fileNameParts[1];

					removeFilePath = true;

					for (String checkTerrCode : workTerrCodeList) {
						if (fileTerrCode.equals(checkTerrCode)) {
							removeFilePath = false;
						}
					}

					if (removeFilePath) {
						useFilePath = false;
					}
				}

				// if ( filePath.startsWith("STLD") ||
				// filePath.startsWith("DetailedSOS") ||
				// filePath.startsWith("MenuItem") ||
				// filePath.startsWith("SecurityData") ||
				// filePath.startsWith("store-db") ||
				// filePath.startsWith("product-db") ) {
				if (useFilePath) {
					retPaths.add(fstus[idx].getPath());

					if (daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum) {
						System.out.println("Added work source file ="
								+ filePath);
					}
				}
			}

		} catch (Exception ex) {
			System.err
					.println("Error occured in TMSHaviXmlDriver.getVaildFilePaths:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}

		if (retPaths.size() == 0) {
			System.err.println("Stopping, No valid files found");
			System.exit(8);
		}

		return (retPaths);
	}

	/**
	 * This Method gets valid File Paths from the Gold layer.
	 * 840:20120701,840:2012-07-05:2012-07-08,250:2012-08-01. This will get a
	 * total of 3 days for 840 and 1 day from 250
	 * 
	 * @param daasConfig
	 *            Configuration object which loads ABaC, teradata and Hadoop
	 *            Paths
	 * @param fileSystem
	 *            Hadoop FileSystem
	 * @param fileType
	 *            FileType for the paths i.e. POS_XML
	 * @param requestedTerrDateParms
	 * @param subTypeCodes
	 * @return
	 */
	private ArrayList<Path> getVaildFilePaths(DaaSConfig daasConfig,
			FileSystem fileSystem, String fileType,
			String requestedTerrDateParms, ArrayList<String> subTypeCodes) {

		ArrayList<Path> retPaths = new ArrayList<Path>();

		try {

			// Path[] requestPaths = HDFSUtil.requestedArgsPaths(fileSystem,
			// daasConfig, requestedTerrDateParms, "STLD", "DetailedSOS",
			// "MenuItem", "SecurityData","store-db","product-db");
			Path[] requestPaths = HDFSUtil.requestedArgsPaths(fileSystem,
					daasConfig, requestedTerrDateParms, subTypeCodes);

			if (requestPaths == null) {
				System.err
						.println("Stopping, No valid territory/date params provided");
				System.exit(8);
			}

			int validCount = 0;

			for (int idx = 0; idx < requestPaths.length; idx++) {
				if (fileSystem.exists(requestPaths[idx])) {
					retPaths.add(requestPaths[idx]);
					validCount++;

					if (daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum) {
						System.out.println("Found valid path = "
								+ requestPaths[idx].toString());
					}
				} else {
					System.err.println("Invalid path \""
							+ requestPaths[idx].toString() + "\" skipping.");
				}
			}

			if (validCount == 0) {
				System.err.println("Stopping, No valid files found");
				System.exit(8);
			}

			if (daasConfig.displayMsgs()) {
				System.out.print("\nFound " + validCount + " HDFS path");
				if (validCount > 1) {
					System.out.print("s");
				}
				System.out.print(" from " + requestPaths.length + " path");
				if (requestPaths.length > 1) {
					System.out.println("s.");
				} else {
					System.out.println(".");
				}
			}

			if (daasConfig.displayMsgs()) {
				System.out.println("\n");
			}

		} catch (Exception ex) {
			System.err
					.println("Error occured in TMSHaviXmlDriver.getVaildFilePaths:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}

		return (retPaths);
	}

	/**
	 * This Method Merges the output of reducer into single file and renames it.
	 * 
	 * @param conf
	 *            : Hadoop Configuration Object
	 * @param outPath
	 *            : Path where reducer part files are present.
	 * @param mergeOutPath
	 *            : Path where merged output file is generated
	 * @param mergeOutDir
	 *            : Directory where merged output file is generated
	 * @return
	 */
	private String mergeOutputandRenameFile(Configuration conf, Path outPath,
			Path mergeOutPath, String mergeOutDir, String filePrefix,DaaSConfig daasConfig) {
		String renameFileTo = "";
		try {

			Date current_timeStamp = new Date();
/*			SimpleDateFormat extractedDateFormat = new SimpleDateFormat(
					"yyyyMMdd");			
			String extractedDate = extractedDateFormat
					.format(current_timeStamp);*/
			SimpleDateFormat extractedTimeStampFormat = new SimpleDateFormat(
					"yyyyMMddHHmmssSSS");
			String extractedTimeStamp = extractedTimeStampFormat
					.format(current_timeStamp);
			//START AWS
			//FileSystem hdfs = FileSystem.get(conf);
			FileSystem hdfs = HDFSUtil.getFileSystem(daasConfig, conf);
			//END AWS
			
			
			String tmpOutDir = daasConfig.hdfsRoot() + Path.SEPARATOR
					+ daasConfig.hdfsWorkSubDir() + Path.SEPARATOR
					+ "TMSHaviExtract_Merge"+Path.SEPARATOR+"tmp";
			//Tmp Path will be deleted after moving files to Merged Directory.
			Path tmpPath = new Path(tmpOutDir);
			HDFSUtil.createHdfsSubDirIfNecessary(hdfs, tmpPath, true);
			
			//AWS START
			//FileSystem tmpFS=tmpPath.getFileSystem(conf);
			FileSystem tmpFS=HDFSUtil.getFileSystem(daasConfig, conf);
			//AWS END
			
			//Move files from reducer output path to temporary folder.
			FileStatus outFS[] = hdfs.listStatus(outPath);
			for (int outFileCounter = 0; outFileCounter < outFS.length; outFileCounter++) {
				if ( outFS[outFileCounter].getPath().getName()
						.startsWith(filePrefix)) {
					//AWS START
					//FileUtil.copy(outFS[outFileCounter].getPath().getFileSystem(conf), outFS[outFileCounter].getPath(), tmpFS, tmpPath, false, conf);
					FileUtil.copy(tmpFS, outFS[outFileCounter].getPath(), tmpFS, tmpPath, false, conf);
					//AWS END

				}
			}
			
			//Creating Merge if necessary and Move files from Tmp folder to Merge folder by using merge function
			HDFSUtil.createHdfsSubDirIfNecessary(hdfs, mergeOutPath, true);
			//AWS START
			//FileUtil.copyMerge(tmpFS, tmpPath, mergeOutPath.getFileSystem(conf), mergeOutPath, true, conf,
			//		null);
			FileUtil.copyMerge(tmpFS, tmpPath, tmpFS, mergeOutPath, true, conf,null);
			//AWS END
			
			//Rename File that is present in Merge folder
			FileStatus mergeFS[] = hdfs.listStatus(mergeOutPath);
			for (int mergeFileCounter = 0; mergeFileCounter < mergeFS.length; mergeFileCounter++) {
				
				if (!mergeFS[mergeFileCounter].getPath().getName()
						.startsWith(PREFIX_FILENAME)) {
					
					String mergedFileName = mergeFS[mergeFileCounter].getPath().getName();
					renameFileTo = mergedFileName.replaceAll(mergedFileName,
							filePrefix + "."
									+ extractedTimeStamp);
										renameFileTo=renameFileTo.replaceAll(DaaSConstants.SPLCHARTILDE_DELIMITER, fileNameSeperator);
					Path psvFile = new Path(mergeOutDir + fileSeperator
							+ renameFileTo + ".psv");
					hdfs.rename(mergeFS[mergeFileCounter].getPath(), psvFile);
					totalRecordsInOutputFile = totalLinesinOutPutFile(psvFile,
							hdfs);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return renameFileTo;
	}

	/**
	 * This method gets Number of Lines in merged output file.
	 * 
	 * @param path
	 *            : Path where merged output file is present.
	 * @param fs
	 *            : Hadoop file system
	 * @return countOfLinesInOutputFile : Returns count of lines in output file
	 * @throws IOException
	 *             : Throws Exception when file is not found.
	 * 
	 * */
	private int totalLinesinOutPutFile(Path path, FileSystem fs)
			throws IOException {
		BufferedReader br = null;
		String line;
		int count = 0;
		if (path.getName().contains(".psv")) {
			try {
				br = new BufferedReader(new InputStreamReader(fs.open(path)));
				while ((line = br.readLine()) != null) {
					count++;
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
		}
		return count;
	}
	
	/** This method stores the territory codes of gold layer in list.
	 * 
	 * @param daasConfig
	 * @param fileType
	 * @param requestedTerrDateParms
	 * @param subTypeCodes
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private ArrayList<String> goldTerrCodeList(DaaSConfig daasConfig,
			FileSystem fileSystem,
			String requestedTerrDateParms, ArrayList<String> subTypeCodes) throws FileNotFoundException, IOException
	{
		ArrayList<String> terrCodes= new ArrayList<String>();
		ArrayList<String> allTerrCodes = new ArrayList<String>();
		
		Path listPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsFinalSubDir() + Path.SEPARATOR + daasConfig.fileSubDir() + Path.SEPARATOR + subTypeCodes.get(0));
		
		FileStatus[] fstus = fileSystem.listStatus(listPath);
		
		for (int idx=0; idx < fstus.length; idx++ ) {
			allTerrCodes.add(fstus[idx].getPath().getName());
		}
		 String[] listParts;
		 String[] argParts;
		listParts = requestedTerrDateParms.split(",");

	    for ( int idxList=0; idxList < listParts.length; idxList++ ) {

	    	argParts = (listParts[idxList]).split(":");
	      
	    	if ( argParts[0].equals("*") ) {
	    		terrCodes = allTerrCodes;
	    	} else {
	    		terrCodes = new ArrayList<String>();
	    		terrCodes.add(argParts[0]);
	    	}
		
		
	}
	    return terrCodes;	
}



}
