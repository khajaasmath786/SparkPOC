package com.mcd.gdw.daas.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.abac.ABaC;
import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;
import com.mcd.gdw.daas.util.RDBMS;
import com.mcd.gdw.daas.util.SimpleEncryptAndDecrypt;
import com.mcd.gdw.daas.mapreduce.ExtractICRawXMLFormatMapper;
import com.mcd.gdw.daas.mapreduce.ExtractICRawXMLFormatReducer;

public class GenerateExtractICRawXMLFormat  extends Configured implements Tool {

	public class ResultListFilter implements PathFilter {
		
		private String prefix = "";
		
		public ResultListFilter() {
			
		}
		
		public ResultListFilter(String prefix) {
		
			this.prefix = prefix;
			
		}
		
	    public boolean accept(Path path) {
	    	
	    	boolean retFlag = false; 
	    	
	    	if ( this.prefix.length() == 0 ) {
		    	retFlag = !path.getName().startsWith("_");
	    	} else {
	    		retFlag = path.getName().startsWith(prefix);
	    	}
	    	
	    	return(retFlag);
	    }
	}
	
	public static final String SEPARATOR_CHARACTER               = "\t";
	public static final String ALT_SEPARATOR_CHARACTER           = "~";
	public static final String CONFIG_SETTING_FILE_TYPES         = "com.mcd.gdw.daas.filetypes";
	public static final String CONFIG_SETTING_IS_HISTORY_REQUEST = "com.mcd.gdw.daas.ishistoryrequest";

	public static final int KEY_POS_TERR_CD                      = 0;
	public static final int KEY_POS_LGCY_LCL_RFR_DEF_CD          = 1;
	public static final int KEY_POS_POS_BUSN_DT                  = 2;
	
	public final static String DIST_CACHE_SELECTION_LIST         = "extract_raw_list.txt";
	
	private static final int MAX_UNIQUE_DAYS                     = 10;
	private static final int MAX_UNIQUE_STORES                   = 100;
	
	private static final String JOB_NAME                         = "Extract IC Raw XML Format";

	private static final String SELECTED_FILETYPES               = "STLD\tDetailedSOS\tMenuItem\tSecurityData";

	private FileSystem fileSystem = null;
	private Configuration hdfsConfig = null;
	private Path baseOutputPath = null;
	private FsPermission newFilePremission;
	
	private ArrayList<String> workTerrCodeList = new ArrayList<String>();
	
	private StringBuffer sql = new StringBuffer();
	
	private RDBMS sqlServer;
	private ABaC abac = null;
	private int jobGroupId;
	private int jobId;
	
	public static void main(String[] args) throws Exception {
		
		Configuration hdfsConfig = new Configuration();
				
		int retval = ToolRunner.run(hdfsConfig,new GenerateExtractICRawXMLFormat(), args);

		System.out.println(" return value : " + retval);
		
		System.exit(retval);
		
	}
	
