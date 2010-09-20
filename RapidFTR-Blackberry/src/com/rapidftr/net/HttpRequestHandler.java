package com.rapidftr.net;

import java.io.IOException;

import javax.microedition.io.HttpConnection;

import com.sun.me.web.request.Arg;
import com.sun.me.web.request.PostData;
import com.sun.me.web.request.RequestListener;
import com.sun.me.web.request.Response;

public class HttpRequestHandler implements RequestListener {

	RequestCallBack requestCallBack;
	private int unprocessedRequests = 0;
	private int totalRequests = 0;

	private HttpService service;

	public HttpRequestHandler(HttpService httpService) {
		service = httpService;
	}

	public void get(String url, Arg[] inputArgs, Arg[] httpArgs, Object context) {
		incrementActiveRequests();
		service.get(url, inputArgs, httpArgs, this, context);
	}

	public void post(String url, Arg[] postArgs, Arg[] httpArgs,
			PostData postData, Object context) {
		incrementActiveRequests();
		service.post(url, postArgs, httpArgs, this, postData, context);
	}

	public void put(String url, Arg[] postArgs, Arg[] httpArgs,
			PostData postData, Object context) {
		incrementActiveRequests();
		service.put(url, postArgs, httpArgs, this, postData, context);
	}

	//sync request
	public Response get(String url, Arg[] inputArgs, Arg[] httpArgs)
			throws IOException {
		Response response = service.get(url, inputArgs, httpArgs);
		if (isValidResponse(response)) {
			return response;
		} else {
			handleResponseErrors(response);
			return null;
		}
	}

	private boolean isValidResponse(Response response) {
		return (response.getException() == null)
				&& (response.getCode() == HttpConnection.HTTP_OK || response
						.getCode() == HttpConnection.HTTP_CREATED);
	}

	public void handleResponseErrors(Response response) {
		if (response.getException() != null) {
			requestCallBack.handleException(response.getException());
		} else if (response.getCode() == HttpConnection.HTTP_UNAUTHORIZED) {
			requestCallBack.handleUnauthorized();
		} else if (response.getCode() != HttpConnection.HTTP_OK
				&& response.getCode() != HttpConnection.HTTP_CREATED) {
			requestCallBack.handleConnectionProblem();
		}
	}

	public void readProgress(Object context, int bytes, int total) {
		// updateRequestProgress(bytes, total);
	}

	public void writeProgress(Object context, int bytes, int total) {
		requestCallBack.writeProgress(context, bytes, total);

	}

	public void updateRequestProgress(int bytes, int total) {
		double size = ((double) bytes) / total;
		requestCallBack.updateRequestProgress((int) (size * 100));
	}

	public void markProcessComplete() {
		unprocessedRequests=totalRequests=0;
		requestCallBack.onProcessComplete();
	}

	public void markProcessFailed() {
		requestCallBack.onProcessFail();
	}

	public void cancelRequestInProgress() {
		service.cancelRequest();
		unprocessedRequests = totalRequests = 0;
	}

	public RequestCallBack getRequestCallBack() {
		return requestCallBack;
	}

	private void incrementActiveRequests() {
		unprocessedRequests += 1;
		totalRequests += 1;
	}

	public boolean isProcessCompleted() {
		return unprocessedRequests == 0;
	}

	public void setRequestCallBack(RequestCallBack requestCallBack) {
		this.requestCallBack = requestCallBack;
	}
	
	
	public void done(Object context, Response response) {
		if (unprocessedRequests > 0) {
			unprocessedRequests--;
		}
		updateRequestProgress(totalRequests - unprocessedRequests, totalRequests);

		if (isValidResponse(response)) {
			requestCallBack.onSuccess(context, response);
		} else {
			handleResponseErrors(response);
		}

		if (isProcessCompleted()) {
			markProcessComplete();
		}
	}

}
