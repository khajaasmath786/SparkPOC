package com.mcd.gdw.daas.mapreduce;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.util.HDFSUtil;




public class FilterOutRestatementsMapper extends Mapper<LongWritable,Text,NullWritable,Text>{

	private MultipleOutputs<NullWritable, Text> mos;
	
	HashSet<String> uniqueTerrCdBusnDtStoreId = new HashSet<String>();
	String multioutbaseOutputPath= "";

	String fileName = "";
	Path fileSplit = null;

	InputStreamReader insr = null;
	
	@Override
	protected void setup(Context context)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		uniqueTerrCdBusnDtStoreId.clear();
		URI[] distPaths;
	    //Path distpath = null;
	    BufferedReader br = null;
	    String[] distPathParts;
	    
	    //AWS START
	    FileSystem hdfsFileSystem;
	    //AWS END
	    
	    fileSplit = ((FileSplit)context.getInputSplit()).getPath();
	    fileName = fileSplit.getName();
	    
	    mos = new MultipleOutputs<NullWritable, Text>(context);
   
	    multioutbaseOutputPath = context.getConfiguration().get("MULTIOUT_BASE_OUTPUT_PATH");
		try {
			//AWS START
			hdfsFileSystem = HDFSUtil.getFileSystem(context.getConfiguration().get(DaaSConstants.HDFS_ROOT_CONFIG), context.getConfiguration());
			//AWS END
			
		    distPaths = context.getCacheFiles();
		    
		    if (distPaths == null){
		    	System.err.println("distpath is null");
		    	System.exit(8);
		    }
		      
		    if ( distPaths != null && distPaths.length > 0 )  {
		    	  
		    	System.out.println(" number of distcache files : " + distPaths.length);
		    	  
		    	for ( int i=0; i<distPaths.length; i++ ) {
			    	  
			    	  //distpath = distPaths[i];
				     
			    	  System.out.println("distpaths:" + distPaths[i].toString());
			    	  //System.out.println("distpaths URI:" + distPaths[i].toUri());
			    	  
			    	  distPathParts = 	distPaths[i].toString().split("#");
			    	  
			    	  if ( distPaths[i].toString().contains("terrcd_busndt_storeid_list.txt") ) {
    	  		      	  
			    		  //AWS START
			    		  //insr = new InputStreamReader(new GZIPInputStream(FileSystem.get(context.getConfiguration()).open(new Path(distPaths[i]))));
			    		  insr = new InputStreamReader(new GZIPInputStream(hdfsFileSystem.open(new Path(distPaths[i]))));
			    		  //AWS END 
//							
							br = new BufferedReader( insr);
//				    	  br  = new BufferedReader(new FileReader(distPathParts[1])); 
				    	  addExcludeListToMap(br);
				      	  System.out.println("Loaded Include List Values Map");
				      }
			      }
		      }
		    
		} catch (Exception ex) {
			System.err.println("Error in initializing TLDNewDataHubMapperHCat:");
			System.err.println(ex.toString());
			System.exit(8);
		}finally{
			try{
				if(br != null)
					br.close();
				if(insr != null)
					insr.close();
			}catch(Exception ex){
				
			}
		}
		
	}
	
	private void addExcludeListToMap(BufferedReader br) {
		
		String line = null;
		String[] lineParts;
		try {
			while ((line = br.readLine()) != null) {
				if (line != null && !line.isEmpty()) {
					
					lineParts = line.split("~");
					
					System.out.println(" line " + line);
					uniqueTerrCdBusnDtStoreId.add(lineParts[0]+"~"+lineParts[1]+"~"+lineParts[2]);
					
//					System.out.println(terrCd + " " + lgcyLclRfrDefCd + " " + fromDt + " " + toDt );
				}
			}
			
			System.out.println(" uniqueTerrCdBusnDtStoreId  " + uniqueTerrCdBusnDtStoreId.size());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(8);
		} finally {
			try {
				if (br != null)
					br.close();
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	HashSet<String> alreadyPrinted = new HashSet<String>();
	
	int i = 0;
	@Override
	protected void map(LongWritable key, Text value,
			org.apache.hadoop.mapreduce.Mapper.Context context)
			throws IOException, InterruptedException {
		
		String valTxt = value.toString();
		String[] valParts = valTxt.split("\t",-1);
		
		if(i == 0)
			System.out.println( "value " + value);
		i++;
		String posBusnDt = valParts[0];
		String terrCd    = valParts[4];
		String storeId   = valParts[2];
		String posOrdKey = valParts[1];
		
//		if("25436".equalsIgnoreCase(storeId) && "POS0003:287909656".equalsIgnoreCase(posOrdKey)){
//				context.getCounter("Count", fileSplit.getName()).increment(1);
//		}
			
//		if(!"25436".equalsIgnoreCase(storeId) || !"POS0003:287909656".equalsIgnoreCase(posOrdKey))
//			return;
		
//		String fileType = "STLD";
//		if(fileSplit.toString().contains("STLD")){
//				fileType = "STLD";
//				posBusnDt = valParts[0];
//				terrCd    = valParts[4];
//				storeId   = valParts[2];
//		}
//		if(fileSplit.toString().contains("DetailedSOS")){
//				fileType="DetailedSOS";
//				posBusnDt = valParts[1];
//				terrCd    = valParts[0];
//				storeId   = valParts[3];
//		}
		
		
		String filterKey = terrCd+"~"+posBusnDt+"~"+storeId;
		if(!alreadyPrinted.contains(filterKey)){
//			System.out.println( " filterKey " + filterKey);
			alreadyPrinted.add(filterKey);
		}
		
//		if("13745".equalsIgnoreCase(storeId)){
//			context.getCounter("Count",fileName).increment(1);
//		}
		
		if(!uniqueTerrCdBusnDtStoreId.contains(filterKey)){
			
//			mos.write(HDFSUtil.replaceMultiOutSpecialChars(terrCd+posBusnDt), NullWritable.get(), value, baseOutputPath+"/"+fileType+"/terr_cd="+terrCd+"/pos_busn_dt="+posBusnDt+"/"+HDFSUtil.replaceMultiOutSpecialChars(terrCd+"~"+posBusnDt));
			mos.write(HDFSUtil.replaceMultiOutSpecialChars(terrCd+posBusnDt), NullWritable.get(), value, multioutbaseOutputPath+"/terr_cd="+terrCd+"/pos_busn_dt="+posBusnDt+"/"+HDFSUtil.replaceMultiOutSpecialChars(terrCd+"~"+posBusnDt));
			context.getCounter("Count","ValidRecords").increment(1);
		}else{
//			if("25436".equalsIgnoreCase(storeId) && "POS0003:287909656".equalsIgnoreCase(posOrdKey)){
//				context.getCounter("Count","SkippingTerrCdBusnDtStore22222").increment(1);
//			}
			context.getCounter("Count","SkippingTerrCdBusnDtStore").increment(1);
		}
	}

	
	
	
	@Override
	protected void cleanup(org.apache.hadoop.mapreduce.Mapper.Context context)
			throws IOException, InterruptedException {
		mos.close();
	}
}