	public int run(String[] args) throws Exception {
		
		SimpleEncryptAndDecrypt decrypt;
		String configXmlFile = "";
		String fileType = "";
		String requestType = "";
		String server = "";
		String userId = "";
		String password = "";
		String database = "";
		String loadDbOnlyText = "false";
		boolean loadDbOnly = false;
		boolean isHistoryRequest;

		for ( int idx=0; idx < args.length; idx++ ) {
			if ( args[idx].equals("-c") && (idx+1) < args.length ) {
				configXmlFile = args[idx+1];
			}

			if ( args[idx].equals("-t") && (idx+1) < args.length ) {
				fileType = args[idx+1];
			}

			if ( args[idx].equals("-requesttype") && (idx+1) < args.length ) {
				requestType = args[idx+1];
			}

			if ( args[idx].equals("-server") && (idx+1) < args.length ) {
				server = args[idx+1];
			}

			if ( args[idx].equals("-userid") && (idx+1) < args.length ) {
				userId = args[idx+1];
			}

			if ( args[idx].equals("-password") && (idx+1) < args.length ) {
				password = args[idx+1];
			}

			if ( args[idx].equals("-database") && (idx+1) < args.length ) {
				database = args[idx+1];
			}

			if ( args[idx].equals("-loaddbonly") && (idx+1) < args.length ) {
				loadDbOnlyText = args[idx+1];
			}
	
		}
		
		if ( loadDbOnlyText.equalsIgnoreCase("TRUE") || loadDbOnlyText.equalsIgnoreCase("T") || loadDbOnlyText.equalsIgnoreCase("YES") || loadDbOnlyText.equalsIgnoreCase("Y") ) {
			loadDbOnly = true;
		}
		
		if ( configXmlFile.length() == 0 || fileType.length() == 0 || requestType.length() == 0 || server.length() == 0 || userId.length() == 0 || password.length() == 0 || database.length() == 0) {
			System.err.println("Missing config.xml (-c), filetype (-t), server (-server), userId (-userid), password (-password), database (-database)");
			System.err.println("Usage: ExtractRawXML -c config.xml -t filetype -server server -userid userId -password password -database database");
			
			System.err.println("Arguments supplied = ");
			for ( int idx = 0; idx < args.length; idx++ ) {
				System.err.print(" " + args[idx]);
			}
			System.err.println("");
			System.exit(8);
		}

		isHistoryRequest = requestType.equalsIgnoreCase("HISTORY");
		
		DaaSConfig daasConfig = new DaaSConfig(configXmlFile, fileType);
		
		if ( daasConfig.configValid() ) {
			
			if ( daasConfig.displayMsgs()  ) {
				System.out.println(daasConfig.toString());
			}

			hdfsConfig = getConf();
			//AWS START			
			//fileSystem = FileSystem.get(hdfsConfig);
			fileSystem = HDFSUtil.getFileSystem(daasConfig, hdfsConfig);
			//AWS END

			decrypt = new SimpleEncryptAndDecrypt();
			
			sqlServer = new RDBMS(RDBMS.ConnectionType.SQLServer, server, userId, decrypt.decryptFromHexString(password));
			sqlServer.setAutoCommit(false);
			sqlServer.setBatchSize(50);

			abac = new ABaC(daasConfig);

			jobGroupId = abac.createJobGroup(JOB_NAME);
		
			newFilePremission = new FsPermission(FsAction.ALL,FsAction.ALL,FsAction.ALL);
			baseOutputPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir() + Path.SEPARATOR + "ExtractICRawXMLFormat");

			if ( !loadDbOnly ) {
				runJob(daasConfig,fileType,isHistoryRequest,database);
			}
			
			updTarget(daasConfig,database,isHistoryRequest);
			
			sqlServer.commit();
			sqlServer.dispose();
			
			abac.closeJobGroup(jobGroupId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
			abac.dispose();
		}
		
		return(0);	
		
	}
	
	private void runJob(DaaSConfig daasConfig
                       ,String fileType
                       ,boolean isHistoryRequest
                       ,String database) {
		
		Job job;
		String requestedTerrDateParms;
		Path distListCache = null;
		ArrayList<Path> requestedPaths = null;
		String[] fileTypes = SELECTED_FILETYPES.split(SEPARATOR_CHARACTER);
		String fileTypesValue = "";
		String stepName = "Source Data From: ";
		
		for ( int idx=0; idx < fileTypes.length; idx++ ) {
			if ( fileTypesValue.length() > 0 ) {
				fileTypesValue += SEPARATOR_CHARACTER;
			}
			
			fileTypesValue += fileTypes[idx] + ALT_SEPARATOR_CHARACTER + idx; 
		}
		
		try {
			jobId = abac.createJob(jobGroupId, 1, stepName);
			
			distListCache = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + "cache" + Path.SEPARATOR + DIST_CACHE_SELECTION_LIST);

			requestedTerrDateParms = getList(daasConfig,database,distListCache,isHistoryRequest);
			
			fileSystem.setPermission(distListCache, newFilePremission);
			
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, baseOutputPath,daasConfig.displayMsgs());
			
			if ( daasConfig.displayMsgs() ) {
				System.out.println("\nOutput path = " + baseOutputPath.toString() + "\n");
			}
			
