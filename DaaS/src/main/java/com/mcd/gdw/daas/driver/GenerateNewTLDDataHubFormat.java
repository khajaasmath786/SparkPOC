package com.mcd.gdw.daas.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hcatalog.common.HCatUtil;
import org.apache.hcatalog.data.HCatRecord;
import org.apache.hcatalog.data.schema.HCatSchema;
import org.apache.hcatalog.mapreduce.HCatOutputFormat;
import org.apache.hcatalog.mapreduce.OutputJobInfo;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.abac.ABaC;
import com.mcd.gdw.daas.mapreduce.TLDDataHubReducerHCat;
import com.mcd.gdw.daas.mapreduce.TLDDataHubReducerMultiOut;
import com.mcd.gdw.daas.mapreduce.TLDNewDataHubMapperHCat;
import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;



public class GenerateNewTLDDataHubFormat extends Configured implements Tool {

	public class ResultListFilter implements PathFilter {
	    public boolean accept(Path path) {
	    	return(!path.getName().startsWith("_"));
	    }
	}
	

	private static final String JOB_GRP_DESC = "Generate TLD New DataHub Extracts";
	
	private FileSystem fileSystem = null;
	private Configuration hdfsConfig = null;
	private Path baseOutputPath = null;
//	private Path baseHivePath = null;
//	FsPermission newFilePremission;
	String createJobDetails = "TRUE";
	String multioutBaseOutputPath = "";
	String storeFilterFilePath = "" ;
	Path workPath = null;
	
	public static void main(String[] args) throws Exception {
		
		Configuration hdfsConfig = new Configuration();
				
		int retval = ToolRunner.run(hdfsConfig,new GenerateNewTLDDataHubFormat(), args);

		System.out.println(" return value : " + retval);

	}
	
