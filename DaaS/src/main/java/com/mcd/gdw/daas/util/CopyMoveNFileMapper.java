package com.mcd.gdw.daas.util;



import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.driver.MergeToFinal;

public class CopyMoveNFileMapper extends Mapper<LongWritable,Text,NullWritable,Text>{

	
	Configuration conf = null;
	FileSystem hdfsFileSystem = null;
	private MultipleOutputs<NullWritable, Text> mos;
	Text copyFileStatus = new Text();
	String copyOrMove = "COPY"; 
	
	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		
		conf = context.getConfiguration();
		//AWS START
		//hdfsFileSystem = FileSystem.get(conf);
		try {
			hdfsFileSystem = HDFSUtil.getFileSystem(conf.get(DaaSConstants.HDFS_ROOT_CONFIG), conf);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			System.exit(8);
		}
		//AWS END

		mos = new MultipleOutputs<NullWritable, Text>(context);
		
		copyOrMove = conf.get(DaaSConstants.JOB_CONFIG_PARM_COPY_OR_MOVE_FILES);
		if(copyOrMove == null){
			copyOrMove = "COPY";
		}else{
			copyOrMove = copyOrMove.trim().toUpperCase();
		}
	}

	String[] paths = null;
	StringBuffer sbf = new StringBuffer();
	
	@Override
	protected void map(LongWritable key, Text value,Context context)
			throws IOException, InterruptedException {
		
		
		try{
			
//			System.out.println( "value "+ value.toString());
			paths = value.toString().split("\t");
			if(paths.length != 2){
				
				context.getCounter("Count","CopyFiles_InvalidPaths").increment(1);
				return;
			}
			
			String fromPathStr = paths[0];
			String toPathStr   = paths[1];
			
			
			
			String fileStatus = "Success";
			
			if(!FileUtil.copy(hdfsFileSystem,new Path(fromPathStr),hdfsFileSystem,new Path(toPathStr),false,conf)){
				fileStatus = "Failed";
			}
			
			sbf.setLength(0);
			sbf.append(fromPathStr).append(" to ").append(toPathStr).append(" | ").append(fileStatus);
			copyFileStatus.clear();
			copyFileStatus.set(sbf.toString());
			
			mos.write("CopyMoveFileStatus", NullWritable.get(), copyFileStatus);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	@Override
	protected void cleanup(org.apache.hadoop.mapreduce.Mapper.Context context)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		super.cleanup(context);
		mos.close();
	}
	
	
	
	
	

}
