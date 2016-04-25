package com.mcd.gdw.daas.driver;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapred.lib.MultipleOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.mcd.gdw.daas.mapreduce.CoffeeExtractMapper;
import com.mcd.gdw.daas.util.HDFSUtil;

public class CoffeeExtractDriver extends Configured implements Tool {

	

	public static void main(String[] args) throws Exception {Configuration conf1 = new Configuration();
	
		int retval = ToolRunner.run(conf1,new CoffeeExtractDriver(), args);

		System.out.println(" return value : " + retval);

	}
	
	@Override
	public int run(String[] argsAll) throws Exception {
		
		try{
			GenericOptionsParser gop = new GenericOptionsParser(argsAll);
			String[] args = gop.getRemainingArgs();
			
			Configuration conf = this.getConf();
			//AWS START
			FileSystem fileSystem = HDFSUtil.getFileSystem(args[1], conf);
			//AWS END
			Job job = new Job(conf,"Coffee Extract");
			//AWS START
			//FileSystem fileSystem = FileSystem.get(conf);
			//AWS END
			
			job.setJarByClass(CoffeeExtractMapper.class);
			
			job.setMapperClass(CoffeeExtractMapper.class);
			
			job.setOutputKeyClass(NullWritable.class);
			job.setOutputValueClass(Text.class);
			
			job.setNumReduceTasks(0);
			
			FileInputFormat.addInputPath(job, new Path(args[0]));
			HDFSUtil.removeHdfsSubDirIfExists(fileSystem, new Path(args[1]), true);
			FileOutputFormat.setOutputPath(job, new Path(args[1]));
			
			String cacheFile = args[2];
			
			String[] cachefilestrs = cacheFile.split(",");

			for (String cachepathstr : cachefilestrs) {

				System.out.println("adding " + cachepathstr + " to dist cache");
				DistributedCache.addCacheFile(new Path(cachepathstr).toUri(),job.getConfiguration());
			}
			
			
			LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
			
			MultipleOutputs.addNamedOutput(job,"COFFEEEXTRACT",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,"CASHLESSEXTRACT",TextOutputFormat.class, Text.class, Text.class);
			MultipleOutputs.addNamedOutput(job,"GUAC",TextOutputFormat.class, Text.class, Text.class);
			job.waitForCompletion(true);
		
		
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		
		
		return 0;
	}


}