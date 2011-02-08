package com.rapidftr.model;

import java.util.*;

import com.rapidftr.utilities.*;
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.io.http.HttpDateParser;
import net.rim.device.api.math.Fixed32;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.util.Persistable;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import com.sun.me.web.request.Arg;
import com.sun.me.web.request.Part;
import com.sun.me.web.request.PostData;

public class Child implements Persistable {

    public static final String CREATED_AT_KEY = "created_at";
	private final Hashtable data;
	private final Hashtable changedFields;

	private ChildStatus childStatus;

	public Child() {
		changedFields = new Hashtable();
		data = new Hashtable();
		put("_id", RandomStringGenerator.generate(32));
		put(CREATED_AT_KEY, getCurrentFormattedDateTime());
		childStatus = ChildStatus.NEW;
	}

    public void setHistories(String histories) {
        setField("histories", histories);
    }

    protected String getCurrentFormattedDateTime() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ssz").format(cal);
    }

	public String toFormatedString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		for (Enumeration keyList = data.keys(); keyList.hasMoreElements();) {
			Object key = keyList.nextElement();
			Object value = data.get(key);
			buffer.append(key + ":" + value + ",");
		}
		buffer.append("]");
		return buffer.toString();
	}

	public String toString() {
		return (String) data.get("name");
	}

	public PostData getPostData() {

		Vector parts = new Vector();
		Enumeration keyList = isNewChild() ? data.keys() : getChangedFields();

		while (keyList.hasMoreElements()) {
			Object key = keyList.nextElement();
			Object value = data.get(key);

            if (key.equals("histories")) {
                continue;
            }

			if (key.equals("current_photo_key")) {
				if (!StringUtility.isBlank(String.valueOf(value))) {
					parts.addElement(multiPart(value, "photo",
							HttpUtility.HEADER_CONTENT_TYPE_IMAGE));
				}
				continue;
			}
			if (key.equals("recorded_audio")) {
				if (value != null
						&& !StringUtility.isBlank(String.valueOf(value))) {
					parts.addElement(multiPart(value, "audio",
							HttpUtility.HEADER_CONTENT_TYPE_AUDIO));
				}
				continue;
			}

			Arg[] headers = new Arg[1];
			headers[0] = new Arg("Content-Disposition",
					"form-data; name=\"child[" + key + "]\"");
			Part part = new Part(value.toString().getBytes(), headers);
			parts.addElement(part);
		}

		Part[] anArray = new Part[parts.size()];
		parts.copyInto(anArray);
		String boundary = "abced";

		PostData postData = new PostData(anArray, boundary);
		return postData;
	}

	private Enumeration getChangedFields() {
		return changedFields.keys();
	}

	private Part multiPart(Object value, String paramName, Arg headerContentType) {
		Arg[] headers = new Arg[2];
		headers[0] = new Arg("Content-Disposition", "form-data; name=\"child["
				+ paramName + "]\"");
		headers[1] = headerContentType;
		byte[] imageData;
		imageData = FileUtility.getByteArray(value.toString());

		return new Part(imageData, headers);
	}

	public void setField(String name, Object value) {
		if (!isNewChild()) {
			Object oldValue = getField(name);

			if (oldValue != null && !oldValue.equals(value)
					&& !name.equals("_id")) {
				changedFields.put(name, value);
			}
		}
		put(name, value);

	}

	private void put(String name, Object value) {
		if (value != null) {
			data.put(name, value);
		}
	}

	public Object getField(String key) {
		return data.get(key);
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((data == null) ? 0 : data.get("_id").hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Child other = (Child) obj;
		if (data == null) {
			if (other.data != null) {
				return false;
			}
		} else {
			if (data.get("_id") != null && other.data.get("_id") != null
					&& data.get("_id").equals((other.data.get("_id")))) {
				return true;
			}
			if (data.get("unique_identifier") != null
					&& other.data.get("unique_identifier") != null
					&& data.get("unique_identifier").equals(
							(other.data.get("unique_identifier")))) {
				return true;
			}
		}

		return false;
	}

	public void updateField(String name) {
		if (!data.containsKey(name)) {
			put(name, "");
		}

	}

	public static Child create(Vector forms) {
        Child child = new Child();
		child.updateChildDetails(forms);
        return child;
	}

	public void updateChildDetails(Vector forms) {
		for (Enumeration list = forms.elements(); list.hasMoreElements();) {

			Object nextElement = list.nextElement();
			if (nextElement != null) {
				Form form = (Form) nextElement;
				for (Enumeration fields = form.getFieldList().elements(); fields
						.hasMoreElements();) {

					Object nextFormfieldElement = fields.nextElement();
					if (nextFormfieldElement != null) {

						FormField field = (FormField) nextFormfieldElement;

						setField(field.getName(), field.getValue());
					}
				}
			}
		}
		setField("last_updated_at", getCurrentFormattedDateTime());
	}

	public void update(String userName, Vector forms) {
		this.updateChildDetails(forms);
		if (isUpdated()) {
			childStatus = ChildStatus.UPDATED;
		}
	}

    public Vector getHistory() {
        return getHistory(TimeZone.getDefault());
    }

	public Vector getHistory(TimeZone timeZone) {
		Vector historyLogs = new Vector();
		try {
			Object JsonHistories = getField("histories");
			if (JsonHistories != null) {
				JSONArray histories = new JSONArray(JsonHistories.toString());
				for (int i = 0; i < histories.length(); i++) {
					JSONObject history = null;
					history = histories.getJSONObject(i);
					JSONObject changes = history.getJSONObject("changes");
					Enumeration changedFields = changes.keys();
					while (changedFields.hasMoreElements()) {
						String changedFieldName = (String) changedFields
								.nextElement();
						JSONObject changedFieldObject = changes
								.getJSONObject(changedFieldName);
						String changeDateTime = toLocalTime(history.getString("datetime"), timeZone);
						String oldValue = changedFieldObject.getString("from");
						String newalue = changedFieldObject.getString("to");
                        String description = "";
						if (oldValue.equals("")) {
							description = changeDateTime + " "
									+ changedFieldName + " intialized to "
									+ newalue + " By "
									+ history.getString("user_name");
						} else {
							description = changeDateTime + " "
									+ changedFieldName + " changed from "
									+ oldValue + " to " + newalue + " By "
									+ history.getString("user_name");

						}
                        historyLogs.addElement(new ChildHistoryItem(history.getString("user_name"), description));
					}

				}
			}
		} catch (JSONException e) {
			throw new RuntimeException("Invalid  History Format"
					+ e.getMessage());
		}

		return historyLogs;
	}

    protected String toLocalTime(String dateTime, TimeZone timeZone) {
        long dateTimeValue = HttpDateParser.parse(dateTime);
        // HttpDateParser.parse returns 0 when it can't parse correctly
        // This is Jan 1 1970 - which is not a valid date in this change history context
        if (dateTimeValue == 0) {
            return dateTime;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(dateTimeValue));
        cal.setTimeZone(timeZone);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal);
    }

    public boolean hasChangesByOtherThan(String username) {
        Enumeration logs = getHistory().elements();
        while (logs.hasMoreElements()) {
            if (!username.equals(((ChildHistoryItem)logs.nextElement()).getUsername())) {
                return true;
            }
        }
        return false;
    }

	public boolean isNewChild() {
		return getField("unique_identifier") == null;
	}

	public boolean isUpdated() {
		return changedFields.size() > 0;
	}

	public ChildStatus childStatus() {
		return childStatus;
	}

	public void syncSuccess() {
		changedFields.clear();
		childStatus = ChildStatus.SYNCED;
	}

	public void syncFailed(String message) {
		ChildStatus status = ChildStatus.SYNC_FAILED;
		status.setSyncError(message);
		childStatus = status;
	}

	public boolean isSyncFailed() {
		return childStatus.equals(ChildStatus.SYNC_FAILED);
	}

	public boolean matches(String queryString) {
		String id = (String) getField("unique_identifier");
		if (id == null) {
			id = "";
		}

		String name = (String) getField("name");
		if (name == null) {
			name = "";
		}

		return ((!"".equals(queryString) && (id.equalsIgnoreCase(queryString))) || (!""
				.equals(queryString) && name.toString().toLowerCase().indexOf(
				queryString) != -1));
	}

	public Bitmap getImage() {
        return getScaledImage(300, 300, "res/head.png");
	}

    private Bitmap getScaledImage(int width, int height, String defaultImage) {
        String imageLocation = (String) getField("current_photo_key");
        EncodedImage fullSizeImage = ImageUtility.getBitmapImageForPath(imageLocation);

        Bitmap bitmap;
        if(fullSizeImage != null)
        {
            int requiredWidth = Fixed32.toFP(width);
            int requiredHeight = Fixed32.toFP(height);
            bitmap =ImageUtility.scaleImage(fullSizeImage, requiredWidth, requiredHeight);
        }
        else
        {
            bitmap = Bitmap.getBitmapResource(defaultImage);
        }

        return bitmap;
    }

    public Bitmap getThumbnail(){
        return getScaledImage(60,60,"res/thumb.png");
    }

    public String getCreatedBy() {
        return (String) getField("created_by");
    }
}