	String outputtotable = "false";
	String outputpath = "";
	public int run(String[] args) throws Exception {

		String configXmlFile = "";
		String fileType = "";
		String terrDate = "";
		boolean helpRequest = false;
		String useStoreFilter = "FALSE";
		
		
		

		for ( int idx=0; idx < args.length; idx++ ) {
			if ( args[idx].equals("-c") && (idx+1) < args.length ) {
				configXmlFile = args[idx+1];
			}
			else
			if ( args[idx].equals("-t") && (idx+1) < args.length ) {
				fileType = args[idx+1];
			}
			else
			if ( args[idx].equals("-d") && (idx+1) < args.length ) {
				terrDate = args[idx+1];
			}else
				if ( args[idx].equals("-o") && (idx+1) < args.length ) {
					outputpath = args[idx+1];
				}
			else
			if ( args[idx].equals("-hcat") && (idx+1) < args.length ) {
				outputtotable = args[idx+1];
			}
			else
			if ( args[idx].toUpperCase().equals("-H") || args[idx].toUpperCase().equals("-HELP")  ) {
				helpRequest = true;
			}
			else if ( args[idx].equals("-createJobDetails") ) {
				createJobDetails = args[idx+1];
				if(StringUtils.isBlank(createJobDetails)){
					createJobDetails = "TRUE";
				}
			}else if ( args[idx].equalsIgnoreCase("-usestorefilter") ) {
				useStoreFilter = args[idx+1].toUpperCase();
			}if ( args[idx].equalsIgnoreCase("-storeFilterFilePath") ) {
				storeFilterFilePath = args[idx+1];
			}
			else if(args[idx].equalsIgnoreCase("-multioutBaseOuputPath")){
				multioutBaseOutputPath = args[idx+1];
			}
		}
		
		
		
		System.out.println(" terrCd  " + terrDate);
		System.out.println(" terrDate " + terrDate + " workPath " + workPath);

		if ( helpRequest ) {
			System.out.println("Usage: GenerateNewTLDDataHubFormat -c config.xml -t filetype -d territoryDateParms ");
			System.out.println("where territoryDateParm is a comma separated list of territory codes and dates separated by colons(:)");
			System.out.println("for example, 840:2012-07-01:2012-07-07 is territory 840 from July 1st, 2012 until July 7th, 2012.");
			System.out.println("the date format is either ISO YYYY-MM-DD or YYYYMMDD (both are valid)");
			System.out.println("If only one date is supplied then a single day is used for that territory");
			System.out.println("Multiple territoryDateParm can be specified as comma separated values: 840:20120701,840:2012-07-05:2012-07-08,250:2012-08-01");
			System.out.println("This will get a total of 3 days for 840 and 1 day from 250");
			System.exit(0);
		}

		if ( configXmlFile.length() == 0 || fileType.length() == 0 || terrDate.length() == 0 ) {
			System.err.println("Missing config.xml (-c), filetype (t), territoryDateParms (-d)");
			System.err.println("Usage: GenerateNewTLDDataHubFormat -c config.xml -t filetype -d territoryDateParms");
			System.err.println("where territoryDateParm is a comma separated list of territory codes and dates separated by colons(:)");
			System.err.println("for example, 840:2012-07-01:2012-07-07 is territory 840 from July 1st, 2012 until July 7th, 2012.");
			System.err.println("the date format is either ISO YYYY-MM-DD or YYYYMMDD (both are valid)");
			System.err.println("If only one date is supplied then a single day is used for that territory");
			System.err.println("Multiple territoryDateParm can be specified as comma separated values: 840:20120701,840:2012-07-05:2012-07-08,250:2012-08-01");
			System.err.println("This will get a total of 3 days for 840 and 1 day from 250");
			System.exit(8);
		}

		DaaSConfig daasConfig = new DaaSConfig(configXmlFile, fileType);
		
		if ( daasConfig.configValid() ) {
			
			if ( daasConfig.displayMsgs()  ) {
				System.out.println(daasConfig.toString());
			}
			outputpath = daasConfig.hdfsRoot() + Path.SEPARATOR+ daasConfig.hdfsWorkSubDir() + Path.SEPARATOR+ "NewTLDDataHubProd";
			multioutBaseOutputPath = daasConfig.hdfsRoot() +Path.SEPARATOR + daasConfig.hdfsHiveSubDir()+Path.SEPARATOR +"datahub_delta/tld";
			
			
			hdfsConfig = getConf();
			//AWS START
			//fileSystem = FileSystem.get(hdfsConfig);
			fileSystem = HDFSUtil.getFileSystem(daasConfig, hdfsConfig);
			//AWS END
			
			hdfsConfig.set("USE_STORE_FILTER",useStoreFilter);

			runJob(daasConfig,fileType,terrDate,true);
			
		} else {
			System.err.println("Invalid Config XML file, stopping");
			System.err.println(daasConfig.errText());
			System.exit(8);
		}
	
		return(0);
	}

	BufferedWriter listpathwriter = null;
	InputStreamReader insr = null;
	BufferedReader br = null;
	
	private void runJob(DaaSConfig daasConfig
                       ,String fileType
                       ,String terrDate
                       ,boolean compressOut) throws Exception{

		ABaC abac = null;
		ArrayList<String> lastList;
		StringBuffer terrDateList = new StringBuffer();
		
		Job job;
		
		int jobGrpId = 0;
		int jobId = 0;

		ArrayList<Path> requestedPaths = null;
		
		
		try {

			baseOutputPath = new Path(outputpath);
			workPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir() + Path.SEPARATOR + daasConfig.fileSubDir());
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, baseOutputPath,daasConfig.displayMsgs());

			if ( daasConfig.displayMsgs() ) {
				System.out.println("\nOutput path = " + baseOutputPath.toString() + "\n");
			}
		
//			hdfsConfig.set("mapred.child.java.opts", daasConfig.fileMapReduceJavaHeapSizeParm()); 

