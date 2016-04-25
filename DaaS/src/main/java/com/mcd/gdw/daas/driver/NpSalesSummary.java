package com.mcd.gdw.daas.driver;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.mapreduce.NpSalesSummaryCompKeyComparator;
import com.mcd.gdw.daas.mapreduce.NpSalesSummaryMapper;
import com.mcd.gdw.daas.mapreduce.NpSalesSummaryReducer;

import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.DataQualityReport;
import com.mcd.gdw.daas.util.DataQualityReport.DataQualityCorrectnessLine;
import com.mcd.gdw.daas.util.DataQualityReport.DataQualityCompletenessSummaryLine;
import com.mcd.gdw.daas.util.DataQualityReport.DataQualityCompletenessDetailLine;
import com.mcd.gdw.daas.util.DataQualityReport.ReportFormat;
import com.mcd.gdw.daas.util.DataQualityReport.ReportType;
import com.mcd.gdw.daas.util.HDFSUtil;

import com.mcd.gdw.daas.util.RDBMS;

public class NpSalesSummary extends Configured implements Tool {
	
	private static final int BUSN_DT_POS = 0;
	private static final int TERR_CD_POS = 1;
	private static final int LCAT_POS = 2;
	private static final int DAILY_SLS_NET_AMT_POS = 3;
	private static final int DAILY_SLS_GROSS_AMT_POS = 4;
	private static final int DAILY_SLS_TC_POS = 5;
	private static final int XML_AMT_POS = 6;
	private static final int XML_TC_POS = 7;
	private static final int NET_GROSS_FL_POS = 8;
	
	private static final int UTF8_BOM = 65279;
	
	private FileSystem fileSystem = null;
	private Configuration hdfsConfig = null;
	private Path baseOutputPath = null;
	private Path xmlSalesOutputPath = null;
	private Path dailySalesOutputPath = null;
	private Path dailySalesFile = null;
	
	private HashMap<String,String> terrMap = new HashMap<String,String>();
	private HashMap<String,String> lcatMap = new HashMap<String,String>();
	
	private String outHtmlNameAndPath2 = "";
	private String outXlsNameAndPath2 = "";
	private String outXlsxNameAndPath2 = "";
	private String summaryNameAndPath2 = ""; 
	private String outXlsNameAndPath1 = "";
	private String outXlsxNameAndPath1 = "";
	
	private StringBuffer sql = new StringBuffer();

	private RDBMS abac;
	private RDBMS gdw;

	private boolean includeGdwFl = false; 
	
	private ArrayList<String> uniqueKeys = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		
		Configuration hdfsConfig = new Configuration();
				
		int retval = ToolRunner.run(hdfsConfig,new NpSalesSummary(), args);

