"use strict";

/**
 * Announcement Lister component JS backing script
 */
use(function () {
    
 var CONST = {
  WRAPPER_CSS_CLASS: "fh-component-announcementlister",
  ANNOUNCEMENT_LANDING_PAGE_PATH: "/content/centernet/en/announcements.html",
  DEPT_NEWS_LANDING_PAGE_PATH: "/content/centernet/en/department-news.html",
  PROP_WIDGET_TYPE: "widgetType",
  ANNOUNCEMENT_FORM_URL: "/content/centernet/en/f/announcement",
  EMPTY_ANNOUNCEMENT_HEADLINE: "Have an announcement to post?",
  EMPTY_ANNOUNCEMENT_TEXT: "Have news for the entire Hutch community? Submit an announcement for the CenterNet home page and the Communications team will post it."
 };

 var announcementLister = {};
 
 announcementLister.CONST = CONST;

 return announcementLister;
    
});