			if ( compressOut ) {
				hdfsConfig.set("mapreduce.map.output.compress", "true");
				hdfsConfig.set("mapreduce.output.fileoutputformat.compress", "true");
				hdfsConfig.set("mapreduce.output.fileoutputformat.compress.type", "BLOCK");
				hdfsConfig.set("mapreduce.map.output.compress.codec","org.apache.hadoop.io.compress.SnappyCodec");
//				hdfsConfig.set("mapreduce.output.fileoutputformat.compress.codec","org.apache.hadoop.io.compress.SnappyCodec");
				hdfsConfig.set("mapreduce.output.fileoutputformat.compress.codec","org.apache.hadoop.io.compress.GzipCodec");
			}
			
			abac = new ABaC(daasConfig);

			if("TRUE".equalsIgnoreCase(createJobDetails)){
				jobGrpId = abac.createJobGroup(JOB_GRP_DESC);
				jobId = abac.createJob(jobGrpId, 1, "DataHubExtract");
				
			}
			
			hdfsConfig.set("PROCESSING_DATA", "GOLD_LAYER");
			
		
			if (terrDate.toUpperCase().startsWith("WORK")) {
				ArrayList<String> workTerrCodeList = new ArrayList<String>();
				ArrayList<String> subTypeList = new ArrayList<String>();
				subTypeList.add("STLD");
				subTypeList.add("DetailedSOS");
				
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
						fileType, subTypeList,workTerrCodeList);
				System.out
						.println("Total number of Input Paths from Work Layer : "
								+ requestedPaths.size());
			}			
			/*if("WORK".equalsIgnoreCase(terrDate)){
				
				
				hdfsConfig.set("PROCESSING_DATA", "WORK_LAYER");
				FileStatus[] fstatusAll = null;
				FileStatus[] stldfstat = fileSystem.globStatus(new Path (workPath.toString()+Path.SEPARATOR+"step1/STLD*"));
				FileStatus[] sosfstat = fileSystem.globStatus(new Path (workPath.toString()+Path.SEPARATOR+"step1/DetailedSOS*"));
				
				fstatusAll = (FileStatus[])ArrayUtils.addAll(fstatusAll, stldfstat);
				fstatusAll = (FileStatus[])ArrayUtils.addAll(fstatusAll, sosfstat);
				if(fstatusAll != null && fstatusAll.length > 0){
					requestedPaths = new ArrayList<Path>();
					for(FileStatus fstat : fstatusAll ){
						requestedPaths.add(fstat.getPath());
					}
				}
				
				System.out.println(" requestedPaths " + requestedPaths.size());
				
			}*/else if ( terrDate.toUpperCase().startsWith("LAST") ) {

				boolean force = terrDate.equalsIgnoreCase("LAST_FORCE");
				
				if ( daasConfig.displayMsgs() ) {

					System.out.println("Getting list of Territory Codes / Business Days that have changed") ;
					System.out.print("The following territory codes / business days have changed since the last export");
					
					if ( force ) {
						System.out.println(" (FORCE LAST MergeToFinal run results):");
					} else {
						System.out.println(":");
					}
				}
				
				lastList = abac.getChangedTerrBusinessDatesSinceTs(JOB_GRP_DESC,force);
				
				int dayCount = 0;
				
				for (String itm : lastList) {
					if (itm.startsWith("840:") ) {
						if ( daasConfig.displayMsgs() ) {
							System.out.println(itm);
						}
						if ( terrDateList.length() > 0 ) {
							terrDateList.append(",");
						}
						terrDateList.append(itm);
						dayCount++;
					}
				}

				if ( dayCount == 0 ) {
					if("TRUE".equalsIgnoreCase(createJobDetails)){
						
						abac.closeJob(jobId, (short)1, "SUCCESSFUL");
						abac.closeJobGroup(jobGrpId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
//						abac.dispose();
					}

					if ( daasConfig.displayMsgs() ) {
						System.out.println("No data changed.  Closing job.");
					}
					System.exit(0);
				}

				requestedPaths = getVaildFilePaths(daasConfig,fileType,terrDateList.toString());
			} else {
				requestedPaths = getVaildFilePaths(daasConfig,fileType,terrDate);
			}
			hdfsConfig.set("MULTIOUT_BASE_OUTPUT_PATH", multioutBaseOutputPath);
			
			job = Job.getInstance(hdfsConfig, "Creating New TLD Datahub Format");
			
			HashSet<String> distinctSubFileTypeTerrCdBusnDts = new HashSet<String>();
			String[] temp;
//			String dt;
			
//			FileInputFormat.addInputPaths(job, "/daas/work/np_xml/step1/STLDRxD126840RxD12620150120-r-00055.gz");
			String multioutpath;
//			String dtwithdashes = "";
			String subfileType = "";
			String terr_cd = "";
			String businessdatewithoutdelimiter  = "";
			String businessdatewithdashes = "";
			String businessdatewithhexdelimiter = "";
			
			for (Path addPath : requestedPaths ) {
				
				if(terrDate.toUpperCase().startsWith("WORK")){
				// if("WORK".equalsIgnoreCase(terrDate)){
					temp = addPath.getName().split("-")[0].split("RxD126");
					
					subfileType = temp[0];
					terr_cd = temp[1];
					businessdatewithoutdelimiter = temp[2];
					
					businessdatewithdashes = businessdatewithoutdelimiter.substring(0,4)+"-"+businessdatewithoutdelimiter.substring(4,6)+"-"+businessdatewithoutdelimiter.substring(6, 8);
					HDFSUtil.removeHdfsSubDirIfExists(fileSystem, new Path(multioutBaseOutputPath),daasConfig.displayMsgs());
				}else{
					temp = addPath.toString().split("/");
					terr_cd = temp[temp.length-2];
					subfileType = temp[temp.length - 3];
					businessdatewithoutdelimiter = temp[temp.length - 1];
					
//					dt  = temp[temp.length-1].substring(0,4)+"RxD045"+temp[temp.length-1].substring(4,6)+"RxD045"+temp[temp.length-1].substring(6, 8);
//					businessdatewithhexdelimiter = businessdatewithoutdelimiter.substring(0,4)+"RxD045"+businessdatewithoutdelimiter.substring(4,6)+"RxD045"+businessdatewithoutdelimiter.substring(6, 8);;
					businessdatewithdashes = businessdatewithoutdelimiter.substring(0,4)+"-"+businessdatewithoutdelimiter.substring(4,6)+"-"+businessdatewithoutdelimiter.substring(6, 8);
					
				}
				
				
				
				
				//if("840".equalsIgnoreCase(terr_cd)){
					FileInputFormat.addInputPath(job, addPath);
					System.out.println( "adding input path " + addPath);
					
					businessdatewithhexdelimiter = HDFSUtil.replaceMultiOutSpecialChars(businessdatewithdashes);
//					multioutpath= multioutBaseOutputPath+Path.SEPARATOR+subfileType+"/terr_cd="+terr_cd+"/pos_busn_dt="+businessdatewithdashes;
					multioutpath= multioutBaseOutputPath+"/terr_cd="+terr_cd+"/pos_busn_dt="+businessdatewithdashes;
					
//					System.out.println(" multioutpath " + multioutpath);
					
					Path paritionpath = new Path(multioutpath);
					
					if(fileSystem.exists(paritionpath)){
						if(fileSystem.delete(paritionpath,true)){
							System.out.println(" delete  " + multioutpath + " success ");
							fileSystem.mkdirs(paritionpath);
	//						System.out.println(" recreate path  " + multioutpath + " success ");
						}else{
							System.out.println(" delete  " + multioutpath + " failed ");
						}
					}
				
//					distinctSubFileTypeTerrCdBusnDts.add(subfileType+"RxD126"+terr_cd+"RxD126"+businessdatewithhexdelimiter);
					distinctSubFileTypeTerrCdBusnDts.add(terr_cd+"RxD126"+businessdatewithhexdelimiter);
					
				//}
			}
			
			Iterator<String> distinctTerrCdBusnDtsIt = distinctSubFileTypeTerrCdBusnDts.iterator();
			String multioutputpath = "";
			while(distinctTerrCdBusnDtsIt.hasNext()){
				multioutputpath = distinctTerrCdBusnDtsIt.next();
				
				System.out.println(" multioutpath " + multioutputpath);
				
				MultipleOutputs.addNamedOutput(job, multioutputpath, TextOutputFormat.class, NullWritable.class, Text.class);
//				MultipleOutputs.addNamedOutput(job, multioutputpath, SequenceFileOutputFormat.class, NullWritable.class, Text.class);
				
//				MultipleOutputs.addNamedOutput(job, multioutputpath, OrcNewOutputFormat.class, NullWritable.class, Text.class);
			}
			MultipleOutputs.addNamedOutput(job, "terrcdbusndtstoreid", TextOutputFormat.class, NullWritable.class, Text.class);
			
			
//			FileInputFormat.addInputPath(job, new Path("/daas/gold/np_xml/STLD/840/20150216/STLD~840~20150216~0000506.gz"));
//			FileInputFormat.addInputPath(job, new Path("/daas/gold/np_xml/STLD/840/20141225/STLD~840~20141225~0000011.gz"));
//			FileInputFormat.addInputPath(job, new Path("/daas/gold/np_xml/STLD/840/20150105/STLD~840~20150105~0000679.gz"));
			


//			FileInputFormat.addInputPath(job, new Path("/daas/gold/np_xml/STLD/840/20150408/STLD~840~20150408~0000608.gz"));
//			FileInputFormat.addInputPath(job, new Path("/daas/gold/np_xml/DetailedSOS/840/20150408/DetailedSOS~840~20150408~0000608.gz"));
			
			Path daypartDistCache = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + "distcachefiles" + Path.SEPARATOR + "DayPart_ID.psv");
			Path menuPriceBasisDistCache = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + "distcachefiles" + Path.SEPARATOR + "MenuPriceBasis.psv");
			
			
