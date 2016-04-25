package com.mcd.gdw.daas.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.abac.ABaC;
import com.mcd.gdw.daas.mapreduce.NpStldXmlMapper;
import com.mcd.gdw.daas.mapreduce.NpStldXmlReducer;
import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;
/**
 * 
 * @author Sateesh Pula
 * Driver class to extract HDR and DTL from STLD files
 * Usage:
 * hadoop jar /home/mc32445/scripts/mcdonaldsmrnew.jar com.mcd.gdw.daas.driver.GenerateTdaFormatStld \
 *	-r /user/mc32445/poc/work/np_xml \
 *	-o /user/mc32445/poc/stldextract3 \
 *	-c /user/mc32445/tdaextract/distcache/TDA_CODE_VALUES.psv,/user/mc32445/tdaextract/distcache/ITEMCODE_MAPPING.psv,/user/mc32445/tdaextract/distcache/MenuPriceBasis.psv
 *  
 *  r - input path, -o for output path, -c all the distributed cache files, -p properties
 */
public class GenerateTdaFormatStld extends Configured implements Tool {

	
	private int jobSeqNbr = 1;
	private int jobGroupId = 0;

	
	private DaaSConfig daasConfig;
	private String fileType;
	String owshFltr = "*";
	String generatFieldsForNielsen = "false";
	private Set<Integer> validTerrCdSet = new HashSet<Integer>();
	private String createJobDetails = "true";
	
//	public static class STLDPathFilter implements PathFilter{
//		
//		@Override
//		public boolean accept(Path pathname) {
//			try{
//			
//			String fileName = pathname.getName().toUpperCase();
//
//			if(fileName.endsWith("STEP1"))
//				return true;
//			if(fileName.startsWith("STLDRXD126840") || fileName.startsWith("STLD~840")){
//				return true;
//			}
//			}catch(Exception ex){
//				ex.printStackTrace();
//			}
//			return false;
//		}
//		
//
//	}

	
	public static void main(String[] args) throws Exception {
		int retval = ToolRunner.run(new Configuration(),new GenerateTdaFormatStld(), args);

//		System.out.println(" return value : " + retval);
		
		

	}
	String filterOnStoreId = "FALSE";
	
