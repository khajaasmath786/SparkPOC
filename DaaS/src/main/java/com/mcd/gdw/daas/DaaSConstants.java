package com.mcd.gdw.daas;

import java.text.SimpleDateFormat;

public class DaaSConstants {
	public static final String OOZIE_ACTION_OUTPUT_PROPERTIES = "oozie.action.output.properties";
	public static final String LAST_JOB_SEQ_NBR = "LAST_JOB_SEQ_NBR";
	public static final String TDA_EXTRACTS_INPUT_PATH = "TDA_EXTRACTS_INPUT_PATH"; 
	public static final String TDA_EXTRACTS_OUTPUT_BASEPATH = "TDA_EXTRACTS_OUTPUT_BASEPATH"; 

	//AWS START
	public static final String HDFS_ROOT_CONFIG = "daas.job.hdfsroot";
	//AWS END

	public static final String JOB_CONFIG_PARM_OWNERSHIP_FILTER = "daas.job.ownership_filter";
	public static final String JOB_CONFIG_PARM_STORE_FILTER = "daas.job.store_filter";
	public static final String JOB_CONFIG_PARM_ABAC_FROM_PATH = "daas.job.abac_from_path";
	public static final String JOB_CONFIG_PARM_ABAC_TO_PATH = "daas.job.abac_to_path";
	public static final String JOB_CONFIG_PARM_ABAC_TO_REJECT_PATH = "daas.job.abac_to_reject_path";
	public static final String JOB_CONFIG_PARM_COPY_OR_MOVE_FILES  = "daas.job.copy_or_move_files";
	
	public static final String LAST_JOB_GROUP_ID = "LAST_JOB_GROUP_ID";
	
	public static final  short JOB_SUCCESSFUL_ID = (short)1;
	public static final  short JOB_FAILURE_ID = (short)2;
	public static final  String JOB_SUCCESSFUL_CD  = "SUCCESSFUL";
	public static final  String JOB_FAILURE_CD  = "FAILED";
	
	public final static int XML_REC_FILE_TYPE_POS = 0;
	public final static int XML_REC_POS_BUSN_DT_POS = 1;
	public final static int XML_REC_DW_FILE_ID_POS = 2;
	public final static int XML_REC_MCD_GBAL_LCAT_ID_NU_POS = 3;
	public final static int XML_REC_TERR_CD_POS = 4;
	public final static int XML_REC_LGCY_LCL_RFR_DEF_CD_POS = 5;
	public final static int XML_REC_REST_OWSH_TYP_SHRT_DS_POS = 6;
	public final static int XML_REC_XML_TEXT_POS = 7;
	
	
	public final static String TDA_EXTRACT_JOBGROUP_NAME	 ="Generate TDA Extracts";
	public final static String DATAHUB_EXTRACT_JOBGROUP_NAME ="Generate TLD New DataHub Extracts";
	
	public static SimpleDateFormat SDF_yyyyMMddHHmmssSSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public static SimpleDateFormat SDF_yyyyMMdd = new SimpleDateFormat("yyyyMMdd");
	public static SimpleDateFormat SDF_yyyy_MM_dd = new SimpleDateFormat("yyyyMMdd");
	public static SimpleDateFormat SDF_yyyyMMddHHmmssSSSnodashes = new SimpleDateFormat("yyyyMMddHHmmss");
	
	
	public static String PIPE_DELIMITER  = "|";
	public static String COMMA_DELIMITER  = ",";
	public static String TAB_DELIMITER   = "\t";
	public static String UNDSCR_DELIMITER   = "\t";
	public static String TILDE_DELIMITER   = "~";
	public static String SPLCHARTILDE_DELIMITER   = "RxD126";
	public static String SPLCHARUNDERSCORE_DELIMITER   = "RxD095";
	public static String SPLCHARHYPHEN_DELIMITER="RxD045";
	
		
	
}