//			Path locationList = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + "cache" + Path.SEPARATOR + "offers_include_list.txt");
			
			Path locationList = null;
			if(StringUtils.isNotBlank(storeFilterFilePath)){
				locationList = new Path(storeFilterFilePath);
				job.addCacheFile(new URI(locationList.toString() + "#" + locationList.getName()));
			}
			job.addCacheFile(new URI(daypartDistCache.toString() + "#" + daypartDistCache.getName()));
			job.addCacheFile(new URI(menuPriceBasisDistCache.toString() + "#" + menuPriceBasisDistCache.getName()));
			
			
//			Path locationList = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + "cache" + Path.SEPARATOR + "offers_include_list.txt");
			
//			if ( fileSystem.exists(locationList) ) {
//				fileSystem.delete(locationList, false);
//			}
			
//			createListDistCache(locationList,daasConfig);
//			fileSystem.setPermission(locationList,newFilePremission);
			
//			job.addCacheFile(new URI(locationList.toString() + "#" + locationList.getName()));

		
			job.setJarByClass(GenerateNewTLDDataHubFormat.class);
			job.setMapperClass(TLDNewDataHubMapperHCat.class);
			
			job.setMapOutputKeyClass(Text.class);
//			job.setMapOutputKeyClass(NullWritable.class);
			job.setMapOutputValueClass(Text.class);
			job.setOutputKeyClass(NullWritable.class);
