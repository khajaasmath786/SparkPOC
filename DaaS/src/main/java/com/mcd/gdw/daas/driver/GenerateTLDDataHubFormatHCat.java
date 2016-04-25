package com.mcd.gdw.daas.driver;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
//import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hcatalog.common.HCatUtil;
import org.apache.hcatalog.data.HCatRecord;
import org.apache.hcatalog.data.schema.HCatSchema;
import org.apache.hcatalog.mapreduce.HCatOutputFormat;
import org.apache.hcatalog.mapreduce.OutputJobInfo;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.abac.ABaC;
import com.mcd.gdw.daas.mapreduce.OffersReportFormatMapper;
import com.mcd.gdw.daas.mapreduce.OffersReportFormatReducer;
import com.mcd.gdw.daas.mapreduce.TLDDataHubMapper;
import com.mcd.gdw.daas.mapreduce.TLDDataHubMapperHCat;
import com.mcd.gdw.daas.mapreduce.TLDDataHubReducer;
import com.mcd.gdw.daas.mapreduce.TLDDataHubReducerHCat;
import com.mcd.gdw.daas.mapreduce.TLDDataHubReducerMultiOut;

import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;
import com.mcd.gdw.daas.util.RDBMS;

public class GenerateTLDDataHubFormatHCat extends Configured implements Tool {

	public class ResultListFilter implements PathFilter {
	    public boolean accept(Path path) {
	    	return(!path.getName().startsWith("_"));
	    }
	}
	
	private class OutputFileName {
		
		private String terrCd;
		private String posBusnDt;
		private String prefix; 
		
		public OutputFileName(FileStatus fileStatus) {
			
			String[] parts = fileStatus.getPath().getName().split("_");
			String[] parts2 = null; 
			
			if ( parts.length > 1 ) {
				terrCd = parts[0];
				
				if ( parts.length == 2 ) {
					parts2 = parts[1].split("-");
					posBusnDt = "";
				} else {
					posBusnDt = parts[1];
					parts2 = parts[2].split("-");
				}
				
				prefix = parts2[0];
				
			} else {
				terrCd = "";
				posBusnDt = "";
				prefix = "";
			}
			
		}
		
		@SuppressWarnings("unused")
		public String getTerrCd() {
			return(terrCd);
		}
		
		@SuppressWarnings("unused")
		public String getPosBusnDt() {
			return(posBusnDt);
		}
		
		public String getPrefix() {
			return(prefix);
		}
		
		public String getKey(boolean includeDate) {

			String retKey = ""; 
			
			if ( includeDate ) {
				retKey = prefix + "_" + terrCd + "_" + posBusnDt; 
				
			} else {
				retKey = prefix + "_" + terrCd; 
			}
			
			return(retKey);
		}
		
		public Path getDirPath(Path basePath) {

			Path retPath = null;
			
			if ( posBusnDt.length() > 0 ) {
				retPath = new Path(basePath.toString() + Path.SEPARATOR + "terr_cd=" + terrCd + Path.SEPARATOR + "pos_busn_dt=" + posBusnDt); 
				
			} else {
				retPath = new Path(basePath.toString() + Path.SEPARATOR + "terr_cd=" + terrCd); 
			}
			
			return(retPath);
			
		}
		
		public Path getNewFilePath(Path basePath
				                  ,Path currPath) {

			Path retPath = null;
			
			if ( posBusnDt.length() > 0 ) {
				retPath = new Path(basePath.toString() + Path.SEPARATOR + "terr_cd=" + terrCd + Path.SEPARATOR + "pos_busn_dt=" + posBusnDt + Path.SEPARATOR + currPath.getName()); 
				
			} else {
				retPath = new Path(basePath.toString() + Path.SEPARATOR + "terr_cd=" + terrCd + Path.SEPARATOR + currPath.getName()); 
			}
			
			return(retPath);
			
		}
	}

	private static final String JOB_GRP_DESC = "Generate TLD DataHub Extracts";
	
	private FileSystem fileSystem = null;
	private Configuration hdfsConfig = null;
	private Path baseOutputPath = null;
//	private Path baseHivePath = null;
//	FsPermission newFilePremission;
	String createJobDetails = "TRUE";
	String multioutBaseOutputPath = "";
	String storeFilterFilePath = "" ;
	
	public static void main(String[] args) throws Exception {
		
		Configuration hdfsConfig = new Configuration();
				
		int retval = ToolRunner.run(hdfsConfig,new GenerateTLDDataHubFormatHCat(), args);

		System.out.println(" return value : " + retval);

	}
	
	String outputtotable = "false";
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