			if ( requestedTerrDateParms.equals("NONE") ) {
				HDFSUtil.createHdfsSubDirIfNecessary(fileSystem, baseOutputPath,daasConfig.displayMsgs());
			} else {
				hdfsConfig.set(CONFIG_SETTING_FILE_TYPES, fileTypesValue);
				
				if ( isHistoryRequest ) {
					hdfsConfig.set(CONFIG_SETTING_IS_HISTORY_REQUEST, "TRUE");
					stepName += "History";
				} else {
					hdfsConfig.set(CONFIG_SETTING_IS_HISTORY_REQUEST, "FALSE");
					stepName += "Daily Files";
				}
				
				setOutputCompression(true,daasConfig.displayMsgs());
				
				job = Job.getInstance(hdfsConfig, JOB_NAME);
				
				if ( requestedTerrDateParms.toUpperCase().startsWith("WORK") ) {
					String[] workParts = (requestedTerrDateParms + ":").split(":");
					
					String filterTerrCodeList = workParts[1];

					if ( filterTerrCodeList.length() > 0 ) {
						System.out.println("Work Layer using only the following Territory Codes:");
						String[] parts = filterTerrCodeList.split(",");
						for ( String addTerrCode : parts ) {
							if ( daasConfig.displayMsgs() ) {
								System.out.println("    " + addTerrCode);
							}
							workTerrCodeList.add(addTerrCode);
						}
					}

					ArrayList<String> subTypeCodes = new ArrayList<String>();
					
					for ( int idx=0; idx < fileTypes.length; idx++ ) {
						subTypeCodes.add(fileTypes[idx]);
					}
					
					requestedPaths = getVaildFilePaths(daasConfig,fileType,subTypeCodes);
				} else {
					requestedPaths = getVaildFilePaths(daasConfig,fileType,requestedTerrDateParms,fileTypes);
				}

				for (Path addPath : requestedPaths ) {
					FileInputFormat.addInputPath(job, addPath);
				}
				
				job.addCacheFile(new URI(distListCache.toString() + "#" + distListCache.getName()));

				LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
				
				job.setJarByClass(GenerateExtractICRawXMLFormat.class);
				job.setMapperClass(ExtractICRawXMLFormatMapper.class);
				job.setReducerClass(ExtractICRawXMLFormatReducer.class);
				job.setMapOutputKeyClass(Text.class);
				job.setMapOutputValueClass(Text.class);
				job.setOutputKeyClass(NullWritable.class);
				job.setOutputKeyClass(Text.class);
				TextOutputFormat.setOutputPath(job, baseOutputPath);

				if ( ! job.waitForCompletion(true) ) {
					System.err.println("Error occured in MapReduce process, stopping");
					System.exit(8);
				}
			}
				