//			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			
			

			if(outputtotable.equalsIgnoreCase("true")){
				job.setOutputFormatClass(HCatOutputFormat.class);
				job.setOutputKeyClass(HCatRecord.class);
				
				Map<String, String> partitionValues = new HashMap<String,String>();
				partitionValues.put("terr_cd", "840");
				partitionValues.put("pos_busn_dt", "2015-01-09");
				
				HCatOutputFormat.setOutput(job, OutputJobInfo.create("default", "TLD_DataHub_delete", partitionValues));
				
				HiveConf hconf = new HiveConf(getConf(),this.getClass());
				
				hconf.addResource(new Path("/etc/hive/conf/hive-site.xml"));
				hconf.set("hive.exec.dynamic.partition", "true");
				hconf.set("hive.exec.dynamic.partition.mode","nonstrict");
				
				HiveMetaStoreClient hmscli = new HiveMetaStoreClient(hconf);
				
				Table table = HCatUtil.getTable(hmscli, "default", "TLD_DataHub_delete");
				
				HCatSchema s = HCatUtil.getTableSchemaWithPtnCols(table);
				
				HCatOutputFormat.setSchema(job, s);
				
				job.setReducerClass(TLDDataHubReducerHCat.class);
			}else{
//				job.setOutputFormatClass(SequenceFileOutputFormat.class);
				job.setReducerClass(TLDDataHubReducerMultiOut.class);
				LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
				TextOutputFormat.setOutputPath(job, baseOutputPath);
//				SequenceFileOutputFormat.setOutputPath(job, baseOutputPath);
//				OrcNewOutputFormat.setOutputPath(job, baseOutputPath);
				
			}
			
