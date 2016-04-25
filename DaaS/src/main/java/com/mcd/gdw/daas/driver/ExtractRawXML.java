package com.mcd.gdw.daas.driver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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

import com.mcd.gdw.daas.mapreduce.ExtractRawXMLMapper;
import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;

public class ExtractRawXML extends Configured implements Tool {

	public static final String SEPARATOR_CHARACTER             = "\t";
	public static final String CONFIG_SETTING_FILE_TYPES       = "com.mcd.gdw.daas.filetypes";
	public final static String CONFIG_SETTING_CTRY_ISO2_LOOKUP = "com.mcd.gdw.daas.ctryiso2toterrcd";
	public final static String CONFIG_SETTING_TIMESTAMP        = "com.mcd.gdw.daas.timestamp";
	
	public final static String DIST_CACHE_SELECTION_LIST       = "extract_raw_list.txt";
	
	private static final int MAX_UNIQUE_DAYS    = 10;
	private static final int MAX_UNIQUE_STORES = 100;
	
	private static final String CTRY_ISO2_LOOKUP = "AF4  AL8  AQ10 DZ12 AS16 AD20 AO24 AG28 AZ31 AR32 AU36 AT40 BS44 BH48 BD50 AM51 BB52 BE56 BM60 BT64 BO68 BA70 BW72 BV74 BR76 BZ84 IO86 SB90 VG92 BN96 BG100MM104BI108BY112KH116CM120CA124CV132KY136CF140LK144TD148CL152CN156TW158CX162CC166CO170KM174YT175CG178CD180CK184CR188HR191CU192CY196CZ203BJ204DK208DM212DO214EC218SV222GQ226ET231ER232EE233FO234FK238GS239FJ242FI246FR250GF254PF258TF260DJ262GA266GE268GM270PS275DE276GH288GI292KI296GR300GL304GD308GP312GU316GT320GN324GY328HT332HM334VA336HN340HK344HU348IS352IN356ID360IR364IQ368IE372IL376IT380CI384JM388JP392KZ398JO400KE404KP408KR410KW414KG417LA418LB422LS426LV428LR430LY434LI438LT440LU442MO446MG450MW454MY458MV462ML466MT470MQ474MR478MU480MX484MC492MN496MD498MS500MA504MZ508OM512NA516NR520NP524NL528AN530CW531AW533NC540VU548NZ554NI558NE562NG566NU570NF574NO578MP580UM581FM583MH584PW585PK586PA591PG598PY600PE604PH608PN612PL616PT620GW624TL626PR630QA634RE638RO642RU643RW646SH654KN659AI660LC662MF663PM666VC670SM674ST678SA682SN686RS688SC690SL694SG702SK703VN704SI705SO706ZA710ZW716ES724EH732SD736SR740SJ744SZ748SE752CH756SY760TJ762TH764TG768TK772TO776TT780AE784TN788TR792TM795TC796TV798UG800UA804MK807EG818GB826TZ834US840VI850BF854UY858UZ860VE862WF876WS882YE887CS891ZM894";

	private FileSystem fileSystem = null;
	private Configuration hdfsConfig = null;
	private Path baseOutputPath = null;
	private FsPermission newFilePremission;

	
	public static void main(String[] args) throws Exception {
		
		Configuration hdfsConfig = new Configuration();
				
		int retval = ToolRunner.run(hdfsConfig,new ExtractRawXML(), args);

		System.out.println(" return value : " + retval);
		
		System.exit(retval);
		
	}
	
	public int run(String[] args) throws Exception {
		
		String configXmlFile = "";
		String fileType = "";
		String requestHdfsFile = "";
		String requestLocalFile = "";
		String xmlTypes = "";

		for ( int idx=0; idx < args.length; idx++ ) {
			if ( args[idx].equals("-c") && (idx+1) < args.length ) {
				configXmlFile = args[idx+1];
			}

			if ( args[idx].equals("-t") && (idx+1) < args.length ) {
				fileType = args[idx+1];
			}
			
			if ( args[idx].equals("-r") && (idx+1) < args.length ) {
				requestHdfsFile = args[idx+1];
			}
			
			if ( args[idx].equals("-rl") && (idx+1) < args.length ) {
				requestLocalFile = args[idx+1];
			}

			if ( args[idx].equals("-x") && (idx+1) < args.length ) {
				xmlTypes = args[idx+1];
			}
		}

		if ( xmlTypes.length() == 0 ) {
			xmlTypes = "STLD,DetailedSOS,MenuItem,SecurityData,Store-Db,Product-Db";
		}
		
		if ( configXmlFile.length() == 0 || fileType.length() == 0 || ( (requestLocalFile.length() == 0 && requestHdfsFile.length() == 0) || ( requestLocalFile.length() > 0 && requestHdfsFile.length() > 0 )) ) {
			System.err.println("Missing config.xml (-c), filetype (-t), requestLocalFile (-rl) | requestHdfsFile (-r) [, xmlTypes (-x)]");
			System.err.println("Usage: ExtractRawXML -c config.xml -t filetype");
			
			System.err.println("Arguments supplied = ");
			for ( int idx = 0; idx < args.length; idx++ ) {
				System.err.print(" " + args[idx]);
			}
			System.err.println("");
			System.exit(8);
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
			
			runJob(daasConfig,fileType,requestLocalFile,requestHdfsFile,xmlTypes);
		}
		
		return(0);	
		
	}
	
	private void runJob(DaaSConfig daasConfig
                       ,String fileType
                       ,String requestLocalFile
                       ,String requestHdfsFile
                       ,String xmlTypes) {

		Job job;
		String requestedTerrDateParms;
		Path distListCache = null;
		ArrayList<Path> requestedPaths = null;
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddhhmmss");

		try {
			newFilePremission = new FsPermission(FsAction.ALL,FsAction.ALL,FsAction.READ_EXECUTE);
			
			baseOutputPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir() + Path.SEPARATOR + "ExtractRawXML");
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, baseOutputPath,daasConfig.displayMsgs());

