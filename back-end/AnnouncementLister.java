package org.fhcrc.centernet.components; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import com.adobe.cq.sightly.WCMUse;
import com.day.cq.wcm.api.Page;
import com.day.cq.search.QueryBuilder;
import javax.jcr.Session;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.result.Hit;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.fhcrc.centernet.Constants;

/**
 * @author ischempp
 */

public class AnnouncementLister extends WCMUse {
	final static Logger log = LoggerFactory.getLogger(AnnouncementLister.class);
	final String DEBUG_PREFIX = "ANNOUNCEMENT LISTER: ";
	final String PN_ON_TIME = "@jcr:content/onTime";
	final String PN_OFF_TIME = "@jcr:content/offTime";
	final String PN_TITLE = "jcr:title";
	final String TEXT_NODE_NAME = "textimage";
	final String HOMEPAGE_PATH = "/content/centernet/en";
	
	private ResourceResolver resResolver;
	private ValueMap properties;
	private List<Map<String, String>> pinList;
	private List<Map<String, String>> remainderList;
	private Page currentPage;
	
	/**
	 * Activate AnnouncementLister Class. Gets the current ResourceResolver and
	 * the Component Properties for use in the class methods.
	 */
	@Override
	public void activate() throws java.lang.Exception {
		
		resResolver = getResourceResolver();
		properties = getProperties();
		currentPage = getCurrentPage();
		pinList = createPinnedList();
		remainderList = createRemainderList();
		
	}
	
	/* True if there are no Pinned Announcements AND no Remainder 
	 * Announcements. False otherwise. */
	public Boolean isEmpty() {
		
		if (getPinnedListLength() + getRemainderListLength() > 0) {
			return false;
		} else {
			return true;
		}
		
	}
	