//			LazyOutputFormat.setOutputFormatClass(job, SequenceFileOutputFormat.class);
//			LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
//			TextOutputFormat.setCompressOutput(job, false);
			
			
//			if(1 == 1 )
//				return;
			if ( ! job.waitForCompletion(true) ) {
				
				if("TRUE".equalsIgnoreCase(createJobDetails)){
//					abac.closeJobGroup(jobGrpId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
					abac.closeJob(jobId, DaaSConstants.JOB_FAILURE_ID, DaaSConstants.JOB_FAILURE_CD);
					
				}
				
				System.err.println("Error occured in MapReduce process, stopping");
				throw new Exception("Error occured in MapReduce process, stopping");
			}

			
			
			String newfileName = "terrcd_busndt_storeid_list.txt.gz";
			FileStatus[] fstatarr = fileSystem.globStatus(new Path(baseOutputPath+Path.SEPARATOR+"terrcd_busndt_storeid_list.txt*"));
			System.out.println(" found file " + "terrcd_busndt_storeid_list");
			Path destfile = new Path(daasConfig.hdfsRoot()+Path.SEPARATOR+daasConfig.hdfsWorkSubDir()+Path.SEPARATOR+newfileName);
			
			if(fstatarr != null && fstatarr.length > 0){
				
				for(FileStatus fstat: fstatarr){
					System.out.println(" found file " + fstat.getPath());
					if(fileSystem.exists(destfile))
						fileSystem.delete(destfile, false);
					newfileName = "terrcd_busndt_storeid_list.txt.gz";
					if(fstat.getPath().getName().contains("terrcd_busndt_storeid_list.txt")){
						if(fileSystem.rename(fstat.getPath(), new Path(daasConfig.hdfsRoot()+Path.SEPARATOR+daasConfig.hdfsWorkSubDir()+Path.SEPARATOR+newfileName))){
							System.out.println(" rename successful " + fstat.getPath().getName()+ " to  "+ daasConfig.hdfsRoot()+Path.SEPARATOR+daasConfig.hdfsWorkSubDir()+Path.SEPARATOR+newfileName);
						}else{
							System.out.println(" rename failed " + fstat.getPath().getName()+ " to  "+ daasConfig.hdfsRoot()+Path.SEPARATOR+daasConfig.hdfsWorkSubDir()+Path.SEPARATOR+newfileName);
						}
						break;
					}
				}
			}
			 
