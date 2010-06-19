package com.rapidftr.utilities;

import java.util.Hashtable;

import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;

public class SettingsStore {
	
	private static final String DEFAULT_HOST = "defaulthost.foo.org";

	private static final String DEFAULT_USERNAME = "default.user";

	private static final long KEY = "com.rapidftr.utilities.ftrstore".hashCode();

	private static final String KEY_LAST_USED_USERNAME = "Last_Used_Username"; 
	private static final String KEY_LAST_USED_HOST = "Last_Used_Host"; 

	private final PersistentObject persistentObject = PersistentStore.getPersistentObject(KEY);

	private Hashtable contents;
    private static final String AUTHORISATION_TOKEN = "Authorisation_Token";

    public SettingsStore() {
		loadContentsHashtable();
	}

	private void loadContentsHashtable() {
		if (persistentObject.getContents() == null) {
			persistentObject.setContents(new Hashtable());
		}
		
		Object contentsObject = (Hashtable)persistentObject.getContents();
		if (!(contentsObject instanceof Hashtable)) {
			persistentObject.setContents(new Hashtable());
		}
		contents = (Hashtable) contentsObject;
	}
	
	public String getLastUsedLoginUsername() {
		return getString(KEY_LAST_USED_USERNAME, DEFAULT_USERNAME);
	}

	public String getLastUsedLoginHost() {
		return getString(KEY_LAST_USED_HOST, DEFAULT_HOST);
	}
	

	public void setLastUsedUsername(String value) {
		setString(KEY_LAST_USED_USERNAME, value);
	}
	
	public void setLastUsedHost(String value) {
		setString(KEY_LAST_USED_HOST, value);
	}

	private String getString(String key, String def) {
		if (contents.containsKey(key)) {
			return "" + contents.get(key);
		} else {
			return def;
		}
	}
	
	private void setString(String key, String value) {
		if (value == null) {
			value = "";
		}
		
		contents.put(key, value);
		persistentObject.commit();
	}

    public void setAuthorisationToken(String authorisationToken) {
        setString(AUTHORISATION_TOKEN, authorisationToken);
    }
}
