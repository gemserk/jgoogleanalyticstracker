/**
 * Copyright (c) 2010 Daniel Murphy
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * Created at Jul 20, 2010, 4:04:22 AM
 */
package com.dmurph.tracking;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Common tracking calls are implemented as methods, but if you want to control what data to send,
 * then use {@link #makeCustomRequest(AnalyticsRequestData)}. If you are making custom calls, the only
 * requirements are:
 * <ul><li>If you are tracking an event, {@link AnalyticsRequestData#setEventCategory(String)} and
 * 			{@link AnalyticsRequestData#setEventAction(String)} must both be populated.</li>
 * 	   <li>If you are not tracking an event, {@link AnalyticsRequestData#setPageURL(String)} must
 * 		   be populated</li></ul>
 * See the <a href=http://code.google.com/apis/analytics/docs/tracking/gaTrackingTroubleshooting.html#gifParameters>
 * Google Troubleshooting Guide</a> for more info on the tracking parameters.
 * @author Daniel Murphy
 *
 */
public class JGoogleAnalyticsTracker {
	public static boolean DEBUG_PRINT = false;
	private static final ThreadGroup asyncThreadGroup = new ThreadGroup("Async Google Analytics Threads");
	static {
		asyncThreadGroup.setMaxPriority(Thread.MIN_PRIORITY);
	}
	
	public static enum GoogleAnalyticsVersion{
		V_4_7_2
	};
	
	private GoogleAnalyticsVersion gaVersion = null;
	private IGoogleAnalyticsURLBuilder builder = null;
	private AnalyticsConfigData configData = null;
	private boolean enabled = false;
	private boolean asynchronous = true;

	public JGoogleAnalyticsTracker(AnalyticsConfigData argConfigData, GoogleAnalyticsVersion argVersion){
		gaVersion = argVersion;
		configData = argConfigData;
		createBuilder();
		enabled = true;
	}
	
	/**
	 * Set if the requests are asynchronous (true by default).
	 * @param asynchronous
	 */
	public void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}

	/**
	 * If the requests are asynchronous (true by default).
	 * @return
	 */
	public boolean isAsynchronous() {
		return asynchronous;
	}
	
	/**
	 * Resets the session cookie.
	 */
	public void resetSession(){
		builder.resetSession();
	}
	
	/**
	 * Sets if the api dispatches tracking requests.
	 * @param argEnabled
	 */
	public void setEnabled(boolean argEnabled){
		enabled = argEnabled;
	}
	
	/**
	 * If the api is dispatching tracking requests (default of true).
	 * @return
	 */
	public boolean isEnabled(){
		return enabled;
	}

	/**
	 * Tracks a page view.
	 * @param argPageURL required, Google won't track without it.  Ex: <code>"org/me/javaclass.java"</code>,
	 * 		  or anything you want as the page url.
	 * @param argPageTitle content title
	 * @param argHostName the host name for the url
	 */
	public void trackPageView(String argPageURL, String argPageTitle, String argHostName){
		trackPageViewFromReferrer(argPageURL, argPageTitle, argHostName, "http://www.dmurph.com", "/");
	}
	
	/**
	 * Tracks a page view.
	 * @param argPageURL required, Google won't track without it.  Ex: <code>"org/me/javaclass.java"</code>,
	 * 		  or anything you want as the page url.
	 * @param argPageTitle content title
	 * @param argHostName the host name for the url
	 * @param argReferrerSite site of the referrer.  ex, www.dmurph.com
	 * @param argReferrerPage page of the referrer.  ex, /mypage.php
	 */
	public void trackPageViewFromReferrer(String argPageURL, String argPageTitle, String argHostName, String argReferrerSite, String argReferrerPage){
		if(argPageURL == null){
			throw new IllegalArgumentException("Page URL cannot be null, Google will not track the data.");
		}
		AnalyticsRequestData data = new AnalyticsRequestData();
		data.setHostName(argHostName);
		data.setPageTitle(argPageTitle);
		data.setPageURL(argPageURL);
		data.setReferer(argReferrerSite, argReferrerPage);
		makeCustomRequest(data);
	}
	
	/**
	 * Tracks a page view.
	 * @param argPageURL required, Google won't track without it.  Ex: <code>"org/me/javaclass.java"</code>,
	 * 		  or anything you want as the page url.
	 * @param argPageTitle content title
	 * @param argHostName the host name for the url
	 * @param argSearchSource source of the search engine.  ex: google
	 * @param argSearchKeywords the keywords of the search. ex: java google analytics tracking utility
	 */
	public void trackPageViewFromSearch(String argPageURL, String argPageTitle, String argHostName, String argSearchSource, String argSearchKeywords){
		if(argPageURL == null){
			throw new IllegalArgumentException("Page URL cannot be null, Google will not track the data.");
		}
		AnalyticsRequestData data = new AnalyticsRequestData();
		data.setHostName(argHostName);
		data.setPageTitle(argPageTitle);
		data.setPageURL(argPageURL);
		data.setSeachReferrer(argSearchSource, argSearchKeywords);
		makeCustomRequest(data);
	}
	
	/**
	 * Tracks an event.  To provide more info about the page, use
	 * {@link #makeCustomRequest(AnalyticsRequestData)}.
	 * @param argCategory
	 * @param argAction
	 */
	public void trackEvent(String argCategory, String argAction){
		trackEvent(argCategory, argAction, null, null);
	}
	
	/**
	 * Tracks an event.  To provide more info about the page, use
	 * {@link #makeCustomRequest(AnalyticsRequestData)}. 
	 * @param argCategory
	 * @param argAction
	 * @param argLabel
	 */
	public void trackEvent(String argCategory, String argAction, String argLabel){
		trackEvent(argCategory, argAction, argLabel, null);
	}
	
	/**
	 * Tracks an event.  To provide more info about the page, use
	 * {@link #makeCustomRequest(AnalyticsRequestData)}.
	 * @param argCategory required
	 * @param argAction required
	 * @param argLabel optional
	 * @param argValue optional
	 */
	public void trackEvent(String argCategory, String argAction, String argLabel, Integer argValue){
		AnalyticsRequestData data = new AnalyticsRequestData();
		data.setEventCategory(argCategory);
		data.setEventAction(argAction);
		data.setEventLabel(argLabel);
		data.setEventValue(argValue);
		
		makeCustomRequest(data);
	}
	
	/**
	 * Makes a custom tracking request based from the given data.
	 * @param argData
	 * @throws NullPointerException if argData is null or if the URL builder is null
	 */
	public synchronized void makeCustomRequest(AnalyticsRequestData argData){
		if(!enabled){
			if(DEBUG_PRINT){
				System.out.println("Ignoring tracking request, enabled is true");
			}
			return;
		}
		if(argData == null){
			throw new NullPointerException("Data cannot be null");
		}
		if(builder == null){
			throw new NullPointerException("Class was not initialized");
		}
		final String url = builder.buildURL(argData);
		
		if(asynchronous){
			Thread t = new Thread(asyncThreadGroup, "AnalyticThread-"+asyncThreadGroup.activeCount()){
				public void run() {
					dispatchRequest(url);
				}
			};
			t.start();
		}else{
			dispatchRequest(url);
		}
	}
	
	private void dispatchRequest(String argURL){
		try {
			URL url = new URL(argURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setInstanceFollowRedirects(true);
			connection.connect();
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				System.err.println("JGoogleAnalyticsTracker: Error requesting url '" + argURL+"', received response code "+responseCode);
			} else {
				if(DEBUG_PRINT){
					System.out.println("JGoogleAnalyticsTracker: Tracking success for url '"+argURL+"'");
				}
			}
		} catch (Exception e) {
			System.err.println("Error making tracking request");
			e.printStackTrace();
		}
	}
	
	private void createBuilder(){
		switch (gaVersion) {
			case V_4_7_2:
				builder = new GoogleAnalyticsV4_7_2(configData);
				break;
			default:
				builder = new GoogleAnalyticsV4_7_2(configData);
				break;
		}
	}
}