		if ( helpRequest ) {
			System.out.println("Usage: GenerateTLDDataHubFormat -c config.xml -t filetype -d territoryDateParms ");
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
			System.err.println("Usage: GenerateTLDDataHubFormat -c config.xml -t filetype -d territoryDateParms");
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

			hdfsConfig = getConf();
			fileSystem = FileSystem.get(hdfsConfig);
			
			hdfsConfig.set("USE_STORE_FILTER",useStoreFilter);

			runJob(daasConfig,fileType,terrDate,true);
			
		} else {
			System.err.println("Invalid Config XML file, stopping");
			System.err.println(daasConfig.errText());
			System.exit(8);
		}
	
		return(0);
	}

	private void runJob(DaaSConfig daasConfig
                       ,String fileType
                       ,String terrDate
                       ,boolean compressOut) {

		ABaC abac;
		ArrayList<String> lastList;
		StringBuffer terrDateList = new StringBuffer();
		
		Job job;
		
		int jobGrpId = 0;

		ArrayList<Path> requestedPaths;
		
		
		try {

			baseOutputPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir() + Path.SEPARATOR + "TLDDataHub");
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, baseOutputPath,daasConfig.displayMsgs());

			if ( daasConfig.displayMsgs() ) {
				System.out.println("\nOutput path = " + baseOutputPath.toString() + "\n");
			}
		
			hdfsConfig.set("mapred.child.java.opts", daasConfig.fileMapReduceJavaHeapSizeParm()); 

			if ( compressOut ) {
				hdfsConfig.set("mapreduce.map.output.compress", "true");
				hdfsConfig.set("mapreduce.output.fileoutputformat.compress", "true");
				hdfsConfig.set("mapreduce.output.fileoutputformat.compress.type", "RECORD");
				hdfsConfig.set("mapreduce.map.output.compress.codec","org.apache.hadoop.io.compress.SnappyCodec");
				hdfsConfig.set("mapreduce.output.fileoutputformat.compress.codec","org.apache.hadoop.io.compress.GzipCodec");
			}
			
			abac = new ABaC(daasConfig);

			if("TRUE".equalsIgnoreCase(createJobDetails)){
				jobGrpId = abac.createJobGroup(JOB_GRP_DESC);
			}

			if ( terrDate.toUpperCase().startsWith("LAST") ) {

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
						abac.closeJobGroup(jobGrpId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
						abac.dispose();
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
			
			job = Job.getInstance(hdfsConfig, "Creating TLD Datahub Format");
			
			HashSet<String> distinctTerrCdBusnDts = new HashSet<String>();
			String[] temp;
			String dt;
			
//			FileInputFormat.addInputPaths(job, "/daas/work/np_xml/step1/STLDRxD126840RxD12620150120-r-00055.gz");
			String multioutpath;
			String dtwithdashes;
			for (Path addPath : requestedPaths ) {
				FileInputFormat.addInputPath(job, addPath);
				
				temp = addPath.toString().split("/");
				
				dt  = temp[temp.length-1].substring(0,4)+"RxD045"+temp[temp.length-1].substring(4,6)+"RxD045"+temp[temp.length-1].substring(6, 8);

				dtwithdashes = temp[temp.length-1].substring(0,4)+"-"+temp[temp.length-1].substring(4,6)+"-"+temp[temp.length-1].substring(6, 8);
				
				
				multioutpath= multioutBaseOutputPath+"/terr_cd="+temp[temp.length-2]+"/pos_busn_dt="+dtwithdashes;
				
				System.out.println(" multioutpath " + multioutpath);
				if(fileSystem.delete(new Path(multioutpath),true)){
					System.out.println(" delete  " + multioutpath + " success ");
				}else{
					System.out.println(" delete  " + multioutpath + " failed ");
				}
				
				
				distinctTerrCdBusnDts.add(temp[temp.length-2]+"RxD126"+dt);
				
				System.out.println(" adding " + temp[temp.length-2]+"RxD126"+dt);
			}
			
			Iterator<String> distinctTerrCdBusnDtsIt = distinctTerrCdBusnDts.iterator();
			
			
			while(distinctTerrCdBusnDtsIt.hasNext()){
				
				MultipleOutputs.addNamedOutput(job, distinctTerrCdBusnDtsIt.next(), TextOutputFormat.class, NullWritable.class, Text.class);
			}
			
//			FileInputFormat.addInputPath(job, new Path("/daas/gold/np_xml/DetailedSOS/840/20141225/DetailedSOS~840~20141225~0000009.gz"));
//			FileInputFormat.addInputPath(job, new Path("/daas/gold/np_xml/STLD/840/20141225/STLD~840~20141225~0000011.gz"));

			Path daypartDistCache = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + "distcachefiles" + Path.SEPARATOR + "DayPart_ID.psv");
			
//			Path locationList = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + "cache" + Path.SEPARATOR + "offers_include_list.txt");
			Path locationList = new Path(storeFilterFilePath);
			
			job.addCacheFile(new URI(daypartDistCache.toString() + "#" + daypartDistCache.getName()));
			job.addCacheFile(new URI(locationList.toString() + "#" + locationList.getName()));
			
//			Path locationList = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + "cache" + Path.SEPARATOR + "offers_include_list.txt");
			
//			if ( fileSystem.exists(locationList) ) {
//				fileSystem.delete(locationList, false);
//			}
			
//			createListDistCache(locationList,daasConfig);
//			fileSystem.setPermission(locationList,newFilePremission);
			
//			job.addCacheFile(new URI(locationList.toString() + "#" + locationList.getName()));

//			LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
			
			job.setJarByClass(GenerateTLDDataHubFormatHCat.class);
			job.setMapperClass(TLDDataHubMapperHCat.class);
			
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(Text.class);
			job.setOutputKeyClass(NullWritable.class);
			job.setOutputKeyClass(HCatRecord.class);
			
		

			if(outputtotable.equalsIgnoreCase("true")){
				job.setOutputFormatClass(HCatOutputFormat.class);
				
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
				
				job.setReducerClass(TLDDataHubReducerMultiOut.class);
				LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
				TextOutputFormat.setOutputPath(job, baseOutputPath);
			}
			
			
			
			if ( ! job.waitForCompletion(true) ) {
				System.err.println("Error occured in MapReduce process, stopping");
				System.exit(8);
			}

			if("TRUE".equalsIgnoreCase(createJobDetails)){
			abac.closeJobGroup(jobGrpId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
			abac.dispose();
			}
			
			
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormat.runJob:");
			System.err.println(ex.toString());
			ex.printStackTrace();
			System.exit(8);
		}

	}

	private ArrayList<Path> getVaildFilePaths(DaaSConfig daasConfig
                                             ,String fileType
                                             ,String requestedTerrDateParms) {

		ArrayList<Path> retPaths = new ArrayList<Path>();

		try {

			Path[] requestPaths = HDFSUtil.requestedArgsPaths(fileSystem, daasConfig, requestedTerrDateParms, "STLD", "DetailedSOS");

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
			System.err.println("Error occured in GenerateOffersReport.getVaildFilePaths:");
			System.err.println(ex.toString());
			ex.printStackTrace();
			System.exit(8);
		}

		return(retPaths);
			
	}
	
	private void createListDistCache(Path locationList
			                        ,DaaSConfig daasConfig) {
		
		try {
			StringBuffer sql = new StringBuffer();
			ResultSet rset;

			if ( daasConfig.displayMsgs() ) {
		    	System.out.print("Connecting to GDW ... ");
		    }
		    
			RDBMS gdw = new RDBMS(RDBMS.ConnectionType.Teradata,daasConfig.gblTpid(),daasConfig.gblUserId(),daasConfig.gblPassword(),daasConfig.gblNumSessions());
		    
		    if ( daasConfig.displayMsgs() ) {
		    	System.out.println("done");
		    }

			sql.setLength(0);
			sql.append("select\n");
			sql.append("   d.CTRY_ISO_NU as TERR_CD\n"); 
			sql.append("  ,d.LGCY_LCL_RFR_DEF_CD\n");
			sql.append("from (select * from {VDB}.V1REST_CHAR_VAL where current_date between REST_CHAR_EFF_DT and coalesce(REST_CHAR_END_DT,cast('9999-12-31' as date))) a\n");
			sql.append("inner join {VDB}.V1GBAL_REST_CHAR b\n");
			sql.append("  on (b.GBAL_REST_CHAR_ID = a.GBAL_REST_CHAR_ID)\n");
			sql.append("inner join (select * from {VDB}.V1REST_CHAR_VLD_LIST where current_date between CHAR_VLD_VAL_EFF_DT and coalesce(CHAR_VLD_VAL_END_DT,cast('9999-12-31' as date))) c\n");
			sql.append("  on (c.REST_CHAR_VLD_LIST_ID = a.REST_CHAR_VLD_LIST_ID)\n");
			sql.append("inner join {VDB}.V1MCD_GBAL_BUSN_LCAT d\n");
			sql.append("  on (d.MCD_GBAL_LCAT_ID_NU = a.MCD_GBAL_LCAT_ID_NU)\n");
			sql.append("where b.GBAL_REST_CHAR_NA = 'XML Provided For'\n");  
			sql.append("and   c.CHAR_VLD_LIST_DS = 'Mobile Offers'\n");
			
			rset = gdw.resultSet(sql.toString().replaceAll("\\{VDB\\}", daasConfig.gblViewDb()));

			BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(fileSystem.create(locationList,true)));

			while ( rset.next() ) {
				bw.write(rset.getString("TERR_CD") + "\t" + rset.getString("LGCY_LCL_RFR_DEF_CD") + "\t1955-04-15\t9999-12-31");
				bw.write("\n");
			}

			rset.close();
			
			bw.close();
			
		} catch (Exception ex) {
			System.err.println("Error occured in GenerateOffersReport.createListDistCache:");
			System.err.println(ex.toString());
			ex.printStackTrace();
			System.exit(8);
		}
		
	}
}
	