	@Override
	public int run(String[] argsall) throws Exception {
		
		GenericOptionsParser gop = new GenericOptionsParser(argsall);
		String[] args = gop.getRemainingArgs();
		
//		System.out.println( " gop.getRemainingArgs().length " + gop.getRemainingArgs().length);
//		System.out.println(args.length);
//		
//		for (int idx2 = 0; idx2 < args.length; idx2++) {
//			System.out.println (idx2 + " : "+args[idx2]);
//		}
		int idx;
		String inputRootDir = "";

		String outputDir = "";

		String cacheFile = "";
		
		String propertiesstr = "";
		
		
		String configXmlFile = "";
		String fileType = "";
		
		String vldTerrCdsStr = "";
		
		

		try {
			for (idx = 0; idx < args.length; idx++) {
				if ((idx % 2) != 0) {
					if (args[idx - 1].equalsIgnoreCase("-r")) {
						inputRootDir = args[idx];
					}

					else if (args[idx - 1].equalsIgnoreCase("-o")) {
						outputDir = args[idx];
					}
					else if (args[idx - 1].equalsIgnoreCase("-dc")) {
						cacheFile = args[idx];
					}
					else if (args[idx - 1].equalsIgnoreCase("-p")) {
						propertiesstr = args[idx];
					}
//					if (args[idx - 1].equals("-seqNbr")) {
//						jobSeqNbr = Integer.parseInt(args[idx]);
//					}
					else if ( args[idx-1].equalsIgnoreCase("-c") ) {
						configXmlFile = args[idx];
					}

					else if ( args[idx-1].equalsIgnoreCase("-t")  ) {
						fileType = args[idx];
					}
					
					else if ( args[idx-1].equalsIgnoreCase("-owshfltr") ) {
						owshFltr = args[idx];
					}
					else if ( args[idx-1].equals("-generatFieldsForNielsen") ) {
						generatFieldsForNielsen = args[idx];
					}
					else if ( args[idx-1].equalsIgnoreCase("-vldTerrCodes") ) {
						vldTerrCdsStr = args[idx];
						
						String[] terrCds = vldTerrCdsStr.split(",");
						for(String terrCd:terrCds){
							validTerrCdSet.add(Integer.parseInt(terrCd));
						}
					}
					else if ( args[idx-1].equalsIgnoreCase("-filterOnStoreId") ) {
						filterOnStoreId = args[idx];
						if(!filterOnStoreId.equalsIgnoreCase("FALSE") && !filterOnStoreId.equalsIgnoreCase("TRUE")){
							filterOnStoreId = "FALSE";
						}
					}
					else if ( args[idx-1].equalsIgnoreCase("-createJobDetails") ) {
						System.out.println(" createJobDetails 1" + createJobDetails);
						createJobDetails = args[idx];
					}
					
					
				}
			}
			if(StringUtils.isBlank(createJobDetails)){
				createJobDetails = "TRUE";
			}
			
			System.out.println(" createJobDetails 2" + createJobDetails);
			
			daasConfig = new DaaSConfig(configXmlFile,fileType);
			
		

			if (inputRootDir.length() > 0 && outputDir.length() > 0) {
				 runJob(inputRootDir, outputDir, cacheFile,propertiesstr,getConf());
			} else {
				Logger.getLogger(GenerateTdaFormatStld.class.getName())
						.log(Level.SEVERE,
								"Missing input root directory, sub directory or output directory arguments");
				Logger.getLogger(GenerateTdaFormatStld.class.getName())
						.log(Level.INFO,
								"Usage "
										+ GenerateTdaFormatStld.class
												.getName()
										+ " -r rootdirectory -o outputdirectory -c distcachefile");
				System.exit(8);
			}
		} catch (Exception ex) {
			Logger.getLogger(GenerateTdaFormatStld.class.getName()).log(
					Level.SEVERE, null, ex);
			throw ex;
		}
		
		return 0;
	}

