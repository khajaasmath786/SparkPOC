package com.mcd.gdw.daas.driver;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.mcd.gdw.daas.mapreduce.CompressUsingGZipCodec;
import com.mcd.gdw.daas.util.HDFSUtil;

public class CompressUsingZipCodecDriver  extends Configured implements Tool {

	public static void main(String[] args) {
		try {
			int retval = ToolRunner.run(new Configuration(),new CompressUsingZipCodecDriver(), args);

			System.out.println(" return value : " + retval);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public int run(String[] args) throws Exception {
		
		Configuration hdfsConfig  = getConf();
		
		//AWS START
		//FileSystem fileSystem = FileSystem.get(getConf());
		FileSystem fileSystem = HDFSUtil.getFileSystem(args[1], getConf());
		//AWS END

		try {
			System.out.println("\nCopy Files with Filter\n");
			
			
			hdfsConfig.set("mapred.compress.map.output", "true");
			hdfsConfig.set("mapred.output.compress", "true");
//			hdfsConfig.set("mapred.output.compression.type", "BLOCK"); 
			hdfsConfig.set("mapred.output.compression.type", "RECORD"); 
			hdfsConfig.set("mapred.map.output.compression.codec", "org.apache.hadoop.io.compress.SnappyCodec");
//			hdfsConfig.set("mapred.map.output.compression.codec", "org.apache.hadoop.io.compress.GzipCodec"); 
			hdfsConfig.set("mapred.output.compression.codec", "org.apache.hadoop.io.compress.GzipCodec");
			
			
			Job job = new Job(hdfsConfig, "Compress ");

			job.setJarByClass(CompressUsingZipCodecDriver.class);
			job.setMapperClass(CompressUsingGZipCodec.class);
			
			job.setMapOutputKeyClass(NullWritable.class);
			job.setMapOutputValueClass(Text.class);
			
			job.setOutputKeyClass(NullWritable.class);
			//job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			job.setNumReduceTasks(0);
//			job.setOutputFormatClass(TextOutputFormat.class);
			LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
			
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, new Path(args[1]), true);
			FileInputFormat.setInputPaths(job, new Path(args[0]));
			TextOutputFormat.setOutputPath(job, new Path(args[1]));
			
			job.waitForCompletion(true);
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return 0;
	}
	
	

}
