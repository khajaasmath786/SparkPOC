package com.mcd.gdw.daas.driver;

import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.mapreduce.FileIdCheckMapper;
import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;

public class FileIdCheck extends Configured implements Tool {

	public static void main(String[] args) throws Exception {

		int retval = ToolRunner.run(new Configuration(),new FileIdCheck(), args);

		System.out.println(" return value : " + retval);		

	}

	@Override
	public int run(String[] argsall) throws Exception {

		String configXmlFile = "";
		String fileType = "";
		String terrDate = "";
		String[] args;

		GenericOptionsParser gop = new GenericOptionsParser(argsall);
		
		args = gop.getRemainingArgs();

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
		}
		
		if ( configXmlFile.length() == 0 || fileType.length() == 0 || terrDate.length() == 0 ) {
			System.err.println("Missing config.xml and/or filetype");
			System.err.println("Usage: GenerateAsterFormat -c config.xml -t filetype -d territoryDateParms");
			System.exit(8);
		}

		DaaSConfig daasConfig = new DaaSConfig(configXmlFile,fileType);
		
		if ( daasConfig.configValid() ) {
			runExtract(daasConfig,fileType,getConf(),terrDate);
			
		} else {
			System.err.println("Invalid config.xml and/or filetype");
			System.err.println("Config File = " + configXmlFile);
			System.err.println("File Type   = " + fileType);
			System.exit(8);
		}
		
		return(0);
	}
	private void runExtract(DaaSConfig daasConfig
            ,String fileType
            ,Configuration hdfsConfig
            ,String terrDate) {

		Job job;
		ArrayList<Path> requestedPaths = null;

		try {
			job = new Job(hdfsConfig, "Processing File ID Extract");

			System.out.println("\nCreate File Id\n");

			//AWS START
			//FileSystem fileSystem = FileSystem.get(hdfsConfig);
			FileSystem fileSystem = HDFSUtil.getFileSystem(daasConfig, hdfsConfig);
			//AWS END

			Path outPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir() + Path.SEPARATOR + "fileidextract" );
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, outPath,daasConfig.displayMsgs());

			if ( terrDate.equalsIgnoreCase("work") ) {
				requestedPaths = getVaildFilePaths(daasConfig,fileSystem,fileType);
			} else {
				requestedPaths = getVaildFilePaths(daasConfig,fileSystem,fileType,terrDate);
			}

			for (Path addPath : requestedPaths ) {
				FileInputFormat.addInputPath(job, addPath);
			}

			//AWS START
			job.setJarByClass(FileIdCheck.class);
			//job.setJarByClass(GenerateAsterFormat.class);
			//AWS END
			job.setMapperClass(FileIdCheckMapper.class);
			//job.setReducerClass(AsterFormatReducer.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(Text.class);
			job.setOutputKeyClass(NullWritable.class);
			job.setOutputValueClass(Text.class);
			job.setOutputFormatClass(TextOutputFormat.class);
			job.setNumReduceTasks(0);

			TextOutputFormat.setOutputPath(job, outPath);

			if ( !job.waitForCompletion(true) ) {
				System.err.println("Error occured in MapReduce process, stopping");
				System.exit(8);
			}

		} catch (Exception ex) {
			System.err.println("Error occured in MapReduce process:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
	}

	private ArrayList<Path> getVaildFilePaths(DaaSConfig daasConfig
                       ,FileSystem fileSystem
                       ,String fileType) {

		ArrayList<Path> retPaths = new ArrayList<Path>();
		String filePath;

		Path listPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir() + Path.SEPARATOR + daasConfig.fileSubDir() + Path.SEPARATOR + "step1");

		try {
			FileStatus[] fstus = fileSystem.listStatus(listPath);

			for (int idx=0; idx < fstus.length; idx++ ) {
				filePath = HDFSUtil.restoreMultiOutSpecialChars(fstus[idx].getPath().getName());

				if ( filePath.startsWith("STLD") || filePath.startsWith("DetailedSOS") || filePath.startsWith("MenuItem") || filePath.startsWith("SecurityData") || filePath.startsWith("store-db") || filePath.startsWith("product-db") ) {
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
                       ,FileSystem fileSystem
                       ,String fileType
                       ,String requestedTerrDateParms) {

		ArrayList<Path> retPaths = new ArrayList<Path>();

		try {

			Path[] requestPaths = HDFSUtil.requestedArgsPaths(fileSystem, daasConfig, requestedTerrDateParms, "STLD", "DetailedSOS", "MenuItem", "SecurityData","store-db","product-db");

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
			System.err.println("Error occured in GenerateAsterFormat.getVaildFilePaths:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}

		return(retPaths);
	}

}