	public int runJob(String inputRootDir, String outputDir,
			String cacheFile,
			String propertiesstr,Configuration conf) throws Exception {

		
		Job job;
		FileSystem hdfsFileSystem = null;
		Path hdfsOutputPath;
	
		
//		PathFilterByFileName stldPathFilter = new PathFilterByFileName("STLD");
	
		int jobId = 0;
		
		
		
		ABaC abac = null;
		try {
//			conf = new Configuration();
			
			
	
//			conf.set("mapred.child.java.opts", "-server -Xmx1024m -Djava.net.preferIPv4Stack=true");
			conf.set(DaaSConstants.JOB_CONFIG_PARM_OWNERSHIP_FILTER, owshFltr);
			conf.set(DaaSConstants.JOB_CONFIG_PARM_STORE_FILTER, filterOnStoreId);
			//AWS START
			//conf.set("dfs.umaskmode", "002");
			//AWS END
			
			
			//AWS START
			//hdfsFileSystem = FileSystem.get(conf);
			hdfsFileSystem = HDFSUtil.getFileSystem(daasConfig, conf);
			//AWS END
			
			job = new Job(conf, "Generate TDA Format STLD Header/Detail");

			System.out.println("  createJobDetails 3 " + createJobDetails);
			if("TRUE".equalsIgnoreCase(createJobDetails)){
				abac = new ABaC(daasConfig);
				jobGroupId = abac.createJobGroup(DaaSConstants.TDA_EXTRACT_JOBGROUP_NAME);
				
				jobId = abac.createJob(jobGroupId, jobSeqNbr, job.getJobName());
				}
//			for (Path addSubDir : inputDirPathList) {
//				FileInputFormat.addInputPath(job, addSubDir);
//			}
//			
			
//			FileInputFormat.addInputPath(job, new Path(inputRootDir+File.separator+"STLD*"));
			
			Path cachePath = new Path(cacheFile);
			
			System.out.println("CACHE PATH->" + cachePath.toString());
			
			job.setJarByClass(GenerateTdaFormatStld.class);
			job.setMapperClass(NpStldXmlMapper.class);
			if(generatFieldsForNielsen.equalsIgnoreCase("TRUE")){
				job.setReducerClass(NpStldXmlReducer.class);
				job.setNumReduceTasks(62);
				
			}else{
				job.setNumReduceTasks(0);
//				job.setOutputKeyClass(NullWritable.class);
			}
			
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			
			//AWS START
			String[] cp = cacheFile.split(",");
			
			for (String cpi:cp) {
				Path ncp = new Path(cpi);
				DistributedCache.addCacheFile(ncp.toUri(),job.getConfiguration());
			}

			//DistributedCache.addCacheFile(cachePath.toUri(),
			//		job.getConfiguration());
			//AWS END 
			
//			job.addCacheFile(new URI(cachePath.toString() + "#" + cachePath.getName()));
//			job.addCacheFile(new URI(cachePath.toString() + "#" + cachePath.getName()));
			
//			String[] cachefilestrs = cacheFile.split(",");
//			Path[] cachefilepaths  = new Path[cachefilestrs.length];
//			int i =0;
//			for(String cachepathstr:cachefilestrs){
////				cachefilepaths[i++] = new Path(cachepathstr);
//				System.out.println( "adding " +cachepathstr +" to dist cache");
//				DistributedCache.addCacheFile(new Path(cachepathstr).toUri(),
//						job.getConfiguration());
//			}
			
//			MultipleOutputs.addNamedOutput(job, "HDR", TextOutputFormat.class,
//					Text.class, Text.class);
//			MultipleOutputs.addNamedOutput(job, "DTL", TextOutputFormat.class,
//					Text.class, Text.class);
//			
			MultipleOutputs.addNamedOutput(job, "NEWTDACODEVALUES", TextOutputFormat.class,
					Text.class, Text.class);
			
			FileStatus[] fstatus = null;
			FileStatus[] fstatustmp = null;
//			String[] inputpathstrs = inputRootDir.split(",");
		
			int totalInputFileCount = 0;
			String[] inputpathstrs = inputRootDir.split(",");
			
			
			
			
			for(String inputpaths:inputpathstrs){
//				System.out.println(" processing inputpaths  " +inputpaths);
				
//				fstatustmp = hdfsFileSystem.listStatus(new Path(inputpaths),new STLDPathFilter());
				fstatustmp = hdfsFileSystem.globStatus(new Path(inputpaths+"/STLD*"));
				fstatus = (FileStatus[])ArrayUtils.addAll(fstatus, fstatustmp);
	
			}
			String filepath;
			String datepart;
			String terrCdDatepart;
			HashSet<String> terrCdDtset = new HashSet<String>();
			String[] fileNameParts;
			for(FileStatus fstat:fstatus){
				String fileName = fstat.getPath().getName().toUpperCase();
				
				String fileNamePartsDelimiter = "~";
				
				if(fileName.indexOf("RXD126") > 0){
					fileNamePartsDelimiter = "RXD126";
				}
				fileNameParts = fileName.split(fileNamePartsDelimiter);
				String terrCdfrmFileName = fileNameParts[1];
				
//				if(fileName.startsWith("STLDRXD126840RXD126") || fileName.startsWith("STLD~840~") ||
//						fileName.startsWith("STLDRXD126702RXD126") || fileName.startsWith("STLD~702~")){
				
				if(validTerrCdSet != null && validTerrCdSet.contains(Integer.parseInt(terrCdfrmFileName))){
					FileInputFormat.addInputPath(job, fstat.getPath());
					
					totalInputFileCount++;
					
					datepart = fileNameParts[2].substring(0,8);
					terrCdDtset.add(terrCdfrmFileName+DaaSConstants.SPLCHARTILDE_DELIMITER+datepart);
					
//					filepath = fstat.getPath().toString().toUpperCase();
//					int lastindx = filepath.lastIndexOf("~");
//					if(lastindx == -1){
//						lastindx = filepath.lastIndexOf("-R-");
//						datepart = filepath.substring(lastindx-8,lastindx);
////						System.out.println(" aadding datepart  to set "+ datepart);
//						dtset.add(datepart);
//					}
				 
				}
				}
			
				if(! (totalInputFileCount > 0)){
					System.out.println(" There are no input files to process; exiting");
					System.exit(1);
				}
				Iterator<String> it = terrCdDtset.iterator();
			
				while(it.hasNext()){
					terrCdDatepart = it.next();
					System.out.println(" addding " +terrCdDatepart);
					MultipleOutputs.addNamedOutput(job,"HDR"+DaaSConstants.SPLCHARTILDE_DELIMITER+terrCdDatepart,TextOutputFormat.class, Text.class, Text.class);
					MultipleOutputs.addNamedOutput(job,"PMIX"+DaaSConstants.SPLCHARTILDE_DELIMITER+terrCdDatepart,TextOutputFormat.class, Text.class, Text.class);
				}
				
			

//			hdfsOutputPath = new Path(outputDir + File.separator + "stld");
			hdfsOutputPath = new Path(outputDir );

			FileOutputFormat.setOutputPath(job, hdfsOutputPath);
//			hdfsFileSystem = FileSystem.get(hdfsOutputPath.toUri(), conf);

			if (hdfsFileSystem.exists(hdfsOutputPath)) {
				hdfsFileSystem.delete(hdfsOutputPath, true);
				Logger.getLogger(GenerateTdaFormatStld.class.getName()).log(
						Level.INFO,
						"Removed existing output path = " + outputDir);
			}
			
	
			//this prevents the creation of part* files when using MultiOutputformat
			LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
			
			
			if(propertiesstr != null && !propertiesstr.isEmpty()){
				
//				System.out.println(" propertiesstr : "+propertiesstr);
				
				String[] propsArray = propertiesstr.trim().split("\\|",-1);
				
//				System.out.println( "propsArray length " + propsArray.length);
				
				String key;
				String val;
				String[] keyval = new String[2];
				
				for(String property:propsArray){
					
				
					keyval = property.split(":",-1);
					
					key = keyval[0];
					val = keyval[1];
					
					job.getConfiguration().set(key, val);
				}
			}
			
			job.getConfiguration().set("generatFieldsForNielsen", generatFieldsForNielsen);
			int retCode = 0;
			retCode = job.waitForCompletion(true) ? 0 : 1;
			
			
//			handleMissingCodes("/daastest/oozieclienttest/oozieworkflow/lib/TDA_CODE_VALUES.psv",outputDir+"/NEWTDACODEVALUES-m-00000","/daastest/oozieclienttest/oozieworkflow/lib/TDA_CODE_VALUES.psv",hdfsFileSystem);
			
			String tdaCodePath= "";
			String[] cachefilestrs = cacheFile.split(",");
			for(String cachepathstr:cachefilestrs){
				if(cachepathstr.contains("TDA_CODE_VALUES.psv"))
					tdaCodePath = cachepathstr;
			}
			
			handleMissingCodes(tdaCodePath,outputDir,tdaCodePath,hdfsFileSystem);
			
			if("TRUE".equalsIgnoreCase(createJobDetails)){
				abac.closeJob(jobId, DaaSConstants.JOB_SUCCESSFUL_ID, DaaSConstants.JOB_SUCCESSFUL_CD);
			}
			
//			String ooziePropFile = System.getProperty(DaaSConstants.OOZIE_ACTION_OUTPUT_PROPERTIES);
//			if(ooziePropFile != null){
//				File propFile = new File(ooziePropFile);
//				
//				Properties props = new Properties();
//				props.setProperty(DaaSConstants.LAST_JOB_GROUP_ID, ""+jobGroupId);
//				props.setProperty(DaaSConstants.LAST_JOB_SEQ_NBR, ""+jobSeqNbr);
//				
//				OutputStream outputStream = new FileOutputStream(propFile);
//				props.store(outputStream, "custom props");
//				outputStream.close();
//				
//			}
//			FsPermission newFilePremission = new FsPermission(FsAction.READ_WRITE,FsAction.READ_EXECUTE,FsAction.READ_EXECUTE);
//			hdfsFileSystem.setPermission(new Path(outputDir+"/*"),newFilePremission);
			return retCode;

		} 
		
		catch (InterruptedException ex) {
			Logger.getLogger(GenerateTdaFormatStld.class.getName()).log(
					Level.SEVERE, null, ex);
			throw ex;
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(GenerateTdaFormatStld.class.getName()).log(
					Level.SEVERE, null, ex);
			throw ex;
		} 
		catch (Exception ex) {
			Logger.getLogger(GenerateTdaFormatStld.class.getName()).log(
					Level.SEVERE, null, ex);
			throw ex;
		}finally{
			if(abac != null)
				abac.dispose();
//			if(hdfsFileSystem != null)
//				hdfsFileSystem.close();
		}

	
	}
	
private void handleMissingCodes(String sourceHDFSPath1,String sourceHDFSPath2,String destHDFSPath,FileSystem hdfsFileSystem) throws Exception{
		
		
		
		BufferedReader bufferedReader = null;
		BufferedWriter bw = null;
		
		try{
			Path srcPath1 = new Path(sourceHDFSPath1);
			Path srcPath2 = new Path(sourceHDFSPath2);
			Path destPath = new Path(destHDFSPath);
			
			Path srcPath1new = srcPath1;
			
			FileStatus[] fs = hdfsFileSystem.listStatus(srcPath2,new PathFilter() {
				
				@Override
				public boolean accept(Path listpath) {
					if(listpath.toString().contains("NEWTDACODEVALUES"))
						return true;
					return false;
				}
			});
			
			if(fs == null || fs.length == 0)
				return;
			
			int numofbytesread = 0;
			
			if(sourceHDFSPath1 !=null && sourceHDFSPath1.equalsIgnoreCase(destHDFSPath)){
				
				srcPath1new = new Path(sourceHDFSPath1+"old");
				hdfsFileSystem.rename(srcPath1, srcPath1new);
				
			}
				
			
			
			bufferedReader = new BufferedReader(new InputStreamReader(hdfsFileSystem.open(srcPath1new)));
			
			
			bw = new BufferedWriter(new OutputStreamWriter(hdfsFileSystem.create(new Path(destHDFSPath))));
			
			String line;
			while ( (line = bufferedReader.readLine()) != null){
				
				bw.write(line+ "\n");
			
			}
			bufferedReader.close();
			
			
			
			HashSet<String> missingCdKey = new HashSet<String>();
			String[] keyparts;
			for(int i=0;i<fs.length;i++){
				bufferedReader = new BufferedReader(new InputStreamReader(hdfsFileSystem.open(fs[i].getPath())));
			
				while ( (line = bufferedReader.readLine()) != null){
					
					keyparts = line.split("\\|");
					if(!missingCdKey.contains(keyparts[0])){
						missingCdKey.add(keyparts[0]);
						bw.write(line+ "\n");
					}
					
					
				}
			}
			
			
			
			if(sourceHDFSPath1 !=null && sourceHDFSPath1.equalsIgnoreCase(destHDFSPath)){
				hdfsFileSystem.delete(srcPath1new, false);
				
				hdfsFileSystem.rename(destPath, srcPath1);
			}
			
			
			
		}catch(Exception ex){
//			ex.printStackTrace();
			throw ex;
		}finally{
			try{
				
				if(bufferedReader != null)
					bufferedReader.close();
				if(bw != null)
					bw.close();
					
				
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

}