//			if("WORK".equalsIgnoreCase(terrDate)){
//			 
//			 HashSet<String> uniqueTerrCdBusndtStoreIds = new HashSet<String>();
//			 
//			//create terrCd_busndt_storeid file
////			FileStatus[] fstatarr = fileSystem.globStatus(new Path(baseOutputPath.toString()+"/part*"));
//			if(fstatarr != null && fstatarr.length > 0){
//				for(FileStatus fstat: fstatarr){
//					
//					insr = new InputStreamReader(new GZIPInputStream(fileSystem.open(fstat.getPath())));
//					
//					br = new BufferedReader( insr);
//					
//					
//					if(br != null){
//						String line = null;
//						
//						while( (line = br.readLine()) != null){
//							
//							uniqueTerrCdBusndtStoreIds.add(line);
////							System.out.println( " line " + line);
//						}
//						
//						br.close();
//						insr.close();
//						
//					}
//				}
//				
//				if(uniqueTerrCdBusndtStoreIds != null && !uniqueTerrCdBusndtStoreIds.isEmpty()){
//					Iterator<String> it = uniqueTerrCdBusndtStoreIds.iterator();
//					Path listpath = new Path(daasConfig.hdfsRoot()+Path.SEPARATOR+daasConfig.hdfsWorkSubDir()+Path.SEPARATOR+"terrcd_busndt_storeid_list.txt");
//					
//					
//					
//					listpathwriter =new BufferedWriter(new OutputStreamWriter(fileSystem.create(listpath,true)));
//					while(it.hasNext()){
//						listpathwriter.write(it.next());
//						listpathwriter.write("\n");
//					}
//					listpathwriter.close();
//					
//				}
//			}
//			}
			
			
			if("TRUE".equalsIgnoreCase(createJobDetails)){
//				abac.closeJobGroup(jobGrpId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
				abac.closeJob(jobId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
		
			}
			
		} catch (Exception ex) {
			System.err.println("Error occured in GenerateNewTLDDataHubFormat.runJob:");
			System.err.println(ex.toString());
			ex.printStackTrace();
			throw ex;
		}finally{
			try{
				
				if(abac != null)
					abac.dispose();
				
				if(listpathwriter != null){
					listpathwriter.close();
					listpathwriter = null;
				}
				if(br != null){
					br.close();
					br = null;
				}
				if(insr != null){
					insr.close();
					insr = null;
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}

	}

	private ArrayList<Path> getVaildFilePaths(DaaSConfig daasConfig
                                             ,String fileType
                                             ,String requestedTerrDateParms) {

		ArrayList<Path> retPaths = new ArrayList<Path>();
		
		System.out.println(" getVaildFilePaths " + requestedTerrDateParms);

		try {

			Path[] requestPaths = HDFSUtil.requestedArgsPaths(fileSystem, daasConfig, requestedTerrDateParms, "STLD", "DetailedSOS");
//			Path[] requestPaths = HDFSUtil.requestedArgsPaths(fileSystem, daasConfig, requestedTerrDateParms, "DetailedSOS");

			if ( requestPaths == null ) {
				System.err.println("Stopping, No valid territory/date params provided");
			
				System.exit(8);
			}

			int validCount = 0;

			for ( int idx=0; idx < requestPaths.length; idx++ ) {
				if ( fileSystem.exists(requestPaths[idx]) ) {
					retPaths.add(requestPaths[idx]);
					validCount++;

					if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
						System.out.println("Found valid path = " + requestPaths[idx].toString());
					}
			} else {
					System.err.println("Invalid path \"" + requestPaths[idx].toString() + "\" skipping.");
				}
			}
				
			if ( validCount == 0 ) {
				System.err.println("Stopping, No valid files found");
				System.exit(8);
			}

			if ( daasConfig.displayMsgs() ) {
				System.out.print("\nFound " + validCount + " HDFS path");
				if ( validCount > 1 ) {
					System.out.print("s");
				}
				System.out.print(" from " + requestPaths.length + " path");
				if ( requestPaths.length > 1 ) {
					System.out.println("s.");
				} else {
					System.out.println(".");
				}
			}

			if ( daasConfig.displayMsgs() ) {
				System.out.println("\n");
			}

		} catch (Exception ex) {
			System.err.println("Error occured in GenerateNewTLDDataHubFormat.getVaildFilePaths:");
			System.err.println(ex.toString());
			ex.printStackTrace();
			System.exit(8);
		}

		return(retPaths);
			
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
			ArrayList<String> subTypeCodes,ArrayList<String> workTerrCodeList) {

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
	
}
	
