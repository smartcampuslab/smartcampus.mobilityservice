package eu.trentorise.smartcampus.mobility.controller.rest;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;

import com.google.common.collect.Lists;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedOutput;

import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.model.Announcements;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;

@Controller
public class AnnouncementController {

	@Autowired
	private DomainStorage storage;
	
	@Autowired
	@Value("${feed.entries}")
	private Integer feedEntries = null;	

	@RequestMapping(method = RequestMethod.GET, value = "/announcements/news/{appId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody Announcements getNews(@PathVariable String appId, @RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer limit, HttpServletResponse response) throws Exception {
		return getAnnouncement("news", appId, skip, limit);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/announcements/notifications/{appId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody Announcements getNotifications(@PathVariable String appId, @RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer limit, HttpServletResponse response) throws Exception {
		return getAnnouncement("notification", appId, skip, limit);
	}	

	@RequestMapping(method = RequestMethod.GET, value = "/announcements/rss/{appId}")
	public void getRSS(@PathVariable String appId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String url = request.getRequestURL().toString();
		
		Document doc = generateRSS(getAnnouncement("news", appId, 0, feedEntries).getAnnouncement(), appId, url);

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult console = new StreamResult(response.getOutputStream());
		transformer.transform(source, console);
		
		response.setHeader("Content-Type", "application/rss+xml");
	}
	
	private Announcements getAnnouncement(String type, String appId, Integer skip, Integer limit) {
		Criteria criteria = new Criteria(type).is(true).and("appId").is(appId);
		Query query = new Query(criteria).with(new Sort(Sort.Direction.DESC, "timestamp"));
				if (skip != null) {
					query = query.skip(skip);
				}
				if (limit != null) {
					query = query.limit(limit);
				}

		List<Announcement> msgs = storage.searchDomainObjects(query, Announcement.class);
		
		Announcements result = new Announcements();
		result.setAnnouncement(msgs);
		return result;
	}	

	private Document generateRSS(List<Announcement> msgs, String appId, String url) throws Exception {
		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("rss_2.0");
		feed.setTitle("Mobility News for  " + StringUtils.capitalize(appId));
		feed.setLink(url);
		feed.setDescription(feed.getTitle());

		List<SyndEntry> entries = Lists.newArrayList();
		SyndEntry entry;
		SyndContent content;

		for (Announcement msg : msgs) {
			entry = new SyndEntryImpl();
			entry.setTitle(msg.getTitle());

			entry.setPublishedDate(new Date(msg.getTimestamp()));

			content = new SyndContentImpl();
			content.setType("text/html");
			content.setValue(msg.getHtml());

			entry.setDescription(content);
			
			List<Element> extensions = Lists.newArrayList();

			if (msg.getDescription() != null) {
			Element el1 = new Element("textDescription");
			el1.addContent(msg.getDescription());
			extensions.add(el1);
			}
			if (msg.getAgencyId() != null) {
			Element el1 = new Element("agencyId");
			el1.addContent(msg.getAgencyId());
			extensions.add(el1);
			}
			if (msg.getRouteId() != null) {
			Element el1 = new Element("routeId");
			el1.addContent(msg.getRouteId());
			extensions.add(el1);
			}
	        
			if (!extensions.isEmpty()) {
				entry.setForeignMarkup(extensions);
			}

			entries.add(entry);
		}

		feed.setEntries(entries);

		SyndFeedOutput output = new SyndFeedOutput();

		return output.outputW3CDom(feed);

	}
}