	/* True if the current page is the CenterNet Homepage. False otherwise. */
	public Boolean isHomepage() {
		
		if (currentPage.getPath().equals(HOMEPAGE_PATH)) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public List<Map<String, String>> getPinnedList() {
		
		return pinList;
		
	}
	
	public List<Map<String, String>> getRemainderList() {
		
		return remainderList;
		
	}
	
	/*
	 * Returns an Integer representing the number of Pinned Announcements
	 * placed in the Pinned List. Returns 0 if Pinned List is null.
	 */
	public Integer getPinnedListLength() {
		
		if (pinList != null) {
			return pinList.size();
		} else {
			return 0;
		}
		
	}
	
	/*
	 * Returns an Integer representing the number of Remainder Announcements
	 * placed in the Remainder List. Returns 0 if Remainder List is null.
	 */
	public Integer getRemainderListLength() {
		
		if (remainderList != null) {
			return remainderList.size();
		} else {
			return 0;
		}
		
	}
	
	/*
	 * Creates a list of Maps that can be iterated through using data-sly-list.
	 * This list contains all announcements that have been selected by the user
	 * to be displayed at the top of the announcement lister.
	 */
	private List<Map<String, String>> createPinnedList() {
		
		ArrayList<Map<String, String>> tempList = new ArrayList<Map<String, String>>();
		String[] paths = properties.get("pinnedAnnouncements", String[].class);
		
		if (paths != null && paths.length > 0) {
		
			/* Iterate through the provided paths, turn them into Pages, and 
			 * extract the relevant data from them */
			Iterator<String> pathIterator = Arrays.asList(paths).iterator();
			while (pathIterator.hasNext()) {
				
				String p = pathIterator.next();
				
				Resource r = resResolver.resolve(p);
				if (r != null) {
					
					Page page = r.adaptTo(Page.class);
					/* There used to be a check here on page template to make
					 * sure the selected page was indeed an Announcement, but
					 * that doesn't work in the publish enivronment*/
					if(!page.getProperties().get("cq:template","").equals(Constants.ANNOUNCEMENT_TEMPLATE)) {
						
						/* If the page is not an Announcement, skip it */
						continue;
						
					}
					if (!page.isValid()) {
						
						/* If the Announcement is not valid, skip it */
						continue;
						
					} else {
					
						addPageToList(page, tempList);
						
					}
					
				}
				
			}
			
		}
		
		return tempList;
		
	}
	
	/*
	 * Creates a list of Maps that can be iterated through using data-sly-list.
	 * This list contains all valid announcements that are not already
	 * contained in the Pinned List.
	 */
	private List<Map<String, String>> createRemainderList() {
		
		ArrayList<Map<String, String>> tempList = new ArrayList<Map<String, String>>();
		Map<String, String> queryMap = createQueryMap();
		Session session = resResolver.adaptTo(Session.class);
		QueryBuilder qb = resResolver.adaptTo(QueryBuilder.class);
		
		/* Execute the Query to find all valid announcements */
		Iterator<Hit> hits = qb.createQuery(PredicateGroup.create(queryMap), session).getResult().getHits().iterator();
		
		while (hits.hasNext()) {
			
			try {
				
				Page page = hits.next().getResource().adaptTo(Page.class);
				
				if (page != null) {
				/* If this page is in the pinned list, then skip it */
					if (isDuplicate(page)) {
						
						continue;
						
					} else {
					
						addPageToList(page, tempList);
						
					}
					
				}
				
			} catch(Exception e) {
				
				log.error(DEBUG_PREFIX + "Error adapting hit to Page", e);
				
			}
			
		}
		
		return tempList;
		
	}
	
	/**
	 * A function that looks through the list of pinned announcements and tests
	 * whether the Page sent as an argument is in that list already
	 * @param Page p - We will check whether this Page is already represented 
	 * in the pinList
	 * @return true if this Page is already in the pinList. false otherwise.
	 */
	private Boolean isDuplicate(Page p) {
		
		/* If there is no Pinned List, then there can be no duplicates */
		if (getPinnedListLength() == 0) {
			
			return false;
			
		} else {
			
			Iterator<Map<String, String>> pinnedIterator = pinList.iterator();
			while (pinnedIterator.hasNext()) {
				
				Map<String, String> testMap = pinnedIterator.next();
				
				/* If the path of our page equals the path of an item in the 
				 * Pinned List, then they represent the same announcement */
				if (p.getPath().equals(testMap.get("path"))) {
					
					return true;
					
				}
				
			}
			
			/* If we haven't returned true by the end of the while loop, then
			 * there are no duplicates in the Pinned List */
			return false;
			
		}
		
	}
	
	private void addPageToList(Page page, List<Map<String, String>> list) {
		
		Map<String, String> map = new HashMap<String, String>();
		/* The four properties we need to output in our Sightly code */
		String title = new String(),
				isTargetBlank = new String(),
				linkURL = new String(),
				text = new String();
		
		ValueMap pageProperties = page.getProperties();
		Resource textNode = page.getContentResource().getChild(TEXT_NODE_NAME);
		title = page.getTitle();
		linkURL = pageProperties.get("linkURL","");
		isTargetBlank = pageProperties.get("isTargetBlank","");
		/* If there is a text node, get the "text" property */
		if (textNode != null) {
			text = textNode.getValueMap().get("text","");
		} else {
			text = "";
		}
		
		/* Put all the relevant data in the Map */
		map.put("title", title);
		map.put("linkURL", linkURL);
		map.put("isTargetBlank", isTargetBlank);
		map.put("text", text);
		/* Needed to compare for duplicates in the isDuplicate function */
		map.put("path", page.getPath());
		
		/* Add this map to our List */
		list.add(map);
		
	}
	
	/*
	 * A function to create a HashMap that can be used to create a 
	 * PredicateGroup for the purposes of executing a QueryBuilder Query.
	 * 
	 * This particular Map should return all nodes of resourceType cq:Page
	 * that exist in the Announcements tree of the CenterNet site. Only pages
	 * whose On Time in the past and Off Time in the future are found.
	 */
	private HashMap<String,String> createQueryMap() {
	
		HashMap<String, String> map = new HashMap<String, String>();
		
		// Only include Pages with the CenterNet Announcement template
		map.put("type","cq:Page");
		map.put("path", Constants.ANNOUNCEMENTS);
		map.put("property","jcr:content/cq:template");
	    map.put("property.value", Constants.ANNOUNCEMENT_TEMPLATE);
	    
	    // Only include assets whose On Time is in the past
	    map.put("1_relativedaterange.property", PN_ON_TIME);
	    map.put("1_relativedaterange.upperBound", "0");
	    
	    // Only include assets whose Off Time is in the future
	    map.put("2_relativedaterange.property", PN_OFF_TIME);
	    map.put("2_relativedaterange.lowerBound", "0");
	    
	    // Include all hits
	    map.put("p.limit", "-1");
	    map.put("p.guessTotal", "true");
	    
	    /* Order by onTime - puts items with the most recent onTime at the top */
	    map.put("orderby", "@jcr:content/onTime");
	    map.put("orderby.sort", "desc");
	    
		return map;
		
	}
	
}
