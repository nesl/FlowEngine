package edu.ucla.nesl.datacollector;

public class Const {
	public static final String TAG = "DataCollector";

	public static final String FLOW_ENGINE_APPLICATION_SERVICE = "edu.ucla.nesl.flowengine.FlowEngine.application";

	public static final String SETUP_MODE = "setup_mode";
	public static final int SETUP_USER = 1;
	public static final int SETUP_BLUETOOTH = 2;
	
	public static final String PREFS_NAME = "data_collector";
	public static final String PREFS_IS_FIRST = "is_first";
	
	public static final String PREFS_SERVER_IP = "server_ip";
	public static final String PREFS_USERNAME = "username";
	public static final String PREFS_PASSWORD = "password";

	public static final String PROPERTY_FILE_NAME = "flowengine.properties";

	public static final Object PROP_NAME_ZEPHYR_ADDR = "zephyr_bluetooth_addr";
	public static final Object PROP_NAME_METAWATCH_ADDR = "metawatch_bluetooth_addr";
}