			if ( daasConfig.displayMsgs() ) {
				System.out.println("\nOutput path = " + baseOutputPath.toString() + "\n");
			}
			
			hdfsConfig.set(CONFIG_SETTING_FILE_TYPES, xmlTypes);
			hdfsConfig.set(CONFIG_SETTING_CTRY_ISO2_LOOKUP, CTRY_ISO2_LOOKUP);
			hdfsConfig.set(CONFIG_SETTING_TIMESTAMP, format1.format(cal.getTime()));
			
			job = Job.getInstance(hdfsConfig, "Extract Raw XML");
			
			if ( requestLocalFile.length() > 0 ) {
				if ( new File(requestLocalFile).exists() ) {
					distListCache = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + "cache" + Path.SEPARATOR + DIST_CACHE_SELECTION_LIST);
					
					fileSystem.copyFromLocalFile(false, true, new Path(requestLocalFile), distListCache);
					fileSystem.setPermission(distListCache, newFilePremission);
				} else {
					System.err.println("Request File: " + requestLocalFile + " not found");
					System.exit(8);
				}
			} else {
				distListCache = new Path(requestHdfsFile);
			}
			
			if ( !fileSystem.exists(distListCache) && !fileSystem.isFile(distListCache) ) {
				System.err.println("Request file is not found/valid: " + distListCache.toString());
				System.exit(8);
			}
			
			requestedTerrDateParms = getRequestPathFromCache(distListCache);
			
			requestedPaths = getVaildFilePaths(daasConfig,fileType,requestedTerrDateParms, xmlTypes.split(","));
			
			for (Path addPath : requestedPaths ) {
				FileInputFormat.addInputPath(job, addPath);
				System.out.println(addPath.toString());
			}
			
			job.addCacheFile(new URI(distListCache.toString() + "#" + distListCache.getName()));

			LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
			
			job.setJarByClass(ExtractRawXML.class);
			job.setMapperClass(ExtractRawXMLMapper.class);
			job.setNumReduceTasks(0);
			job.setMapOutputKeyClass(NullWritable.class);
			job.setMapOutputValueClass(Text.class);
			job.setOutputKeyClass(NullWritable.class);
			job.setOutputKeyClass(Text.class);
			TextOutputFormat.setOutputPath(job, baseOutputPath);

			if ( ! job.waitForCompletion(true) ) {
				System.err.println("Error occured in MapReduce process, stopping");
				System.exit(8);
			}
			
			fileSystem.setPermission(baseOutputPath, newFilePremission);
			
			FileStatus[] status = fileSystem.listStatus(baseOutputPath);
			String[] parts;
			
			for ( int idx=0; idx < status.length; idx++ ) {
				if ( status[idx].getPath().getName().startsWith("_") ) {
					fileSystem.delete(status[idx].getPath(), false);
				} else {
					parts = status[idx].getPath().toString().split("~");
					fileSystem.rename(status[idx].getPath(), new Path(parts[0]));
					fileSystem.setPermission(new Path(parts[0]), newFilePremission);
				}
			}
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			System.exit(8);
		}
	}
	
	private String getRequestPathFromCache(Path cachePath) throws Exception {
	
		StringBuffer retValue = new StringBuffer();
		String line;
		String[] parts;
		
		HashMap<String,String> uniqueList = new HashMap<String,String>();
		HashMap<String,String> uniqueDays = new HashMap<String,String>();
		HashMap<String,String> uniqueStores = new HashMap<String,String>();

		BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fileSystem.open(cachePath))));
		
		while ( (line=br.readLine()) != null) {
			parts = line.split(SEPARATOR_CHARACTER, -1);
			
			if ( parts.length !=3 ) {
				System.err.println("Input line invalid '" + line + "'");
			} else {
				if ( !uniqueList.containsKey(parts[0] + ":" + parts[2]) ) {
					uniqueList.put(parts[0] + ":" + parts[2],"");
				}
				
				if ( !uniqueDays.containsKey(parts[2]) ) {
					uniqueDays.put(parts[2], "");
				}
				
				if ( !uniqueStores.containsKey(parts[0] + SEPARATOR_CHARACTER + parts[1]) ) {
					uniqueStores.put(parts[0] + SEPARATOR_CHARACTER + parts[1], "");
				}
			}
		}
	
		br.close();
		
		if ( uniqueList.size() == 0 || uniqueDays.size() > MAX_UNIQUE_DAYS || uniqueStores.size() > MAX_UNIQUE_STORES ) {
			
			System.err.println("\n\n*************************************\n");
			
			if ( uniqueList.size() == 0 ) {
				System.err.println("ERRROR: No valid input found");
			}
			
			if ( uniqueDays.size() > MAX_UNIQUE_DAYS ) {
				System.err.println("ERRROR: Total number of days for extract exceeds maximum of " + MAX_UNIQUE_DAYS + " found " + uniqueDays.size());
			}

			if ( uniqueStores.size() > MAX_UNIQUE_STORES ) {
				System.err.println("ERRROR: Total number of locations for extract exceeds maximum of " + MAX_UNIQUE_STORES + " found " + uniqueStores.size());
			}
			
			System.err.println("\n*************************************\n\nAdjust input file so that it does not exceed maximums");
			System.exit(8);
		}
		
		for (Map.Entry<String, String> entry : uniqueList.entrySet()) {
			if ( retValue.length() > 0 ) {
				retValue.append(",");
			}
			retValue.append(entry.getKey());
		}
		
		return(retValue.toString());
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
}
