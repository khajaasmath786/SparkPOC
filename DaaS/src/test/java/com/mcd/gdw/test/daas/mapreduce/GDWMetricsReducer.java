package com.mcd.gdw.test.daas.mapreduce;

import java.io.IOException;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.test.daas.driver.*;
import com.mcd.gdw.daas.util.HDFSUtil;


public class GDWMetricsReducer extends Reducer<Text, Text, Text, Text> {
	private MultipleOutputs<Text, Text> mos;
	StringBuffer mosKey = new StringBuffer();
	StringBuffer mosValue = new StringBuffer();
	Text outputkey = new Text();
	Text outputvalue = new Text();

	@Override
	public void setup(Context context) {

		try {
			mos = new MultipleOutputs<Text, Text>(context);

		} catch (Exception ex) {
			System.err.println("Error in initializing GDWMetricsReducer:");
			System.err.println(ex.toString());
			System.exit(8);
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		mos.close();
	}


	@Override
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		int wordCount = 0;
		if(key.toString().contains("Count"))
		{
		for (Text value : values) {
					
			
				int count = Integer.parseInt(value.toString());
				wordCount += count;
			
				
		}
		
		mosKey.setLength(0);
		mosKey.append(GenerateGDWMetrics.GDW_FILE_STORE_COUNTS);

		mosValue.setLength(0);
		mosValue.append(HDFSUtil.restoreMultiOutSpecialChars(key.toString())+DaaSConstants.PIPE_DELIMITER+wordCount);

		outputkey.clear();
		outputvalue.clear();

		outputkey.set(HDFSUtil.replaceMultiOutSpecialChars(mosKey.toString()));
		outputvalue.set(mosValue.toString());
		mos.write(outputkey.toString(), NullWritable.get(), outputvalue);

		}
		else
		{
			for (Text value : values) {
			mosKey.setLength(0);
			mosKey.append(GenerateGDWMetrics.GDW_METRICS_FILE);
			
			mosValue.setLength(0);
			mosValue.append(value.toString());
		
									
			outputkey.clear();
			outputvalue.clear();
			
			outputkey.set(HDFSUtil.replaceMultiOutSpecialChars(mosKey.toString()));
			outputvalue.set(mosValue.toString());
			mos.write( outputkey.toString(),NullWritable.get(), outputvalue);
		}
		}
	}
}