			abac.closeJob(jobId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			
			try {
				abac.closeJob(jobId, DaaSConstants.JOB_FAILURE_ID, DaaSConstants.JOB_FAILURE_CD);
				abac.closeJobGroup(jobGroupId, DaaSConstants.JOB_FAILURE_ID, DaaSConstants.JOB_FAILURE_CD);
				abac.dispose();
				
			} catch ( Exception ex1 ) {
			}
			
			System.exit(8);
		}
		
	}

	private void updTarget(DaaSConfig daasConfig
			              ,String database
			              ,boolean isHistoryRequest) {
		
		FileStatus[] status;
		InputStreamReader insr = null;
		BufferedReader br = null;
		String line = null;
		String[] parts;
		int rowCnt;
		int totCnt=0;

		try {
			if ( fileSystem.isDirectory(baseOutputPath) ) {

				jobId = abac.createJob(jobGroupId, 2, "Stage data to SQL Server Temp Table");
				
				fileSystem.setPermission(baseOutputPath, newFilePremission);
				
				status = fileSystem.listStatus(baseOutputPath, new ResultListFilter("_SUCCESS"));

				for ( int idx=0; idx < status.length; idx++ ) {
					fileSystem.setPermission(status[idx].getPath(), newFilePremission);
				}
				
				status = fileSystem.listStatus(baseOutputPath, new ResultListFilter());
				
				sql.setLength(0);
				sql.append("CREATE TABLE #TMP_DLY_STLD_XML (\n");
				sql.append("   CTRY_ISO_NU smallint NOT NULL,\n");
				sql.append("   LGCY_LCL_RFR_DEF_CD varchar(12) NOT NULL,\n");
				sql.append("   BIZ_DT date NOT NULL,\n");
				sql.append("   STLD varchar(max) NOT NULL,\n");
				sql.append("   DetailedSOS varchar(max) NOT NULL,\n");
				sql.append("   MenuItem varchar(max) NOT NULL,\n");
				sql.append("   Security varchar(max) NOT NULL,\n");
			    sql.append("PRIMARY KEY CLUSTERED ( CTRY_ISO_NU, LGCY_LCL_RFR_DEF_CD, BIZ_DT));\n");			
				
				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString());
				}
				
			    sqlServer.executeUpdate(sql.toString());
			    
			    sql.setLength(0);
				sql.append("insert into #TMP_DLY_STLD_XML (\n");
				sql.append("   CTRY_ISO_NU\n");
				sql.append("  ,LGCY_LCL_RFR_DEF_CD\n");
				sql.append("  ,BIZ_DT\n");
				sql.append("  ,STLD\n");
				sql.append("  ,DetailedSOS\n");
				sql.append("  ,MenuItem\n");
				sql.append("  ,Security)\n");
				sql.append("values (?,?,?,?,?,?,?)\n");
				
				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString());
				}

				sqlServer.setPreparedStatement(sql.toString()); 
						
				for ( int idx=0; idx < status.length; idx++ ) {
					fileSystem.setPermission(status[idx].getPath(), newFilePremission);
					
					if ( status[idx].getPath().getName().endsWith(".gz") ) {
						insr = new InputStreamReader(new GZIPInputStream(fileSystem.open(status[idx].getPath())));
					} else {
						insr = new InputStreamReader(new DataInputStream(fileSystem.open(status[idx].getPath())));
					}
					
					br = new BufferedReader(insr);
					
					if(br != null){
						
						while( (line = br.readLine()) != null){
							parts = line.split(SEPARATOR_CHARACTER);
							
							//System.out.println(parts[0] + "|" + parts[1] + "|" + parts[2].substring(0, 4) + "-" + parts[2].substring(4, 6) + "-" + parts[2].substring(6, 8) + "|" + parts[3].length() + "|" + parts[4].length() + "|" + parts[5].length() + "|" + parts[6].length());
							
							rowCnt = sqlServer.addBatch(Short.parseShort(parts[0])
									                   ,parts[1]
									                   ,java.sql.Date.valueOf(parts[2].substring(0, 4) + "-" + parts[2].substring(4, 6) + "-" + parts[2].substring(6, 8))
									                   ,parts[3]
									                       ,parts[4]
											           ,parts[5]
											           ,parts[6]
									                   );
							
							if ( rowCnt > 0 ) {
								totCnt += rowCnt;
								
								if ( daasConfig.displayMsgs() ) {
									System.out.println("Loaded " + totCnt + " rows");
								}
							}
						}
						
						br.close();
						insr.close();
					}
						
				}
				
				totCnt = sqlServer.finalizeBatch();
				
				if ( daasConfig.displayMsgs() ) {
					System.out.println("Loaded " + totCnt + " rows\n");
				}
				
				sqlServer.commit();

				abac.insertExecutionTargetFile(jobId, 1, "#TMP_DLY_STLD_XML", "Innovation Center Raw XML Extract Staging Table", "Temp Table", totCnt);
				abac.closeJob(jobId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);

				jobId = abac.createJob(jobGroupId, 3, "Merge (insert/update) data in final table");
				
				sql.setLength(0);
				sql.append("merge into ");
				sql.append(database);
				sql.append(".DLY_STLD_XML t\n");
				sql.append("using (\n");
				sql.append("   select\n"); 
				sql.append("      CTRY_ISO_NU\n");
				sql.append("     ,LGCY_LCL_RFR_DEF_CD\n");
				sql.append("     ,BIZ_DT\n");
				sql.append("     ,STLD\n");
				sql.append("     ,DetailedSOS\n");
				sql.append("     ,MenuItem\n");
				sql.append("     ,Security\n");
				sql.append("   from #TMP_DLY_STLD_XML) s\n");
				sql.append("  on (t.CTRY_ISO_NU = s.CTRY_ISO_NU\n");
				sql.append("      and t.LGCY_LCL_RFR_DEF_CD = s.LGCY_LCL_RFR_DEF_CD\n");
				sql.append("      and t.BIZ_DT = s.BIZ_DT)\n");
				sql.append("when matched then update\n");             
				sql.append("  set STLD = s.STLD\n");
				sql.append("     ,DetailedSOS = s.DetailedSOS\n");
				sql.append("     ,MenuItem = s.MenuItem\n");
				sql.append("     ,Security = s.Security\n");
				sql.append("when not matched then insert (\n");
				sql.append("   CTRY_ISO_NU\n");
				sql.append("  ,LGCY_LCL_RFR_DEF_CD\n");
				sql.append("  ,BIZ_DT\n");
				sql.append("  ,STLD\n");
				sql.append("  ,DetailedSOS\n");
				sql.append("  ,MenuItem\n");
				sql.append("  ,Security) values (\n");
				sql.append("   s.CTRY_ISO_NU\n");
				sql.append("  ,s.LGCY_LCL_RFR_DEF_CD\n");
				sql.append("  ,s.BIZ_DT\n");
				sql.append("  ,s.STLD\n");
				sql.append("  ,s.DetailedSOS\n");
				sql.append("  ,s.MenuItem\n");
				sql.append("  ,s.Security);");
				
				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString());
				}

				if ( daasConfig.displayMsgs() ) {
					System.out.print("Started finalizing data ... ");
				}
				
			    totCnt = sqlServer.executeUpdate(sql.toString());
				
			    if ( isHistoryRequest ) {
					sql.setLength(0);
					sql.append("merge into ");
					sql.append(database);
					sql.append(".HIST_LCAT_LIST t\n");
					sql.append("using (\n");
					sql.append("   select\n"); 
					sql.append("      CTRY_ISO_NU\n");
					sql.append("     ,LGCY_LCL_RFR_DEF_CD\n");
					sql.append("     ,BIZ_DT as CAL_DT\n");
					sql.append("     ,cast(1 as bit) as RDY_FL\n");
					sql.append("   from #TMP_DLY_STLD_XML) s\n");
					sql.append("  on (t.CTRY_ISO_NU = s.CTRY_ISO_NU\n");
					sql.append("      and t.LGCY_LCL_RFR_DEF_CD = s.LGCY_LCL_RFR_DEF_CD\n");
					sql.append("      and t.CAL_DT = s.CAL_DT)\n");
					sql.append("when matched then update\n");             
					sql.append("  set RDY_FL = s.RDY_FL;\n");
			    }
			    
			    sqlServer.executeUpdate(sql.toString());
			    
			    sqlServer.commit();

			    abac.insertExecutionTargetFile(jobId, 1, "DLY_STLD_XML", "Innovation Center Raw XML Extract Table", "Table", totCnt);

			    if ( daasConfig.displayMsgs() ) {
					System.out.println("done\n");
				}
				
				sql.setLength(0);
				sql.append("DROP TABLE #TMP_DLY_STLD_XML\n");
				
				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString());
				}
				
			    sqlServer.executeUpdate(sql.toString());

				abac.closeJob(jobId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
			}
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);

			try {
				abac.closeJob(jobId, DaaSConstants.JOB_FAILURE_ID, DaaSConstants.JOB_FAILURE_CD);
				abac.closeJobGroup(jobGroupId, DaaSConstants.JOB_FAILURE_ID, DaaSConstants.JOB_FAILURE_CD);
				abac.dispose();
				
			} catch ( Exception ex1 ) {
			}
			
			System.exit(8);
		}
		
	}
	
	private String getList(DaaSConfig daasConfig
			              ,String database
			              ,Path distListCache
			              ,boolean isHistoryRequest) throws Exception {

		String retValue = "";
		HashMap<String,String> uniqueValues = new HashMap<String,String>();
		ResultSet rset; 
		BufferedWriter bw = null;
		int locCnt;
		
		if ( isHistoryRequest ) {

			sql.setLength(0);
			sql.append("select\n");
			sql.append("   count(*) as LOC_CNT\n");
			sql.append("from (\n");
			sql.append("     select distinct\n");
			sql.append("        CTRY_ISO_NU\n");
			sql.append("       ,LGCY_LCL_RFR_DEF_CD\n");
			sql.append("     from " + database + ".HIST_LCAT_LIST\n");
			sql.append("     where INC_FL = 'Y') a\n");

		} else {

			sql.setLength(0);
			sql.append("select\n");
			sql.append("   count(*) as LOC_CNT\n");
			sql.append("from (\n");
			sql.append("     select distinct\n");
			sql.append("        CTRY_ISO_NU\n");
			sql.append("       ,LGCY_LCL_RFR_DEF_CD\n");
			sql.append("     from " + database + ".DLY_LCAT_LIST\n");
			sql.append("     where ACT_FL = 'Y') a\n");
		}
		
		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		rset = sqlServer.resultSet(sql.toString());
		rset.next();
		
		locCnt = rset.getInt("LOC_CNT");
		
		if ( locCnt > MAX_UNIQUE_STORES ) { 
			System.err.println(locCnt + " > " + MAX_UNIQUE_STORES);
			System.exit(8);
		}
		
		if ( locCnt == 0 ) {
			System.err.println("NO Locations to extract");
			retValue = "NONE";
		} else {

			if ( isHistoryRequest ) {

				sql.setLength(0);
				sql.append("select\n");
				sql.append("   count(*) as DT_CNT\n");
				sql.append("from (\n");
				sql.append("     select distinct\n");
				sql.append("        CAL_DT\n");
				sql.append("     from " + database + ".HIST_LCAT_LIST\n");
				sql.append("     where INC_FL = 'Y') a\n");
				
				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString());
				}
				
				rset = sqlServer.resultSet(sql.toString());
				rset.next();
				
				if ( rset.getInt("DT_CNT") > MAX_UNIQUE_DAYS ) { 
					System.err.println(rset.getInt("DT_CNT") + " > " + MAX_UNIQUE_DAYS);
					System.exit(8);
				}

				sql.setLength(0);
				sql.append("update ");
				sql.append(database);
				sql.append(".HIST_LCAT_LIST\n");
				sql.append("   set RDY_FL = 0\n");
				sql.append("where INC_FL = 'Y'\n");

				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString());
				}

				sqlServer.executeUpdate(sql.toString());
				sqlServer.commit();
				
				sql.setLength(0);
				sql.append("select\n"); 
				sql.append("   CTRY_ISO_NU\n");
				sql.append("  ,case when CTRY_ISO_NU = 840 then\n");
				sql.append("     case len(LGCY_LCL_RFR_DEF_CD) when 4 then '0'\n");
				sql.append("                                   when 3 then '00'\n");
				sql.append("                                   when 2 then '000'\n");
				sql.append("                                   when 1 then '0000'\n");
				sql.append("                                   else '' end + LGCY_LCL_RFR_DEF_CD\n");
				sql.append("   else LGCY_LCL_RFR_DEF_CD end as LGCY_LCL_RFR_DEF_CD\n");
				sql.append("  ,substring(cast(CAL_DT as CHAR(10)),1,4) + substring(cast(CAL_DT as CHAR(10)),6,2) + substring(cast(CAL_DT as CHAR(10)),9,2) as CAL_DT\n");
				sql.append("from " + database + ".HIST_LCAT_LIST\n");
				sql.append("where INC_FL = 'Y'\n");
				sql.append("and   RDY_FL = 0\n");
				
				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString());
				}
			} else {

				sql.setLength(0);
				sql.append("select\n");
				sql.append("   CTRY_ISO_NU\n");
				sql.append("  ,case when CTRY_ISO_NU = 840 then\n");
				sql.append("     case len(LGCY_LCL_RFR_DEF_CD) when 4 then '0'\n");
				sql.append("                                   when 3 then '00'\n");
				sql.append("                                   when 2 then '000'\n");
				sql.append("                                   when 1 then '0000'\n");
				sql.append("                                   else '' end + LGCY_LCL_RFR_DEF_CD\n");
				sql.append("   else LGCY_LCL_RFR_DEF_CD end as LGCY_LCL_RFR_DEF_CD\n");
				sql.append("from " + database + ".DLY_LCAT_LIST\n");
				sql.append("where ACT_FL = 'Y';");
				
				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString());
				}
			}

			bw=new BufferedWriter(new OutputStreamWriter(fileSystem.create(distListCache,true)));
			
			rset = sqlServer.resultSet(sql.toString());
			
			while ( rset.next() ) {
				if ( isHistoryRequest ) {
					if ( !uniqueValues.containsKey(rset.getString("CTRY_ISO_NU") + ":" + rset.getString("CAL_DT")) ) {
						uniqueValues.put(rset.getString("CTRY_ISO_NU") + ":" + rset.getString("CAL_DT"),"");
					}
					
					bw.write(rset.getString("CTRY_ISO_NU") + SEPARATOR_CHARACTER + rset.getString("LGCY_LCL_RFR_DEF_CD") + SEPARATOR_CHARACTER + rset.getString("CAL_DT") + "\n");
				} else {
					if ( !uniqueValues.containsKey(rset.getString("CTRY_ISO_NU")) ) {
						uniqueValues.put(rset.getString("CTRY_ISO_NU"),"");
					}
					
					bw.write(rset.getString("CTRY_ISO_NU") + SEPARATOR_CHARACTER + rset.getString("LGCY_LCL_RFR_DEF_CD") + "\n");
				}
			}
			
			rset.close();
			bw.close();
			
			for (Map.Entry<String, String> entry : uniqueValues.entrySet()) {
				if ( isHistoryRequest ) {
					if (retValue.length() > 0) {
						retValue += ",";
					}
					retValue += entry.getKey();
				} else {
					if (retValue.length() == 0) {
						retValue = "WORK";
					} 
					retValue += ":" + entry.getKey();
				}
			}
		}
		
		return(retValue);
	}
	
	private ArrayList<Path> getVaildFilePaths(DaaSConfig daasConfig
                                             ,String fileType
                                             ,ArrayList<String> subTypeCodes) {

		ArrayList<Path> retPaths = new ArrayList<Path>();
		String filePath;
		boolean useFilePath;
		boolean removeFilePath;
		String[] fileNameParts;
		String fileTerrCode;
		
		Path listPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir() + Path.SEPARATOR + daasConfig.fileSubDir() + Path.SEPARATOR + "step1");
		
		try {
			FileStatus[] fstus = fileSystem.listStatus(listPath);
			
			for (int idx=0; idx < fstus.length; idx++ ) {
				filePath = HDFSUtil.restoreMultiOutSpecialChars(fstus[idx].getPath().getName());
				
				useFilePath = false;
				for (int idxCode=0; idxCode < subTypeCodes.size(); idxCode++) {
					if ( filePath.startsWith(subTypeCodes.get(idxCode)) ) {
						useFilePath = true;
					}
				}
				
				if ( useFilePath && workTerrCodeList.size() > 0 ) {
					fileNameParts = filePath.split("~");
					fileTerrCode = fileNameParts[1];
					
					removeFilePath = true;
					
					for ( String checkTerrCode : workTerrCodeList ) {
						if ( fileTerrCode.equals(checkTerrCode) ) {
							removeFilePath = false;
						}
					}
					
					if ( removeFilePath ) {
 						useFilePath = false;
					}
				}
				
				if ( useFilePath ) {
					retPaths.add(fstus[idx].getPath());
					
					if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
						System.out.println("Added work source file =" + filePath);
					}
				}
			}
			
		} catch (Exception ex) {
			System.err.println("Error occured in GenerateAsterFormat.getVaildFilePaths:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}

		if ( retPaths.size() == 0 ) {
			System.err.println("Stopping, No valid files found");
			System.exit(8);
		}

		return(retPaths);
	}
	
	private ArrayList<Path> getVaildFilePaths(DaaSConfig daasConfig
                                             ,String fileType
                                             ,String requestedTerrDateParms
                                             ,String[] xmlTypes) {

		ArrayList<Path> retPaths = new ArrayList<Path>();
		System.out.println(" requestedTerrDateParms = " + requestedTerrDateParms);

		try {

			Path[] requestPaths = HDFSUtil.requestedArgsPaths(fileSystem, daasConfig, requestedTerrDateParms, xmlTypes);

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

	private void setOutputCompression(boolean compress
			                         ,boolean displayMsg) {

		if ( compress ) {
			hdfsConfig.set("mapreduce.map.output.compress", "true");
			hdfsConfig.set("mapreduce.output.fileoutputformat.compress", "true");
			hdfsConfig.set("mapreduce.output.fileoutputformat.compress.type", "RECORD");
			hdfsConfig.set("mapreduce.map.output.compress.codec","org.apache.hadoop.io.compress.SnappyCodec");
			hdfsConfig.set("mapreduce.output.fileoutputformat.compress.codec","org.apache.hadoop.io.compress.GzipCodec");
			
			if ( displayMsg ) {
				System.out.println("Set output compression on");
			}
		} else {
			hdfsConfig.set("mapreduce.map.output.compress", "false");
			hdfsConfig.set("mapreduce.output.fileoutputformat.compress", "false");
			hdfsConfig.set("mapreduce.output.fileoutputformat.compress.type", "BLOCK");
			hdfsConfig.set("mapreduce.map.output.compress.codec","org.apache.hadoop.io.compress.DefaultCodec");
			hdfsConfig.set("mapreduce.output.fileoutputformat.compress.codec","org.apache.hadoop.io.compress.DefaultCodec");			

			if ( displayMsg ) {
				System.out.println("Set output compression off");
			}
		}
	}

}