		System.out.println(" return value : " + retval);
	}

	public int run(String[] args) throws Exception {

		String configXmlFile = "";
		String fileType = "";
		String terrDate = "";
		String salesParm = "";
		File salesFile = null;
		boolean helpRequest = false;
		String owshFltr = "*";
		
		for ( int idx=0; idx < args.length; idx++ ) {
			if ( args[idx].equals("-c") && (idx+1) < args.length ) {
				configXmlFile = args[idx+1];
			}

			if ( args[idx].equals("-t") && (idx+1) < args.length ) {
				fileType = args[idx+1];
			}

			if ( args[idx].equals("-d") && (idx+1) < args.length ) {
				terrDate = args[idx+1];
			}

			if ( args[idx].equals("-sls") && (idx+1) < args.length ) {
				salesParm = args[idx+1];
			}

			if ( args[idx].equals("-owshfltr") && (idx+1) < args.length ) {
				owshFltr = args[idx+1];
			}

			if ( args[idx].equals("-xls1") && (idx+1) < args.length ) {
				outXlsNameAndPath1 = args[idx+1];
			}

			if ( args[idx].equals("-xlsx1") && (idx+1) < args.length ) {
				outXlsxNameAndPath1 = args[idx+1];
			}

			if ( args[idx].equals("-s2") && (idx+1) < args.length ) {
				summaryNameAndPath2 = args[idx+1];
			}
			
			if ( args[idx].equals("-htm2") && (idx+1) < args.length ) {
				outHtmlNameAndPath2 = args[idx+1];
			}

			if ( args[idx].equals("-xls2") && (idx+1) < args.length ) {
				outXlsNameAndPath2 = args[idx+1];
			}

			if ( args[idx].equals("-xlsx2") && (idx+1) < args.length ) {
				outXlsxNameAndPath2 = args[idx+1];
			}
			
			if ( args[idx].toUpperCase().equals("-H") || args[idx].toUpperCase().equals("-HELP")  ) {
				helpRequest = true;
			}
		}
		
		if ( helpRequest ) {
			System.out.println("Usage: NpSalesSummary -c config.xml -t filetype -d territoryDateParms -sls GDW|salesFile -xls1 xls1Out or -xlsx1 xls1Out -s2 summaryFile2 -htm2 htmOut2 and/or -xls2 xlsOut2 and/or -xlsx2 xlsxOut2");
			System.out.println("where territoryDateParm is a comma separated list of territory codes and dates separated by colons(:)");
			System.out.println("for example, 840:2012-07-01:2012-07-07 is territory 840 from July 1st, 2012 until July 7th, 2012.");
			System.out.println("the date format is either ISO YYYY-MM-DD or YYYYMMDD (both are valid)");
			System.out.println("If only one date is supplied then a single day is used for that territory");
			System.out.println("Multiple territoryDateParm can be specified as comma separated values: 840:20120701,840:2012-07-05:2012-07-08,250:2012-08-01");
			System.out.println("This will get a total of 3 days for 840 and 1 day from 250");
			System.exit(0);
		}
		
		if ( configXmlFile.length() == 0 || fileType.length() == 0 || terrDate.length() == 0 || salesParm.length() == 0 || (outXlsNameAndPath1.length() == 0 && outXlsxNameAndPath1.length() == 0 && summaryNameAndPath2.length() == 0 && outHtmlNameAndPath2.length() == 0 && outXlsNameAndPath2.length() == 0 && outXlsxNameAndPath2.length() == 0) ) {
			System.err.println("Missing config.xml (-c), filetype (t), territoryDateParms (-d), GDW|salesFile (-sls), xlsout (xls1)|xlsxout (xlsx1), summaryFile (-s2) and htmlout (htm2)|xlsout (xls2)|xlsxout (xlsx2)  ");
			System.err.println("Usage: NpSalesSummary -c config.xml -t filetype -d territoryDateParms -htm htmOut and/or -xls xlsOut and/or -xlsxOut");
			System.err.println("where territoryDateParm is a comma separated list of territory codes and dates separated by colons(:)");
			System.err.println("for example, 840:2012-07-01:2012-07-07 is territory 840 from July 1st, 2012 until July 7th, 2012.");
			System.err.println("the date format is either ISO YYYY-MM-DD or YYYYMMDD (both are valid)");
			System.err.println("If only one date is supplied then a single day is used for that territory");
			System.err.println("Multiple territoryDateParm can be specified as comma separated values: 840:20120701,840:2012-07-05:2012-07-08,250:2012-08-01");
			System.err.println("This will get a total of 3 days for 840 and 1 day from 250");
			System.exit(8);
		}

		if ( salesParm.equalsIgnoreCase("GDW") ) {
			includeGdwFl = true;
		} else {
			salesFile = new File(salesParm);
			
			if ( !salesFile.isFile() ) {
				System.err.println("Sales File (-sls) parameter not a valid file: " + salesParm);
				System.exit(8);
			}
		}
		
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

			baseOutputPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir() + Path.SEPARATOR + "data_quality");
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, baseOutputPath,daasConfig.displayMsgs());

			if ( daasConfig.displayMsgs() ) {
				System.out.println("\nOutput path = " + baseOutputPath.toString() + "\n");
			}

			xmlSalesOutputPath = new Path(baseOutputPath.toString() + Path.SEPARATOR + "xml_sales");
			dailySalesOutputPath = new Path(baseOutputPath.toString() + Path.SEPARATOR + "daily_sales");

			ArrayList<Path> requestedPaths = getVaildFilePaths(daasConfig,fileType,terrDate);
			
			connectSQL(daasConfig,includeGdwFl);
			
			getUniqueKeys(daasConfig,requestedPaths);

			if ( outXlsNameAndPath1.length() > 0 ) {
				createCompletenessReport(ReportFormat.XLS,outXlsNameAndPath1,daasConfig, owshFltr);
			}

			if ( outXlsxNameAndPath1.length() > 0 ) {
				createCompletenessReport(ReportFormat.XLSX,outXlsxNameAndPath1,daasConfig, owshFltr);
			}
			
			getLcatDataFromABaC(daasConfig);
			getSalesData(daasConfig,salesFile); 
			
			runJob(daasConfig,fileType,requestedPaths,owshFltr);
			
			if ( outHtmlNameAndPath2.length() > 0 ) {
				if ( daasConfig.displayMsgs() ) {
					System.out.print("Creating HTML Report ... ");
				}
				createCorrectnessReport(ReportFormat.HTML,outHtmlNameAndPath2,daasConfig);
				if ( daasConfig.displayMsgs() ) {
					System.out.println("done");
				}
			}
			
			if ( outXlsNameAndPath2.length() > 0 ) {
				if ( daasConfig.displayMsgs() ) {
					System.out.print("Creating Excel (XLS) Report ... ");
				}
				createCorrectnessReport(ReportFormat.XLS,outXlsNameAndPath2,daasConfig);
				if ( daasConfig.displayMsgs() ) {
					System.out.println("done");
				}
			}
			
			if ( outXlsxNameAndPath2.length() > 0 ) {
				if ( daasConfig.displayMsgs() ) {
					System.out.print("Creating Excel (XLSX) Report ... ");
				}
				createCorrectnessReport(ReportFormat.XLSX,outXlsxNameAndPath2,daasConfig);
				if ( daasConfig.displayMsgs() ) {
					System.out.println("done");
				}
			}
			
		} else {
			System.err.println("Invalid config.xml and/or filetype");
			System.err.println("Config File    = " + configXmlFile);
			System.err.println("File Type      = " + fileType);
			System.err.println(daasConfig.errText());
			System.exit(8);
		}

		try {
		    gdw.dispose();
		    abac.dispose();
		} catch (Exception ex) {
		}
		
		return(0);
		
	}
	
	private void runJob(DaaSConfig daasConfig
                       ,String fileType
                       ,ArrayList<Path> requestedPaths
                       ,String owshFltr) {
				
		Job job;

		hdfsConfig.set("mapred.child.java.opts", daasConfig.fileMapReduceJavaHeapSizeParm()); 
		hdfsConfig.set(DaaSConstants.JOB_CONFIG_PARM_OWNERSHIP_FILTER, owshFltr);

		try {
			job = new Job(hdfsConfig, "Creating Summary for Daily Sales and Transaction Counts");

			for (Path addPath : requestedPaths ) {
				FileInputFormat.addInputPath(job, addPath);
			}

			FileInputFormat.addInputPath(job, dailySalesFile);

			job.setJarByClass(NpSalesSummary.class);
			job.setMapperClass(NpSalesSummaryMapper.class);
			job.setReducerClass(NpSalesSummaryReducer.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(Text.class);
			job.setOutputKeyClass(NullWritable.class);
			job.setOutputKeyClass(Text.class);
			job.setSortComparatorClass(NpSalesSummaryCompKeyComparator.class);
			job.setNumReduceTasks(1);
			job.setOutputFormatClass(TextOutputFormat.class);
			TextOutputFormat.setOutputPath(job, xmlSalesOutputPath);
	
			if ( ! job.waitForCompletion(true) ) {
				System.err.println("Error occured in MapReduce process, stopping");
				System.exit(8);
			}
			
		} catch (Exception ex) {
			System.err.println("Error occured in NpSalesSummary.runJob:");
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
		
			Path[] requestPaths = HDFSUtil.requestedArgsPaths(fileSystem, daasConfig, requestedTerrDateParms, "STLD", "Store-Db");
		
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
			System.err.println("Error occured in NpSalesSummary.getVaildFilePaths:");
			System.err.println(ex.toString());
			ex.printStackTrace();
			System.exit(8);
		}

		return(retPaths);
		
	}

	private void getUniqueKeys(DaaSConfig daasConfig
                              ,ArrayList<Path> requestedPaths) {
		
		HashMap<String,Integer> terrDateMap = new HashMap<String,Integer>();
		String[] parts;
		String terrCd;
		String busnDt;
		String key;
		
		try {
			
			for ( Path requestedPath : requestedPaths ) {
				parts = requestedPath.toString().split(Path.SEPARATOR);
				terrCd = parts[parts.length-2];
				busnDt = parts[parts.length-1].substring(0,4) + "-" + parts[parts.length-1].substring(4,6) + "-" + parts[parts.length-1].substring(6,8);
				
				key = terrCd + "|" + busnDt;
						
				if ( terrDateMap.containsKey(key) ) {
					terrDateMap.put(key, (int)terrDateMap.get(key)+1);
				} else {
					terrDateMap.put(key, 1);
				}
				
			}
			
			for ( Map.Entry<String, Integer> entry : terrDateMap.entrySet()) {
				uniqueKeys.add(entry.getKey());
			}
			
		} catch (Exception ex) { 
			System.err.println("Exception occred getting Daily Sales from Teradata");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
	}
	
	private void getSalesData(DaaSConfig daasConfig
			                 ,File salesFile) {
		
		String[] parts;
		String terrCd;
		String busnDt = "";
		short ctryIsoNu = 0;
		String lcat = "";
		BigDecimal netSalesAm = null;
		BigDecimal grossSalesAm = null;
		int tcQty = 0;
		int whereCondCnt = 0;
		StringBuffer innerWhereCond = new StringBuffer();
		StringBuffer outerWhereCond = new StringBuffer();
		ResultSet rset=null;
		BufferedReader reader=null;
	    String lineIn=null;
	    boolean firstLine = true;
	    boolean moreFl = true;
	    boolean skipLine = false;
		
		try {
			dailySalesFile = new Path(dailySalesOutputPath.toString() + Path.SEPARATOR + "DailySales.psv");
			
			FSDataOutputStream out = fileSystem.create(dailySalesFile);

			if ( daasConfig.displayMsgs() ) {
				if ( salesFile == null ) {
					System.out.print("Extracting Daily Sales from Teradata ... ");
				} else {
					System.out.print("Extracting Daily Sales from " + salesFile.getAbsolutePath() + " ... ");
				}
			}
			
			if ( salesFile == null ) {
				for ( String terrDate : uniqueKeys ) {
					parts = terrDate.split("\\|");
					
					terrCd = parts[0];
					busnDt = parts[1];
					
					if ( whereCondCnt == 0 ) {
						innerWhereCond.append("        where (");
						outerWhereCond.append("    where (");
						
					} else { 
						innerWhereCond.append("        or    (");
						outerWhereCond.append("    or    (");
					}

					innerWhereCond.append("c.CAL_DT = '" + busnDt + "')\n");
					outerWhereCond.append("a.TERR_CD = " + terrCd + " and a.CAL_DT = '" + busnDt + "')\n");
					
					whereCondCnt++;
				}
				
				if ( whereCondCnt == 0 ) {
					throw new IndexOutOfBoundsException("requestedTerrDateParms cannot be empty");
				}
				
				sql.setLength(0);
			    sql.append("select\n");
			    sql.append("   a.CAL_DT\n");
			    sql.append("  ,a.TERR_CD as CTRY_ISO_NU\n");
			    sql.append("  ,a.LGCY_LCL_RFR_DEF_CD\n");
			    sql.append("  ,a.DLY_NET_SLS_AM\n");
			    sql.append("  ,a.DLY_GRSS_SLS_AM\n");
			    sql.append("  ,a.DLY_TRN_CNT_QT\n");
			    sql.append("from (\n");
			    sql.append("	select\n");
			    sql.append("	   a.TERR_CD\n");
			    sql.append("	  ,c.LGCY_LCL_RFR_DEF_CD\n");
			    sql.append("	  ,cast(extract(year from a.CAL_DT) as char(4)) || case when extract(month from a.CAL_DT) < 10 then '0' else '' end || cast(extract(month from a.CAL_DT) as varchar(2)) || case when extract(day from a.CAL_DT) < 10 then '0' else '' end || cast(extract(day from a.CAL_DT) as varchar(2)) as CAL_DT\n");
			    sql.append("	  ,cast(a.DLY_NET_SLS_AM as decimal(18,2)) as DLY_NET_SLS_AM\n");
			    sql.append("	  ,cast(coalesce(a.DLY_GRSS_SLS_AM,0) as decimal(18,2)) as DLY_GRSS_SLS_AM\n");
			    sql.append("	  ,a.DLY_TRN_CNT_QT\n");
			    sql.append("	  ,coalesce(d.REST_OWSH_TYP_SHRT_DS,'X') as REST_OWSH_TYP_SHRT_DS\n");
			    sql.append("	from {VDB}.V1DLY_SLS a\n");
			    sql.append("	inner join (\n");
			    sql.append("	        select\n");
			    sql.append("	           SALE_TYP_ID_NU\n");
			    sql.append("	        from {VDB}.V1SALE_TYP\n");
			    sql.append("	        where SALE_TYP_DS = 'Total') b\n");
			    sql.append("	  on (b.SALE_TYP_ID_NU = a.SALE_TYP_ID_NU)\n");
			    sql.append("	inner join {VDB}. V1GBAL_LCAT c\n");
			    sql.append("	  on (c.MCD_GBAL_LCAT_ID_NU = a.MCD_GBAL_LCAT_ID_NU)\n");
			    sql.append("	left outer join (\n");  
			    sql.append("		select\n");
			    sql.append("		   a.MCD_GBAL_LCAT_ID_NU\n");
			    sql.append("		  ,c.CAL_DT\n");
			    sql.append("		  ,b.REST_OWSH_TYP_SHRT_DS\n");
			    sql.append("		from {VDB}.V1REST_OWSH a\n");
			    sql.append("		inner join {VDB}. V1REST_OWSH_TYP b\n");
			    sql.append("		  on (b.REST_OWSH_TYP_ID = a.REST_OWSH_TYP_ID)\n");
			    sql.append("		inner join {VDB}.V1CAL_DT c\n");
			    sql.append("		  on (c.CAL_DT between a.REST_OWSH_EFF_DT and coalesce(a.REST_OWSH_END_DT,cast('9999-12-31' as date)))\n");
			    sql.append(innerWhereCond.toString());
			    sql.append("	   ) d\n");
			    sql.append("	  on (d.MCD_GBAL_LCAT_ID_NU = a.MCD_GBAL_LCAT_ID_NU\n");
			    sql.append("	      and d.CAL_DT = a.CAL_DT)\n");
			    sql.append(outerWhereCond.toString());
			    sql.append("   ) a\n");
			    
			    if ( daasConfig.fileApplyCompanyOwnedFilterTerrList().length() > 0 ) {
				    sql.append("where (a.TERR_CD in (" + daasConfig.fileApplyCompanyOwnedFilterTerrList() + ") and a.REST_OWSH_TYP_SHRT_DS = 'M')\n");
				    sql.append("or    (a.TERR_CD not in (" + daasConfig.fileApplyCompanyOwnedFilterTerrList() + "))\n");		
			    }
				
				if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
					System.out.println(sql.toString().replaceAll("\\{VDB\\}",daasConfig.gblViewDb()));
				}
				
				rset = gdw.resultSet(sql.toString().replaceAll("\\{VDB\\}",daasConfig.gblViewDb()));
			    moreFl = rset.next();
			} else {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(salesFile),"UTF8"));
				if ( (lineIn = reader.readLine()) == null) {
					moreFl=false;
				}
			}
			
			while ( moreFl ) {

				if ( salesFile == null ) {
					busnDt = rset.getString("CAL_DT");
					ctryIsoNu = rset.getShort("CTRY_ISO_NU");
					lcat = rset.getString("LGCY_LCL_RFR_DEF_CD");
					netSalesAm = rset.getBigDecimal("DLY_NET_SLS_AM");
					grossSalesAm = rset.getBigDecimal("DLY_GRSS_SLS_AM");
					tcQty = rset.getInt("DLY_TRN_CNT_QT");
				} else {
		    		if ( firstLine ) {
		    			firstLine = false;
		    			if ( (int)lineIn.charAt(0) == UTF8_BOM ) {
		    				lineIn = lineIn.substring(1);
		    			}
		    		}
		    		
		    		parts = lineIn.split("\\t");
		    		
		    		if ( parts.length >= 6 ) {
		    			busnDt = parts[0];
		    			try {
			    			ctryIsoNu = Short.parseShort(parts[1]);
		    			} catch (Exception ex) {
		    				skipLine = true;
		    			}
		    			lcat = parts[2];
		    			try {
		    				netSalesAm = new BigDecimal(parts[3]);
		    			} catch (Exception ex) {
		    				skipLine = true;
		    			}
		    			try {
		    				grossSalesAm = new BigDecimal(parts[4]);
		    			} catch (Exception ex) {
		    				skipLine = true;
		    			}
		    			try {
		    				tcQty = Integer.parseInt(parts[5]);
		    			} catch (Exception ex) {
		    				skipLine = true;
		    			}
		    		} else {
		    			skipLine = true;
		    		}
				}

		    	if ( skipLine ) {
		    		skipLine = false;
		    		System.err.println("  ==>>Skipping invalid input line: '" + lineIn + "'<<==");
		    	} else {
		    		out.writeBytes(busnDt + "|" + ctryIsoNu + "|" + lcat + "|" + netSalesAm.toString() + "|" + grossSalesAm.toString() + "|" + tcQty + "\n");
		    	}

				if ( salesFile == null ) {
					moreFl = rset.next();
				} else {
					if ( (lineIn = reader.readLine()) == null) {
						moreFl=false;
					}
				}
			}
			
			out.close();

			if ( salesFile == null ) {
		    	rset.close();
		    } else {
		    	reader.close();
		    }

			if ( daasConfig.displayMsgs()) {
				System.out.println("done");
			}
			
		} catch (Exception ex) { 
			if ( salesFile == null ) {
				System.err.println("Exception occred getting Daily Sales from Teradata");
			} else {
				System.err.println("Exception occred getting Daily Sales from " + salesFile.getAbsolutePath());
			}
			ex.printStackTrace(System.err);
			System.exit(8);
		}
	}

	
	private void getLcatDataFromABaC(DaaSConfig daasConfig) {
		
		ResultSet rset;
		
		try {
			if ( daasConfig.displayMsgs()) {
				System.out.print("Extracting Country List from ABaC ... ");
			}

			sql.setLength(0);
		    sql.append("select\n");
		    sql.append("   CTRY_ISO_NU\n");
		    sql.append("  ,CTRY_SHRT_NA\n");
		    sql.append("from " + daasConfig.abacSqlServerDb() + ".ctry with (NOLOCK)\n");
		    sql.append("order by\n");
		    sql.append("   CTRY_ISO_NU\n");

		    if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
		    	System.out.println(sql.toString());
		    }
		    
			rset = abac.resultSet(sql.toString());
			
			while ( rset.next() ) {
				terrMap.put(Integer.toString(rset.getInt("CTRY_ISO_NU")), rset.getString("CTRY_SHRT_NA"));
			}
			
			rset.close();

			if ( daasConfig.displayMsgs() ) {
				System.out.println("done");
				System.out.print("Extracting Location List from ABaC ... ");
			}
			
		    sql.setLength(0);
		    sql.append("select\n");
		    sql.append("   cast(a.CTRY_ISO_NU as varchar(3)) + '_' + a.LGCY_LCL_RFR_DEF_CD as LCAT_KEY\n");
		    sql.append("  ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    sql.append("from " + daasConfig.abacSqlServerDb() + ".gbal_lcat a with (NOLOCK)\n");
		    sql.append("order by 1\n");

		    if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
		    	System.out.println(sql.toString());
		    }
		    
		    rset = abac.resultSet(sql.toString());
		    
			while ( rset.next() ) {
				lcatMap.put(rset.getString("LCAT_KEY"), rset.getString("MCD_GBAL_BUSN_LCAT_NA"));
			}
			
			rset.close();

			if ( daasConfig.displayMsgs() ) {
				System.out.println("done");
			}
			
		} catch (Exception ex) { 
			System.err.println("Exception occred getting Country/Locaiton from ABaC");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
	}
	
	private void createCorrectnessReport(ReportFormat rptFormat
			                            ,String fileNameAndPath
			                            ,DaaSConfig daasConfig) {
		
		try {
			DataQualityReport rpt = new DataQualityReport(rptFormat,ReportType.Correctness,fileNameAndPath,daasConfig.getLocale());
			
			DataQualityCorrectnessLine dataLine = rpt.new DataQualityCorrectnessLine();

			FileStatus[] status = null;
			status = fileSystem.listStatus(xmlSalesOutputPath);

			String line;
			String[] parts;
			String terrCd = "";
			String terrNa = "";
			String lcat = "";
			String lcatNa = "";

			if ( status != null ) {
				for (int idx=0; idx < status.length; idx++ ) {
					BufferedReader br=new BufferedReader(new InputStreamReader(fileSystem.open(status[idx].getPath())));

					while ((line=br.readLine()) != null){
						parts = line.split("\\|");
						
						if ( !parts[TERR_CD_POS].equals(terrCd) ) {
							terrCd = parts[TERR_CD_POS];
							terrNa = terrMap.get(terrCd);
							if ( terrNa == null ) {
								terrNa = "";
							}
 						}
						
						lcat = parts[LCAT_POS];
						lcatNa = lcatMap.get(terrCd + "_" + lcat);
						
						if ( lcatNa == null ) {
							lcatNa = "";
						}

						dataLine.busnDt             = parts[BUSN_DT_POS];
						dataLine.terrCd             = terrCd;
						dataLine.terrNa             = terrNa;
						dataLine.lcat               = lcat;
					    dataLine.lcatNa             = lcatNa;
						dataLine.dailySalesNetAmt   = Double.parseDouble(parts[DAILY_SLS_NET_AMT_POS]);
						dataLine.dailySalesGrossAmt = Double.parseDouble(parts[DAILY_SLS_GROSS_AMT_POS]);
						dataLine.netGrossFl         = parts[NET_GROSS_FL_POS];
						dataLine.dailySalesTc       = Integer.parseInt(parts[DAILY_SLS_TC_POS]);
						dataLine.xmlSalesAmt        = Double.parseDouble(parts[XML_AMT_POS]);
						dataLine.xmlTc              = Integer.parseInt(parts[XML_TC_POS]);
						
						rpt.addLine(dataLine);
						
					}
					
					br.close();
					
				}
			}
			
			rpt.save();

			if ( summaryNameAndPath2.length() > 0 ) {
	 			BufferedWriter summaryOut = new BufferedWriter(new FileWriter(summaryNameAndPath2));
				summaryOut.write(rpt.summaryReport());
				summaryOut.close();
			}
			
		} catch (Exception ex) { 
			System.err.println("Exception occred getting output data");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
		
	}

	private void connectSQL(DaaSConfig daasConfig
			               ,boolean includeGdwFl) {

		try {
			if ( includeGdwFl) {
			    if ( daasConfig.displayMsgs() ) {
			    	System.out.print("Connecting to GDW ... ");
			    }

			    gdw = new RDBMS(RDBMS.ConnectionType.Teradata,daasConfig.gblTpid(),daasConfig.gblUserId(),daasConfig.gblPassword(),daasConfig.gblNumSessions());
			    
			    if ( daasConfig.displayMsgs() ) {
			    	System.out.println("done");
			    }
			}

			if ( daasConfig.displayMsgs() ) {
		    	System.out.print("Connecting to ABaC ... ");
		    }

			abac = new RDBMS(RDBMS.ConnectionType.SQLServer,daasConfig.abacSqlServerServerName(),daasConfig.abacSqlServerUserId(),daasConfig.abacSqlServerPassword());
			abac.setBatchSize(daasConfig.abacSqlServerBatchSize());
		    abac.setAutoCommit(false);
		    
		    if ( daasConfig.displayMsgs() ) {
		    	System.out.println("done");
		    }
			
		} catch (RDBMS.DBException ex ) {
			System.err.println("RDBMS Exception occured connecting to database:");
			ex.printStackTrace(System.err);
			System.exit(8);
			
		} catch (Exception ex ) {
			System.err.println("Exception occured connecting to database:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
		
	}
	
	private void createCompletenessReport(ReportFormat fmt
			                             ,String outputNameAndPath
			                             ,DaaSConfig daasConfig
			                             ,String owshFltr) {
		
		ResultSet rset;
	    StringBuffer sql1 = new StringBuffer();
	    StringBuffer sql2 = new StringBuffer();
	    String[] parts;
	    String terrCd = "";
	    String busnDt = "";
	    String terrCdList = "";
	    String busnDtList = "";
	    String cond = "";
		
		try {
			for ( String terrDate : uniqueKeys ) {
				parts = terrDate.split("\\|");
				
				terrCd = parts[0];
				busnDt = parts[1];
				
				if ( !terrCdList.contains(terrCd) ) {
					if ( terrCdList.length() > 0 ) {
						terrCdList += ",";
					}
					terrCdList += terrCd;
				}
				
				if ( !busnDtList.contains(busnDt) ) {
					if ( busnDtList.length() > 0 ) {
						busnDtList += ",";
					}
					busnDtList += "'" + busnDt + "'";
				}
			}

			if ( daasConfig.displayMsgs() ) {
				System.out.print("Creating Data Quality Completeness Summary Report ... ");
			}
						
			sql.setLength(0);
		    sql.append("select\n"); 
		    sql.append("   REST_OWSH_TYP_SHRT_DS\n");
		    sql.append("  ,REST_OWSH_TYP_DS\n");
		    sql.append("  ,REST_OWSH_SUB_TYP_SHRT_DS\n");
		    sql.append("  ,REST_OWSH_SUB_TYP_DS\n");
		    sql.append(" from " + daasConfig.abacSqlServerDb() + ".rest_owsh_typ with (NOLOCK)\n");
		    
		    if (owshFltr.equals("M") || owshFltr.equals("L")) {
		    	sql.append(" where REST_OWSH_TYP_SHRT_DS = '" + owshFltr + "'\n");		    	
		    }
		    
		    sql.append(" order by\n");
		    sql.append("  REST_OWSH_TYP_SHRT_DS desc\n");
		    sql.append(" ,REST_OWSH_SUB_TYP_SHRT_DS\n");
		    
		    HashMap<String,Integer> owshMap = new HashMap<String,Integer>();
		    ArrayList<String> owshTypes = new ArrayList<String>();
		    
		    if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
		    	System.out.println(sql.toString());
		    }
		    
		    rset = abac.resultSet(sql.toString());
		    
		    int colNum = 1;
		    		
		    while ( rset.next() ) {
		    	owshMap.put(rset.getString("REST_OWSH_TYP_SHRT_DS").trim() + rset.getString("REST_OWSH_SUB_TYP_SHRT_DS").trim(),colNum);
		    	colNum += 5;
		    	owshTypes.add(rset.getString("REST_OWSH_TYP_DS") + "|" + rset.getString("REST_OWSH_SUB_TYP_DS"));
		    }
		    
		    rset.close();
		    
		    sql.setLength(0);
		    sql.append("select count(*) as LCAT_COUNT from " + daasConfig.abacSqlServerDb() + ".gbal_lcat with (NOLOCK)");
		    
		    if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
		    	System.out.println(sql.toString());
		    }
		    
		    rset = abac.resultSet(sql.toString());
		    
		    rset.next();
		    
		    boolean lcatCountZero = false;
		    
		    if ( rset.getInt("LCAT_COUNT") == 0 ) {
		    	lcatCountZero = true;
		    }
		    
		    rset.close();

		    if ( lcatCountZero ) {
		    	sql2.append("          select\n");
		    	sql2.append("             a.CTRY_ISO_NU\n");
		    	sql2.append("            ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql2.append("            ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    	sql2.append("            ,a.CAL_DT\n");
		    	sql2.append("            ,a.REST_OWSH_TYP_SHRT_DS\n");
		    	sql2.append("            ,a.REST_OWSH_SUB_TYP_SHRT_DS\n");
		    	sql2.append("          from (\n");
		    	sql2.append("               select\n");
		    	sql2.append("                  a.CTRY_ISO_NU\n");
		    	sql2.append("                 ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql2.append("                 ,CAST('UNKNOWN' as nvarchar(80)) as MCD_GBAL_BUSN_LCAT_NA\n");
		    	sql2.append("                 ,b.CAL_DT\n");
		    	sql2.append("                 ,cast('~' as char(1)) as REST_OWSH_TYP_SHRT_DS\n");
		    	sql2.append("                 ,cast('~' as char(3)) as REST_OWSH_SUB_TYP_SHRT_DS\n");
		    	sql2.append("               from (\n");
		    	sql2.append("                    select distinct\n");
		    	sql2.append("                       a.TERR_CD as CTRY_ISO_NU\n");
		    	sql2.append("                      ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql2.append("                    from " + daasConfig.abacSqlServerDb() + ".dw_file a with (NOLOCK)\n");
		    	sql2.append("                    inner join " + daasConfig.abacSqlServerDb() + ".dw_data_typ b with (NOLOCK)\n");
		    	sql2.append("                      on (b.DW_DATA_TYP_ID = a.DW_DATA_TYP_ID)\n");
		    	sql2.append("                    where a.TERR_CD in (" + terrCdList + ")\n");
		    	sql2.append("                    and   b.DW_DATA_TYP_DS = '" + daasConfig.fileType() + "') a\n");
		    	sql2.append("               ,(\n");
		    	sql2.append("                    select\n");
		    	sql2.append("                       a.CAL_DT\n");
		    	sql2.append("                    from " + daasConfig.abacSqlServerDb() + ".cal_dt a with (NOLOCK)\n");
		    	sql2.append("                    where a.CAL_DT in (" + busnDtList + ") ) b) a\n");

		    	cond = "where ";
		    	
		    	for ( String terrDate : uniqueKeys ) {
					parts = terrDate.split("\\|");
					
					terrCd = parts[0];
					busnDt = parts[1];
				
			    	sql2.append("          " + cond + "(a.CTRY_ISO_NU = " + terrCd + " and a.CAL_DT = '" + busnDt + "')\n" );
			    	
			    	cond = "or    ";
		    	}
		    	sql2.append(") a\n");
		    	
		    } else {
		    	sql2.append("          select\n");
		    	sql2.append("             a.CTRY_ISO_NU\n");
		    	sql2.append("            ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql2.append("            ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    	sql2.append("            ,b.CAL_DT\n");
		    	sql2.append("            ,cast(coalesce(c.REST_OWSH_TYP_SHRT_DS,'~') as char(1)) as REST_OWSH_TYP_SHRT_DS\n");
		    	sql2.append("            ,cast(coalesce(c.REST_OWSH_SUB_TYP_SHRT_DS,'~') as char(3)) as REST_OWSH_SUB_TYP_SHRT_DS\n");
		    	sql2.append("          from " + daasConfig.abacSqlServerDb() + ".gbal_lcat a with (NOLOCK)\n");
		    	sql2.append("          inner join " + daasConfig.abacSqlServerDb() + ".CAL_DT b\n");
		    	sql2.append("            on (b.CAL_DT between a.STR_OPEN_DT and a.STR_CLSE_DT)\n");
		    	sql2.append("          left outer join " + daasConfig.abacSqlServerDb() + ".rest_owsh c with (NOLOCK)\n");
		    	sql2.append("            on (c.CTRY_ISO_NU = a.CTRY_ISO_NU\n");
		    	sql2.append("                and c.LGCY_LCL_RFR_DEF_CD = a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql2.append("                and b.CAL_DT between c.REST_OWSH_EFF_DT and c.REST_OWSH_END_DT)\n");

		    	cond = "where ";
		    	
		    	for ( String terrDate : uniqueKeys ) {
					parts = terrDate.split("\\|");
					
					terrCd = parts[0];
					busnDt = parts[1];
				
			    	sql2.append("          " + cond + "(a.CTRY_ISO_NU = " + terrCd + " and b.CAL_DT = '" + busnDt + "')\n" );
			    	
			    	if (owshFltr.equals("M")) {
			    		sql2.append(" and (c.REST_OWSH_TYP_SHRT_DS = '" + owshFltr + "' OR c.REST_OWSH_TYP_SHRT_DS IS NULL)\n");
			    	} else if (owshFltr.equals("L")) {
			    		sql2.append(" and c.REST_OWSH_TYP_SHRT_DS = '" + owshFltr + "'\n");
			    	}
			    	
			    	cond = "or    ";
		    	}

		    	sql2.append(") a\n");
		    }

		    sql1.setLength(0);
		    sql1.append("select\n");
		    sql1.append("   a.CTRY_ISO_NU\n");
		    sql1.append("  ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("  ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    sql1.append("  ,a.CAL_DT\n");
		    sql1.append("  ,a.REST_OWSH_TYP_SHRT_DS\n");
		    sql1.append("  ,coalesce(d.REST_OWSH_TYP_DS,'UNKNOWN') as REST_OWSH_TYP_DS\n");
		    sql1.append("  ,a.REST_OWSH_SUB_TYP_SHRT_DS\n");
		    sql1.append("  ,coalesce(d.REST_OWSH_SUB_TYP_DS,'UNKNOWN') as REST_OWSH_SUB_TYP_DS\n");
		    sql1.append("  ,1 as EXPECTED_FILE_COUNT\n");
		    sql1.append("  ,case when b.LGCY_LCL_RFR_DEF_CD is null then 0 else 1 end as FILE_COUNT\n");
		    sql1.append("  ,case when b.LGCY_LCL_RFR_DEF_CD is null then 0 else case when a.SUB_EXPECT_FILE_COUNT <> coalesce(c.SUB_FILE_ACT_COUNT,0) then 1 else 0 end end as FILE_INCOMPLETE_COUNT\n");
		    sql1.append("from (\n");
		    sql1.append("     select\n"); 
		    sql1.append("        a.CTRY_ISO_NU\n");
		    sql1.append("       ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("       ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    sql1.append("       ,a.CAL_DT\n");
		    sql1.append("       ,a.REST_OWSH_TYP_SHRT_DS\n");
		    sql1.append("       ,a.REST_OWSH_SUB_TYP_SHRT_DS\n");
		    sql1.append("       ,b.SUB_EXPECT_FILE_COUNT\n");
		    sql1.append("     from (\n");
		    sql1.append(sql2);
		    sql1.append("     ,(\n");
		    sql1.append("          select\n");
		    sql1.append("             count(*) as SUB_EXPECT_FILE_COUNT\n");
		    sql1.append("          from " + daasConfig.abacSqlServerDb() + ".dw_data_typ a with (NOLOCK)\n");
		    sql1.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_sub_file_data_typ b with (NOLOCK)\n");
		    sql1.append("            on (b.DW_DATA_TYP_ID = a.DW_DATA_TYP_ID)\n");
		    sql1.append("          where a.DW_DATA_TYP_DS = '" + daasConfig.fileType() + "') b\n");
		    sql1.append("     ) a\n");
		    sql1.append("left outer join (\n");
		    sql1.append("     select\n"); 
		    sql1.append("        a.TERR_CD\n");
		    sql1.append("       ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("       ,a.CAL_DT\n");
		    sql1.append("       ,a.DW_FILE_ID\n");
		    sql1.append("     from (\n");  
		    sql1.append("          select\n"); 
		    sql1.append("             a.TERR_CD\n");
		    sql1.append("            ,cast(case when a.TERR_CD = 840 then\n");
		    sql1.append("                  case len(a.LGCY_LCL_RFR_DEF_CD) when 1 then '0000'\n");
		    sql1.append("                                                  when 2 then '000'\n");
		    sql1.append("                                                  when 3 then '00'\n");
		    sql1.append("                                                  when 4 then '0'\n");
		    sql1.append("                                                  else '' end + a.LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("            else a.LGCY_LCL_RFR_DEF_CD end as varchar(12)) as LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("            ,a.CAL_DT\n");
		    sql1.append("            ,a.DW_FILE_ID\n");
		    sql1.append("            ,ROW_NUMBER() over(partition by a.TERR_CD, a.LGCY_LCL_RFR_DEF_CD, a.CAL_DT order by a.FILE_MKT_OGIN_TS desc, a.FILE_DW_ARRV_TS desc, a.DW_FILE_ID) as ROW_NUM\n");
		    sql1.append("          from " + daasConfig.abacSqlServerDb() + ".dw_file a with (NOLOCK)\n");
		    sql1.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_data_typ b with (NOLOCK)\n");
		    sql1.append("            on (b.DW_DATA_TYP_ID = a.DW_DATA_TYP_ID)\n");
		    sql1.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_audt_stus_typ c with (NOLOCK)\n");
		    sql1.append("       on (c.DW_AUDT_STUS_TYP_ID = a.DW_AUDT_STUS_TYP_ID)\n");

	    	cond = "          where (";
	    	
	    	for ( String terrDate : uniqueKeys ) {
				parts = terrDate.split("\\|");
				
				terrCd = parts[0];
				busnDt = parts[1];
			
		    	sql1.append("          " + cond + "(a.TERR_CD = " + terrCd + " and a.CAL_DT = '" + busnDt + "')\n" );
		    	
		    	cond = "          or    ";
	    	}

	    	sql1.append(")\n");
		    
		    sql1.append("          and   b.DW_DATA_TYP_DS = '" + daasConfig.fileType() + "'\n");
		    sql1.append("          and   c.DW_AUDT_STUS_TYP_DS = 'SUCCESSFUL') a\n");
		    sql1.append("     where a.ROW_NUM = 1) b\n");
		    sql1.append("  on (b.TERR_CD = a.CTRY_ISO_NU\n");
		    sql1.append("      and b.LGCY_LCL_RFR_DEF_CD = a.LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("      and b.CAL_DT = a.CAL_DT)\n");
		    sql1.append("left outer join (\n");
		    sql1.append("     select\n"); 
		    sql1.append("        c.TERR_CD\n");
		    sql1.append("       ,c.LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("       ,c.CAL_DT\n");
		    sql1.append("       ,a.DW_FILE_ID\n");
		    sql1.append("       ,count(*) as SUB_FILE_ACT_COUNT\n");
		    sql1.append("     from " + daasConfig.abacSqlServerDb() + ".dw_sub_file a with (NOLOCK)\n");
		    sql1.append("     inner join " + daasConfig.abacSqlServerDb() + ".dw_audt_stus_typ b with (NOLOCK)\n");
		    sql1.append("       on (b.DW_AUDT_STUS_TYP_ID = a.DW_AUDT_STUS_TYP_ID)\n");
		    sql1.append("     inner join " + daasConfig.abacSqlServerDb() + ".dw_file c with (NOLOCK)\n");
		    sql1.append("       on (c.DW_FILE_ID = a.DW_FILE_ID)\n");
		    sql1.append("     inner join " + daasConfig.abacSqlServerDb() + ".dw_data_typ d with (NOLOCK)\n");
		    sql1.append("       on (d.DW_DATA_TYP_ID = c.DW_DATA_TYP_ID)\n");  
		    sql1.append("     where b.DW_AUDT_STUS_TYP_DS = 'SUCCESSFUL'\n");
		    sql1.append("     and   c.CAL_DT in (" + busnDtList + ")\n");
		    sql1.append("     and   d.DW_DATA_TYP_DS      = '" + daasConfig.fileType() + "'\n");
		    sql1.append("     group by\n"); 
		    sql1.append("        c.TERR_CD\n");
		    sql1.append("       ,c.LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("       ,c.CAL_DT\n");
		    sql1.append("       ,a.DW_FILE_ID) c\n");
		    sql1.append("  on (c.TERR_CD = b.TERR_CD\n");
		    sql1.append("      and c.LGCY_LCL_RFR_DEF_CD = b.LGCY_LCL_RFR_DEF_CD\n");
		    sql1.append("      and c.CAL_DT = b.CAL_DT\n");
		    sql1.append("      and c.DW_FILE_ID = b.DW_FILE_ID)\n");
		    sql1.append("left outer join " + daasConfig.abacSqlServerDb() + ".rest_owsh_typ d with (NOLOCK)\n");
		    sql1.append("  on (d.REST_OWSH_TYP_SHRT_DS = a.REST_OWSH_TYP_SHRT_DS\n");
		    sql1.append("      and d.REST_OWSH_SUB_TYP_SHRT_DS = a.REST_OWSH_SUB_TYP_SHRT_DS)\n");
		    
		    sql.setLength(0);
		    sql.append("select\n");
		    sql.append("   a.CTRY_ISO_NU\n");
		    sql.append("  ,b.CTRY_SHRT_NA\n");
		    sql.append("  ,a.CAL_DT\n");
		    sql.append("  ,a.REST_OWSH_TYP_SHRT_DS\n");
		    sql.append("  ,a.REST_OWSH_TYP_DS\n");
		    sql.append("  ,a.REST_OWSH_SUB_TYP_SHRT_DS\n");
		    sql.append("  ,a.REST_OWSH_SUB_TYP_DS\n");
		    sql.append("  ,SUM(a.EXPECTED_FILE_COUNT) as EXPECTED_FILE_COUNT\n");
		    sql.append("  ,SUM(a.FILE_COUNT) as FILE_COUNT\n");
		    sql.append("  ,SUM(a.FILE_INCOMPLETE_COUNT) as FILE_INCOMPLETE_COUNT\n");
		    sql.append("from (\n");
		    sql.append(sql1);
		    sql.append(")a\n");
		    sql.append("inner join " + daasConfig.abacSqlServerDb() + ".ctry b with (NOLOCK)\n");
		    sql.append("  on (b.CTRY_ISO_NU = a.CTRY_ISO_NU)\n");
		    sql.append("group by\n");
		    sql.append("   a.CTRY_ISO_NU\n");
		    sql.append("  ,b.CTRY_SHRT_NA\n");
		    sql.append("  ,a.CAL_DT\n");
		    sql.append("  ,a.REST_OWSH_TYP_SHRT_DS\n");
		    sql.append("  ,a.REST_OWSH_TYP_DS\n");
		    sql.append("  ,a.REST_OWSH_SUB_TYP_SHRT_DS\n");
		    sql.append("  ,a.REST_OWSH_SUB_TYP_DS\n");
		    sql.append("order by\n");
		    sql.append("   a.CTRY_ISO_NU\n");
		    sql.append("  ,a.CAL_DT\n");
		    sql.append("  ,a.REST_OWSH_TYP_SHRT_DS\n");
		    sql.append("  ,a.REST_OWSH_SUB_TYP_SHRT_DS\n");
		    
		    if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
		    	System.out.println(sql.toString());
		    }
		    
		    rset = abac.resultSet(sql.toString());
		    
			DataQualityReport rpt = new DataQualityReport(fmt,ReportType.Completeness,outputNameAndPath,daasConfig.getLocale());
			
			DataQualityCompletenessSummaryLine summaryLine = rpt.new DataQualityCompletenessSummaryLine();
			DataQualityCompletenessDetailLine detailLine = rpt.new DataQualityCompletenessDetailLine();
			
			summaryLine.owshTypes = owshTypes;

			String key; 
			
		    while ( rset.next() ) {
		    	
		    	summaryLine.busnDt = rset.getDate("CAL_DT").toString();
		    	summaryLine.terrCd = Short.toString(rset.getShort("CTRY_ISO_NU"));
		    	summaryLine.terrNa = rset.getString("CTRY_SHRT_NA");
		    	summaryLine.expectedCount = rset.getInt("EXPECTED_FILE_COUNT");
		    	summaryLine.fileCount = rset.getInt("FILE_COUNT");
		    	summaryLine.incompleteFileCount = rset.getInt("FILE_INCOMPLETE_COUNT");
		    
		    	key = rset.getString("REST_OWSH_TYP_SHRT_DS").trim() + rset.getString("REST_OWSH_SUB_TYP_SHRT_DS").trim();
		    	
		    	if ( owshMap.containsKey(key) ) {
		    		summaryLine.startColumn = owshMap.get(key);
		    		rpt.addLine(summaryLine);
		    	}
		    }

		    rset.close();

			if ( daasConfig.displayMsgs() ) {
				System.out.println("done");
				System.out.print("Creating Data Quality Completeness Detail Report ... ");
			}

		    sql.setLength(0);
		    sql.append("select\n");
		    sql.append("   a.DW_SUB_FILE_DATA_TYP_DS\n");
		    sql.append("from " + daasConfig.abacSqlServerDb() + ".dw_sub_file_data_typ a with (NOLOCK)\n");
		    sql.append("inner join " + daasConfig.abacSqlServerDb() + ".dw_data_typ b with (NOLOCK)\n");
		    sql.append("  on (b.DW_DATA_TYP_ID = a.DW_DATA_TYP_ID)\n");
		    sql.append("where b.DW_DATA_TYP_DS = '" + daasConfig.fileType() + "'\n");
		    sql.append("order by\n");
		    sql.append("   a.DW_SUB_FILE_DATA_TYP_DS\n");
		    
		    ArrayList<String> subTypes = new ArrayList<String>();

		    if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
		    	System.out.println(sql.toString());
		    }
		    
		    rset = abac.resultSet(sql.toString());
		   
		    while ( rset.next() ) {
		    	subTypes.add(rset.getString("DW_SUB_FILE_DATA_TYP_DS"));
		    }
		    
		    detailLine.subTypes = subTypes;
		    
		    sql.setLength(0);
		    sql.append("select\n");
		    sql.append("   a.CTRY_ISO_NU\n");
		    sql.append("  ,a1.CTRY_SHRT_NA\n");
		    sql.append("  ,a.CAL_DT\n");
		    sql.append("  ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql.append("  ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    sql.append("  ,coalesce(b.DW_SUB_FILE_DATA_TYP_DS,'~') as DW_SUB_FILE_DATA_TYP_DS\n");
		    sql.append("  ,coalesce(b.SUB_FILE_STUS_DS,'') as SUB_FILE_STUS_DS\n");
		    sql.append("  ,case when b.DW_SUB_FILE_DATA_TYP_DS is null and c.FILE_REJECT_RESN_DS is null then 'NOT SENT' else coalesce(c.FILE_REJECT_RESN_DS,'') end as FILE_REJECT_RESN_DS\n");
		    sql.append("from (\n");
		    
		    if ( lcatCountZero ) {
		    	sql.append("     select\n");
		    	sql.append("        a.CAL_DT\n");
		    	sql.append("       ,a.CTRY_ISO_NU\n");
		    	sql.append("       ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql.append("       ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    	sql.append("     from (\n");
		    	
		    	sql.append("          select\n");
		    	sql.append("             b.CAL_DT\n");
		    	sql.append("            ,a.CTRY_ISO_NU\n");
		    	sql.append("            ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql.append("            ,CAST('UNKNOWN' as nvarchar(80)) as MCD_GBAL_BUSN_LCAT_NA\n");
		    	sql.append("          from (\n");
		    	sql.append("               select distinct\n");
		    	sql.append("                  a.TERR_CD as CTRY_ISO_NU\n");
		    	sql.append("                 ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql.append("               from " + daasConfig.abacSqlServerDb() + ".dw_file a with (NOLOCK)\n");
		    	sql.append("               inner join " + daasConfig.abacSqlServerDb() + ".dw_data_typ b with (NOLOCK)\n");
		    	sql.append("                 on (b.DW_DATA_TYP_ID = a.DW_DATA_TYP_ID)\n");
		    	sql.append("               where a.TERR_CD in (" + terrCdList + ")\n");
		    	sql.append("               and   b.DW_DATA_TYP_DS = '" + daasConfig.fileType() + "') a\n");
		    	sql.append("          ,(\n");
		    	sql.append("               select\n");
		    	sql.append("                  a.CAL_DT\n");
		    	sql.append("               from " + daasConfig.abacSqlServerDb() + ".cal_dt a with (NOLOCK)\n");
		    	sql.append("               where a.CAL_DT in (" + busnDtList + ") ) b) a\n");

		    	cond = "where ";
		    	
		    	for ( String terrDate : uniqueKeys ) {
					parts = terrDate.split("\\|");
					
					terrCd = parts[0];
					busnDt = parts[1];
				
			    	sql.append("          " + cond + "(a.CTRY_ISO_NU = " + terrCd + " and a.CAL_DT = '" + busnDt + "')\n" );
			    	
			    	cond = "or    ";
		    	}

		    	sql.append(") a\n");
		    
		    } else {
		    	sql.append("     select\n");
		    	sql.append("        a.CAL_DT\n");
		    	sql.append("       ,a.CTRY_ISO_NU\n");
		    	sql.append("       ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql.append("       ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    	sql.append("     from (\n");
		    	sql.append("          select\n");
		    	sql.append("             b.CAL_DT\n");
		    	sql.append("            ,a.CTRY_ISO_NU\n");
		    	sql.append("            ,a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql.append("            ,a.MCD_GBAL_BUSN_LCAT_NA\n");
		    	sql.append("          from " + daasConfig.abacSqlServerDb() + ".gbal_lcat a with (NOLOCK)\n");
		    	sql.append("          inner join " + daasConfig.abacSqlServerDb() + ".CAL_DT b with (NOLOCK)\n");
		    	sql.append("       		on (b.CAL_DT between a.STR_OPEN_DT and a.STR_CLSE_DT) ) a\n");
		    	sql.append("          left outer join " + daasConfig.abacSqlServerDb() + ".rest_owsh c with (NOLOCK)\n");
		    	sql.append("			on (c.CTRY_ISO_NU = a.CTRY_ISO_NU\n");
		    	sql.append("		  and c.LGCY_LCL_RFR_DEF_CD = a.LGCY_LCL_RFR_DEF_CD\n");
		    	sql.append("		  and a.CAL_DT between c.REST_OWSH_EFF_DT and c.REST_OWSH_END_DT)\n");

		    	cond = "where ";
		    	
		    	for ( String terrDate : uniqueKeys ) {
					parts = terrDate.split("\\|");
					
					terrCd = parts[0];
					busnDt = parts[1];
				
			    	sql.append("          " + cond + "(a.CTRY_ISO_NU = " + terrCd + " and a.CAL_DT = '" + busnDt + "')\n" );

			    	if (owshFltr.equals("M")) {
			    		sql.append(" and (c.REST_OWSH_TYP_SHRT_DS = '" + owshFltr + "' OR c.REST_OWSH_TYP_SHRT_DS IS NULL)\n");
			    	} else if (owshFltr.equals("L")) {
			    		sql.append(" and c.REST_OWSH_TYP_SHRT_DS = '" + owshFltr + "'\n");
			    	}
			    	
			    	cond = "or    ";
		    	}
		    	
		    	sql.append(") a\n");
		    }

		    sql.append("inner join " + daasConfig.abacSqlServerDb() + ".ctry a1 with (NOLOCK)\n");
	    	sql.append("  on (a1.CTRY_ISO_NU = a.CTRY_ISO_NU)\n");
		    sql.append("left outer join (\n");
		    sql.append("     select\n");
		    sql.append("        a.CAL_DT\n");
		    sql.append("       ,a.CTRY_ISO_NU\n");
		    sql.append("       ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql.append("       ,b.DW_SUB_FILE_DATA_TYP_DS\n");
		    sql.append("       ,case when d.DW_AUDT_STUS_TYP_DS is null then 'REJECTED, MISSING' else d.DW_AUDT_STUS_TYP_DS + case when f.DW_AUDT_RJCT_RESN_DS is not null then ', ' + coalesce(f.DW_AUDT_RJCT_RESN_DS,'') else '' end end as SUB_FILE_STUS_DS\n");
		    sql.append("     from (\n");
		    sql.append("          select\n"); 
		    sql.append("             a.CAL_DT\n");
		    sql.append("            ,a.CTRY_ISO_NU\n");
		    sql.append("            ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql.append("            ,a.DW_FILE_ID\n");
		    sql.append("            ,a.DW_DATA_TYP_ID\n");
		    sql.append("          from (\n");
		    sql.append("               select\n"); 
		    sql.append("                  a.CAL_DT\n");
		    sql.append("                 ,a.TERR_CD as CTRY_ISO_NU\n");
		    sql.append("                 ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql.append("                 ,a.DW_FILE_ID\n");
		    sql.append("                 ,a.DW_DATA_TYP_ID\n");
		    sql.append("                 ,ROW_NUMBER() over(partition by a.CAL_DT,a.TERR_CD,a.LGCY_LCL_RFR_DEF_CD order by a.FILE_MKT_OGIN_TS desc, a.FILE_DW_ARRV_TS desc) as ROW_NUM\n");
		    sql.append("               from " + daasConfig.abacSqlServerDb() + ".dw_file a with (NOLOCK)\n");
		    sql.append("               inner join " + daasConfig.abacSqlServerDb() + ".dw_data_typ b with (NOLOCK)\n");
		    sql.append("                 on (b.DW_DATA_TYP_ID = a.DW_DATA_TYP_ID)\n");
		    sql.append("               inner join " + daasConfig.abacSqlServerDb() + ".dw_audt_stus_typ c with (NOLOCK)\n");
		    sql.append("                 on (c.DW_AUDT_STUS_TYP_ID = a.DW_AUDT_STUS_TYP_ID)\n");

	    	cond = "where (";
	    	
	    	for ( String terrDate : uniqueKeys ) {
				parts = terrDate.split("\\|");
				
				terrCd = parts[0];
				busnDt = parts[1];
			
		    	sql.append("          " + cond + "(a.TERR_CD = " + terrCd + " and a.CAL_DT = '" + busnDt + "')\n" );
		    	
		    	cond = "or    ";
	    	}

	    	sql.append(")\n");
		    
		    sql.append("               and   b.DW_DATA_TYP_DS = '" + daasConfig.fileType() + "'\n");
		    sql.append("               and   c.DW_AUDT_STUS_TYP_DS = 'SUCCESSFUL') a\n");
		    sql.append("          where a.ROW_NUM = 1) a\n");
		    sql.append("     inner join " + daasConfig.abacSqlServerDb() + ".dw_sub_file_data_typ b with (NOLOCK)\n");
		    sql.append("       on (b.DW_DATA_TYP_ID = a.DW_DATA_TYP_ID)\n");
		    sql.append("     left outer join " + daasConfig.abacSqlServerDb() + ".dw_sub_file c with (NOLOCK)\n");
		    sql.append("       on (c.DW_FILE_ID = a.DW_FILE_ID\n");
		    sql.append("      and c.DW_SUB_FILE_DATA_TYP_ID = b.DW_SUB_FILE_DATA_TYP_ID)\n");
		    sql.append("     left outer join " + daasConfig.abacSqlServerDb() + ".dw_audt_stus_typ d with (NOLOCK)\n");
		    sql.append("       on (d.DW_AUDT_STUS_TYP_ID = c.DW_AUDT_STUS_TYP_ID)\n");
		    sql.append("     left outer join " + daasConfig.abacSqlServerDb() + ".dw_sub_file_rjct_resn_assc e with (NOLOCK)\n");
		    sql.append("       on (e.DW_FILE_ID = a.DW_FILE_ID\n");
		    sql.append("           and e.DW_SUB_FILE_DATA_TYP_ID = b.DW_SUB_FILE_DATA_TYP_ID)\n");
		    sql.append("     left outer join " + daasConfig.abacSqlServerDb() + ".dw_audt_rjct_resn f with (NOLOCK)\n");
		    sql.append("       on (f.DW_AUDT_RJCT_RESN_ID = e.DW_AUDT_RJCT_RESN_ID)) b\n");
		    sql.append("  on (b.CAL_DT = a.CAL_DT\n");
		    sql.append("      and b.CTRY_ISO_NU = a.CTRY_ISO_NU\n");
		    sql.append("      and b.LGCY_LCL_RFR_DEF_CD = a.LGCY_LCL_RFR_DEF_CD)\n");
		    sql.append("left outer join (\n");
		    sql.append("     select\n");
		    sql.append("        a.CAL_DT\n");
		    sql.append("       ,a.CTRY_ISO_NU\n");
		    sql.append("       ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql.append("       ,a.DW_AUDT_STUS_TYP_DS + ', ' + a.DW_AUDT_RJCT_RESN_DS as FILE_REJECT_RESN_DS\n");
		    sql.append("     from (\n");
		    sql.append("          select\n"); 
		    sql.append("             a.CAL_DT\n");
		    sql.append("            ,a.TERR_CD as CTRY_ISO_NU\n");
		    sql.append("            ,a.LGCY_LCL_RFR_DEF_CD\n");
		    sql.append("            ,a.DW_FILE_ID\n");
		    sql.append("            ,c.DW_AUDT_STUS_TYP_DS\n");
		    sql.append("            ,coalesce(e.DW_AUDT_RJCT_RESN_DS,'UNKNOWN') as DW_AUDT_RJCT_RESN_DS\n");
		    sql.append("            ,ROW_NUMBER() over(partition by a.CAL_DT,a.TERR_CD,a.LGCY_LCL_RFR_DEF_CD order by a.FILE_MKT_OGIN_TS desc, a.FILE_DW_ARRV_TS desc) as ROW_NUM\n");
		    sql.append("          from " + daasConfig.abacSqlServerDb() + ".dw_file a with (NOLOCK)\n");
		    sql.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_data_typ b with (NOLOCK)\n");
		    sql.append("            on (b.DW_DATA_TYP_ID = a.DW_DATA_TYP_ID)\n");
		    sql.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_audt_stus_typ c with (NOLOCK)\n");
		    sql.append("            on (c.DW_AUDT_STUS_TYP_ID = a.DW_AUDT_STUS_TYP_ID)\n");
		    sql.append("          left outer join " + daasConfig.abacSqlServerDb() + ".dw_file_rjct_resn_assc d with (NOLOCK)\n");
		    sql.append("            on (d.DW_FILE_ID = a.DW_FILE_ID)\n");
		    sql.append("          left outer join " + daasConfig.abacSqlServerDb() + ".dw_audt_rjct_resn e with (NOLOCK)\n");
		    sql.append("            on (e.DW_AUDT_RJCT_RESN_ID = d.DW_AUDT_RJCT_RESN_ID)\n");

	    	cond = "where (";
	    	
	    	for ( String terrDate : uniqueKeys ) {
				parts = terrDate.split("\\|");
				
				terrCd = parts[0];
				busnDt = parts[1];
			
		    	sql.append("          " + cond + "(a.TERR_CD = " + terrCd + " and a.CAL_DT = '" + busnDt + "')\n" );
		    	
		    	cond = "or    ";
	    	}

	    	sql.append(")\n");
		    
		    sql.append("          and   b.DW_DATA_TYP_DS = '" + daasConfig.fileType() + "'\n");
		    sql.append("          and   c.DW_AUDT_STUS_TYP_DS = 'REJECTED'\n");
		    sql.append("          and   e.DW_AUDT_RJCT_RESN_DS <> 'INTERBATCH DUPLICATE') a\n");
		    sql.append("     where a.ROW_NUM = 1) c\n");
		    sql.append("  on (c.CAL_DT = a.CAL_DT\n");
		    sql.append("      and c.CTRY_ISO_NU = a.CTRY_ISO_NU\n");
		    sql.append("      and c.LGCY_LCL_RFR_DEF_CD = a.LGCY_LCL_RFR_DEF_CD)\n");
		    sql.append("order by\n");   
		    sql.append("   a.CTRY_ISO_NU\n");
		    sql.append("  ,a.CAL_DT\n");
		    sql.append("  ,cast(a.LGCY_LCL_RFR_DEF_CD as decimal(12,0))\n");
		    
		    if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
		    	System.out.println(sql.toString());
		    }
		    
		    rset = abac.resultSet(sql.toString());

		    while ( rset.next() ) {
			    
		    	detailLine.terrCd = Short.toString(rset.getShort("CTRY_ISO_NU"));
		    	detailLine.terrNa = rset.getString("CTRY_SHRT_NA");
		    	detailLine.busnDt = rset.getDate("CAL_DT").toString();
		    	detailLine.lcat = rset.getString("LGCY_LCL_RFR_DEF_CD");
		    	detailLine.lcatNa = rset.getString("MCD_GBAL_BUSN_LCAT_NA");
		    	detailLine.fileRejectReason = rset.getString("FILE_REJECT_RESN_DS");
		    	detailLine.subType = rset.getString("DW_SUB_FILE_DATA_TYP_DS");
		    	detailLine.subTypeRejectReason = rset.getString("SUB_FILE_STUS_DS");
		    	
	    		rpt.addLine(detailLine);
		    	
		    }
		    
		    rpt.save();

			if ( daasConfig.displayMsgs() ) {
				System.out.println("done");
			}
			
		} catch (RDBMS.DBException ex ) {
			System.err.println("RDBMS Exception occured in get data completeness data:");
			ex.printStackTrace(System.err);
			System.exit(8);
			
		} catch (Exception ex ) {
			System.err.println("Exception occured in get data completeness data:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}

	}

}

