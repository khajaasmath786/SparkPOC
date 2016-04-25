package com.mcd.gdw.daas.driver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
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
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.abac.ABaC;
import com.mcd.gdw.daas.mapreduce.PMIXMultiFileCombinerMapper;
import com.mcd.gdw.daas.mapreduce.PMIXMultiFileCombinerReducer;
import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;
/**
 * 
 * @author Sateesh Pula
 * Driver program to combine multiple PMIX files by business date
 */

public class PMIXMultiFileCombinerDriver extends Configured implements Tool {

	
	public static void main(String[] args) throws Exception{
		
		ToolRunner.run(new Configuration(),new PMIXMultiFileCombinerDriver(), args);
	}
	@Override
	public int run(String[] argAll) throws Exception {
		
		ABaC abac2 = null;
		DaaSConfig daasConfig;
		
		GenericOptionsParser gop = new GenericOptionsParser(argAll);
		String[] args = gop.getRemainingArgs();
		
		if(args == null || args.length < 6){
			System.out.println( " Wrong usage. Required Params: InputRootDir OutputDir ConfigFile FileType NumofReducers FinalDestPath [PrevJobSeqNbr] [PrevJobGroupId]");
			
			return 0;
		}
		String inputRootDir 			 = args[0];
		String outputDir    			 = args[1];
		String configXmlFile    		 = args[2];
		String fileType     			 = args[3];
		String numofReducers  			 = args[4];
		String salesanapmixFinalDestPath = args[5];
		String createJobDetails			 = args[6];
		String writeOutputtohivepartitions = "FALSE";
		if(args.length > 7)
			writeOutputtohivepartitions =  args[7];
		
		System.out.println ( " createJobDetails ********* " + createJobDetails);
		
		String jobSeqNbrStr    			 = null;
		
		
		String prevJobGroupIdStr		 = null;
		
		int prevjobSeqNbr = -11;
		int prevJobGroupId =-1;
		
		
		prevjobSeqNbr = 3;
		
		daasConfig = new DaaSConfig(configXmlFile,fileType);
		
		
		int jobId = 0;
		Job job = new Job(getConf(),"PMIXMultiFileCombiner");
		
		if("TRUE".equalsIgnoreCase(createJobDetails)){
			abac2 = new ABaC(daasConfig);
			if(prevJobGroupIdStr != null){
				prevJobGroupId = Integer.parseInt(prevJobGroupIdStr);
			}else{	
				prevJobGroupId= abac2.getOpenJobGroupId(DaaSConstants.TDA_EXTRACT_JOBGROUP_NAME);
				if(prevJobGroupId == -1)	
					prevJobGroupId = abac2.createJobGroup(DaaSConstants.TDA_EXTRACT_JOBGROUP_NAME);
					
				
			}
			jobId=abac2.createJob(prevJobGroupId,++prevjobSeqNbr,job.getJobName());
		}
		
		getConf().set("WRITE_TO_PARTITIONS", writeOutputtohivepartitions);
		
		//AWS START
		//FileSystem hdfsFileSystem = FileSystem.get(getConf());
		FileSystem hdfsFileSystem = HDFSUtil.getFileSystem(daasConfig, getConf());
		//AWS END
		
		
		job.setMapperClass(PMIXMultiFileCombinerMapper.class);
		job.setReducerClass(PMIXMultiFileCombinerReducer.class);
		job.setJarByClass(PMIXMultiFileCombinerDriver.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		
		job.setInputFormatClass(TextInputFormat.class);
		job.setNumReduceTasks(Integer.parseInt(numofReducers));
		
		
		FileInputFormat.addInputPaths(job, inputRootDir+"/PMIX*");
//		FileInputFormat.setInputPathFilter(job,  PMIXPathFilter.class);
		
		Path outputPath = new Path(outputDir);
		FileOutputFormat.setOutputPath(job, outputPath);
		if (hdfsFileSystem.exists(outputPath)) {
			hdfsFileSystem.delete(outputPath, true);
			Logger.getLogger(GenerateTdaFormatStld.class.getName()).log(
					Level.INFO,
					"Removed existing output path = " + outputDir);
		}
		
		
		
		
		
		FileStatus[] fstatus = null;
		FileStatus[] fstatustmp = null;
		String[] inputpathstrs = inputRootDir.split(",");
	
		for(String cachepathstr:inputpathstrs){
			
			fstatustmp = hdfsFileSystem.listStatus(new Path(cachepathstr), new PathFilter() {
				
				@Override
				public boolean accept(Path pathname) {
					if(pathname.getName().startsWith("PMIX"))
						return true;
							
					return false;
				}
			});
			fstatus = (FileStatus[])ArrayUtils.addAll(fstatus, fstatustmp);

		}
		String filepath;
		String datepart;
		HashSet<String> dtset = new HashSet<String>();
		
		for(FileStatus fstat:fstatus){
			filepath = fstat.getPath().getName();
			int lastindx = filepath.indexOf("-");
			datepart = filepath.substring(0,lastindx);
			dtset.add(datepart);
		}
		Iterator<String> it = dtset.iterator();
		
		while(it.hasNext()){
			datepart = it.next();
			System.out.println(" trying to add "+datepart);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"0",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"1",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"2",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"3",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"4",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"5",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"6",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"7",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"8",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,datepart+DaaSConstants.SPLCHARTILDE_DELIMITER+"9",TextOutputFormat.class, Text.class, Text.class);
			
		}
		
		LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
		
		job.waitForCompletion(true);
		
		
		
		
		//move files
		fstatustmp = hdfsFileSystem.listStatus(outputPath,new PathFilter() {
			
			@Override
			public boolean accept(Path pathname) {
				if(pathname.getName().startsWith("PMIX"))
					return true;
				return false;
			}
		});
		System.out.println(" num of output files at " + outputPath.toString() + " " +fstatustmp.length);
		String fileName;
		
		FsPermission fspermission =  new FsPermission(FsAction.ALL,FsAction.ALL,FsAction.ALL);
		
		StringBuffer sbf = new StringBuffer();
//GTDA_Dly_Pmix_US_01_20140617.20140716190326.psv
//		String dtFmt = DaaSConstants.SDF_yyyyMMddHHmmssSSS.format(new Date());
//		sbf.append("GTDA_Dly_Pmix_US_");
		
		String[] fileNameParts;
		
	  
		HashSet<String> uniqueSet = new HashSet<String>();
		
		for(FileStatus fstat:fstatustmp){
//			fileName  = fstat.getPath().getName().replace("PMIX", "PMIX-");
			
			fileName  = fstat.getPath().getName();
			
			fileNameParts = fileName.split(DaaSConstants.SPLCHARTILDE_DELIMITER);
			
			
			sbf.setLength(0);
			sbf.append(fileNameParts[0]).append("-").append(fileNameParts[1]).append("-").append(fileNameParts[2]).append("-").append(fileNameParts[3].substring(0,1));
			
			fileName = sbf.toString();
			
			System.out.println(" fileName  " + fileName);
			
			String finalPath = salesanapmixFinalDestPath+fileName;
			
			String datewithdashes = formatDateAsTsDtOnly(fileNameParts[2]);
			if("TRUE".equalsIgnoreCase(writeOutputtohivepartitions)){
			
				
				
				if(!uniqueSet.contains(fileNameParts[1]+"-"+fileNameParts[2])){
					if(hdfsFileSystem.exists(new Path(salesanapmixFinalDestPath+"type=PMIX"+Path.SEPARATOR+"terr_cd="+fileNameParts[1]+Path.SEPARATOR+"pos_busn_dt="+datewithdashes))){
						hdfsFileSystem.delete(new Path(salesanapmixFinalDestPath+"type=PMIX"+Path.SEPARATOR+"terr_cd="+fileNameParts[1]+Path.SEPARATOR+"pos_busn_dt="+datewithdashes),true);
					}
					hdfsFileSystem.mkdirs(new Path(salesanapmixFinalDestPath+"type=PMIX"+Path.SEPARATOR+"terr_cd="+fileNameParts[1]+Path.SEPARATOR+"pos_busn_dt="+datewithdashes));
				}
				finalPath = salesanapmixFinalDestPath+"type=PMIX"+Path.SEPARATOR+"terr_cd="+fileNameParts[1]+Path.SEPARATOR+"pos_busn_dt="+datewithdashes+Path.SEPARATOR+fileName;
				uniqueSet.add(fileNameParts[1]+"-"+fileNameParts[2]);
			}
		
			System.out.println(" finalPath = " + finalPath);
				if(!hdfsFileSystem.rename(new Path(fstat.getPath().toString()), new Path(finalPath))){
					System.out.println("could not rename " + fstat.getPath().toString() + " to " +finalPath);
				}else{
					hdfsFileSystem.setPermission(new Path(finalPath), fspermission);
					System.out.println(" renamed " + fstat.getPath().toString() + " to " +finalPath);
				}
			
		}
		
		
		if("TRUE".equalsIgnoreCase(createJobDetails)){
			abac2.closeJob(jobId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
			abac2.closeJobGroup(prevJobGroupId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
			
			abac2.dispose();
		}
		return 0;
	}
	
	private String formatDateAsTsDtOnly(String in) {

		String retTs = StringUtils.EMPTY;
		
		if ( in.length() >= 8 ) {
			retTs = in.substring(0, 4) + "-" + in.substring(4, 6) + "-" + in.substring(6, 8);
		}

		return(retTs);
		
	}	

}
