package nl.siwoc.moviejukebox.plugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.SearchEngineTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;

public class MovieMeterRestPlugin extends ImdbPlugin {

	private enum ResponseType {
		SEARCH,
		DETAIL
	}
	
	private class HasYear {

		public String id;
		public String title;
		public String alternative_title;
		
		HasYear(String idIn, String titleIn, String alternativeIn) {
			id = idIn;
			title = titleIn;
			alternative_title = alternativeIn;
		}
	}

	private static String MOVIEMETER_API_KEY = PropertiesUtil.getProperty("API_KEY_MovieMeter");
	private static final String MOVIEMETER_REST_BASE_URL = "http://www.moviemeter.nl/api/film/";
	public static final String MOVIEMETER_REST_PLUGIN_ID = "movieMeterRest";
	private static final Logger LOGGER = Logger.getLogger(MovieMeterRestPlugin.class);
	private static final String LOG_MESSAGE = "MovieMeterRestPlugin 0.9.2: ";

	private SearchEngineTools searchEngineTools;

	public static void main(String[] args) {
		if (MOVIEMETER_API_KEY == null) {
			MOVIEMETER_API_KEY = "tyk0awf19uqm65mjfsqw9z9rx6t706pe";			
		}
		MovieMeterRestPlugin plugin = new MovieMeterRestPlugin();
		/*
		System.out.println("soof 2013 id " + plugin.getMovieId("soof","2013"));
		System.out.println("Wolf 2013 id " + plugin.getMovieId("Wolf","2013"));
		HashMap<String, Object> filmInfo = plugin.getMovieDetailsById("5208");
		System.out.println("imdb " + filmInfo.get("imdb"));
		filmInfo = plugin.getMovieDetailsById(filmInfo.get("imdb").toString());
		System.out.println("title " + filmInfo.get("title"));
		System.out.println("furious 2001 id " + plugin.getMovieId("furious","2001"));
		System.out.println("Independence_Day 2001 id " + plugin.getMovieId("Independence Day","1996"));
		HashMap<String, Object> soof = plugin.getMovieDetailsById("90303");
		System.out.println("title " + soof.get("title"));
		*/
		System.out.println(" id " + plugin.getMovieId("Brave","2012"));

	}

	public static void setMOVIEMETER_API_KEY(String mOVIEMETER_API_KEY) {
		MOVIEMETER_API_KEY = mOVIEMETER_API_KEY;
	}

	public MovieMeterRestPlugin()
	{
		this.searchEngineTools = new SearchEngineTools("nl");
	}

	public String getPluginID()
	{
		return MOVIEMETER_REST_PLUGIN_ID;
	}

	public String getMovieId(Movie movie)
	{
		String moviemeterId = movie.getId("moviemeter");
		if (!StringUtils.isNumeric(moviemeterId))
		{
			moviemeterId = getMovieId(movie.getTitle(), movie.getYear());
			movie.setId("moviemeter", moviemeterId);
		}
		return moviemeterId;
	}

	public String getMovieId(String title, String yearToFind)
	{
		LOGGER.debug(LOG_MESSAGE + "Searching id for title : " + title + " year: " + yearToFind);
		String moviemeterId = "UNKNOWN";
		HashMap<String, Object> basicFilmInfoSet = null;
		try {
			String encodedTitle = URLEncoder.encode(title, "UTF-8");
			basicFilmInfoSet = getMovieInfoByUrl(MOVIEMETER_REST_BASE_URL + "?q=" + encodedTitle + "&api_key=" + MOVIEMETER_API_KEY, ResponseType.SEARCH);
			if (basicFilmInfoSet != null)
			{
				// only 1 film found, assume that name is correct and year doesn't matter
				if (basicFilmInfoSet.size() == 1) {
					@SuppressWarnings("unchecked")
					HashMap<String,Object> basicFilmInfo = (HashMap<String, Object>) basicFilmInfoSet.values().toArray()[0];
					moviemeterId = basicFilmInfo.get("id").toString();
					LOGGER.debug(LOG_MESSAGE + "Found ONLY id by name: " + moviemeterId);
					if (StringUtils.isNumeric(moviemeterId)) {
						return moviemeterId;
					}
				}
				
				// multiple films found, filter on year
				Set<String> keys = basicFilmInfoSet.keySet();
				HashSet<HasYear> haveYearResults = new HashSet<HasYear>();
				for (String key : keys) {
					@SuppressWarnings("unchecked")
					HashMap<String,Object> basicFilmInfo = (HashMap<String, Object>) basicFilmInfoSet.get(key);
					// find year in set
					String foundYear = String.valueOf(basicFilmInfo.get("year"));
					if (foundYear != null && foundYear.equals(yearToFind)) {
						String hasYearId = basicFilmInfo.get("id").toString();
						HasYear hasYear = new HasYear(hasYearId, (String) basicFilmInfo.get("title"), (String) basicFilmInfo.get("alternative_title"));
						{
							haveYearResults.add(hasYear);
						}
						LOGGER.debug(LOG_MESSAGE + "Found id by part of name with correct year: " + hasYearId);
					}
				}
				if (haveYearResults.size() == 1) {
					String hasYearId = ((HasYear) haveYearResults.toArray()[0]).id;
					LOGGER.debug(LOG_MESSAGE + "Found ONLY id by part of name with correct year: " + hasYearId);
					if (StringUtils.isNumeric(hasYearId)) {
						return hasYearId;
					}
				} else {
					// multipe films with searchterm in the name and requested year
					// find name equality
					for (HasYear hasYear : haveYearResults) {
						if (hasYear.title.equalsIgnoreCase(title) || (hasYear.alternative_title != null && hasYear.alternative_title.equals(title))) {
							LOGGER.debug(LOG_MESSAGE + "Found id by part of name BEST MATCH with correct year: " + hasYear.id);
							if (StringUtils.isNumeric(hasYear.id)) {
								return hasYear.id;
							}
						}
					}
					
				}
			}
			String url = this.searchEngineTools.searchMovieURL(title, yearToFind, "www.moviemeter.nl/film");
	
			int beginIndex = url.indexOf("www.moviemeter.nl/film/");
			if (beginIndex != -1)
			{
				StringTokenizer st = new StringTokenizer(url.substring(beginIndex + 23), "/\"");
				moviemeterId = st.nextToken();
				if (StringUtils.isNumeric(moviemeterId)) {
					return moviemeterId;
				}
			}
		} catch (UnsupportedEncodingException error) {
			LOGGER.debug(LOG_MESSAGE + "Unable to URLEncode title : " + title);
			LOGGER.error(SystemTools.getStackTrace(error));
		} catch (RuntimeException error) {
			LOGGER.error(LOG_MESSAGE + "Unable to retrieve info for : " + title + " (HTTP)");
			LOGGER.error(SystemTools.getStackTrace(error));
		} catch (Exception error) {
			LOGGER.error(LOG_MESSAGE + "Unable to retrieve info for : " + title);
			LOGGER.error(SystemTools.getStackTrace(error));
		}
		return "UNKNOWN";
	}

	public boolean scan(Movie movie)
	{
		String moviemeterId = getMovieId(movie);
		if (!StringUtils.isNumeric(moviemeterId))
		{
			LOGGER.debug(LOG_MESSAGE + "Moviemeter id not available : " + movie.getTitle());
			return false;
		}
		LOGGER.debug(LOG_MESSAGE + "Moviemeter id available (" + moviemeterId + "), updating media info");

		LOGGER.debug(LOG_MESSAGE + "Start fetching info from moviemeter.nl: " + moviemeterId);
		try
		{
			HashMap<String, Object> filmInfo = getMovieDetailsById(moviemeterId);
			if ((filmInfo == null) || (filmInfo.isEmpty())) {
				return false;
			}
			movie.setId("moviemeter", filmInfo.get("id").toString());
			if (filmInfo.get("imdb") != null)
			{
				movie.setId("imdb", filmInfo.get("imdb").toString());
				LOGGER.debug(LOG_MESSAGE + "Fetched imdb id: " + movie.getId("imdb"));
			}
			
			// title
			// start with Dutch title
			String title = (String) filmInfo.get("alternative_title");
			if (title == null) {
				title = (String) filmInfo.get("title");
			}
			if (title != null)
			{
				if (OverrideTools.checkOverwriteTitle(movie, "moviemeter"))
				{
					movie.setTitle(title, "moviemeter");
					LOGGER.debug(LOG_MESSAGE + "Fetched title: " + movie.getTitle());
				}
				if (OverrideTools.checkOverwriteOriginalTitle(movie, "moviemeter")) {
					movie.setOriginalTitle(title, "moviemeter");
				}
			}
			
			// average=rating
			if (movie.getRating() == -1)
			{
				
				LOGGER.debug(LOG_MESSAGE + "Average parsed: " + filmInfo.get("average") + " class: " + filmInfo.get("average").getClass());
				Double average = Double.valueOf(filmInfo.get("average").toString());
				if (average != null) {
					int rating = (int) Math.round(average * 20.0F);
					movie.addRating("moviemeter", rating);
					LOGGER.debug(LOG_MESSAGE + "Added rating: " + rating);
				}
			}
			
			/* don't know if the REST api returns this
			if (OverrideTools.checkOverwriteReleaseDate(movie, "moviemeter"))
			{
				Object[] dates = (Object[])filmInfo.get("dates_cinema");
				if ((dates != null) && (dates.length > 0))
				{
					HashMap dateshm = (HashMap)dates[0];
					movie.setReleaseDate(dateshm.get("date").toString(), "moviemeter");
					LOGGER.debug(LOG_MESSAGE + "Fetched releasedate: " + movie.getReleaseDate());
				}
			}
			*/
			
			// duration=runtime
			Integer duration = (Integer) filmInfo.get("duration");
			if ((OverrideTools.checkOverwriteRuntime(movie, "moviemeter")) && 
					(duration != null))
			{
				movie.setRuntime(duration.toString(), "moviemeter");
				LOGGER.debug(LOG_MESSAGE + "Fetched runtime: " + movie.getRuntime());
			}
			
			// country
			@SuppressWarnings("unchecked")
			ArrayList<String> countries = (ArrayList<String>) filmInfo.get("countries");
			if (OverrideTools.checkOverwriteCountry(movie, "moviemeter") && countries != null)
			{
				movie.setCountry(StringUtils.join(countries, ", "), "moviemeter");
				LOGGER.debug(LOG_MESSAGE + "Fetched country: " + movie.getCountry());
			}
			
			// genres
			@SuppressWarnings("unchecked")
			ArrayList<String> genres = (ArrayList<String>) filmInfo.get("genres");
			if (OverrideTools.checkOverwriteGenres(movie, "moviemeter") && genres != null)
			{
				movie.setGenres(genres, "moviemeter");
				LOGGER.debug(LOG_MESSAGE + "Fetched genres: " + StringUtils.join(movie.getGenres(), ", "));
			}
			
			// plot
			String plot = (String) filmInfo.get("plot");
			if (OverrideTools.checkOverwritePlot(movie, "moviemeter") && plot != null) {
				movie.setPlot(plot, "moviemeter");
			}
			
			// year
			Integer year = (Integer) filmInfo.get("year");
			if (OverrideTools.checkOverwriteYear(movie, "moviemeter") && year != null)
			{
				movie.setYear(year.toString(), "moviemeter");
				LOGGER.debug(LOG_MESSAGE + "Fetched year: " + movie.getYear());
			}
			
			// actors
			// do we use addActor or setCast?
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String,Object>> actors = (ArrayList<HashMap<String,Object>>) filmInfo.get("actors");
			if (OverrideTools.checkOverwriteActors(movie, "moviemeter") && actors != null) {
				ArrayList<String> newActors = new ArrayList<String>();
				for (int i = 0; i < actors.size(); i++) {
					newActors.add((String) actors.get(i).get("name"));
				}
				movie.setCast(newActors, "moviemeter");
				LOGGER.debug(LOG_MESSAGE + "Fetched actors: " + StringUtils.join(movie.getCast(), ", "));
			}
			
			// directors
			@SuppressWarnings("unchecked")
			ArrayList<String> directors = (ArrayList<String>)filmInfo.get("directors");
			if (OverrideTools.checkOverwriteDirectors(movie, "moviemeter") && directors != null && directors.size() > 0) {
				movie.setDirectors(directors, "moviemeter");
				LOGGER.debug(LOG_MESSAGE + "Fetched director: " + movie.getDirector());
			}
			
			if (this.downloadFanart && StringTools.isNotValidString(movie.getFanartURL()))
			{
				movie.setFanartURL(getFanartURL(movie));
				if (StringTools.isValidString(movie.getFanartURL())) {
					movie.setFanartFilename(movie.getBaseName() + this.fanartToken + "." + this.fanartExtension);
				}
			}
			return true;
		}
		catch (Exception error)
		{
			LOGGER.error(LOG_MESSAGE + "Failed retrieving media info : " + moviemeterId);
			LOGGER.error(SystemTools.getStackTrace(error));
		}
		return false;
	}

	public boolean scanNFO(String nfo, Movie movie)
	{
		if (StringTools.isValidString(movie.getId("moviemeter"))) {
			return true;
		}
		LOGGER.debug(LOG_MESSAGE + "Scanning NFO for Moviemeter id");
		int beginIndex = nfo.indexOf("www.moviemeter.nl/film/");
		if (beginIndex != -1)
		{
			StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 23), "/ \n,:!&é\"'(--è_çà)=$");
			movie.setId("moviemeter", st.nextToken());
			LOGGER.debug(LOG_MESSAGE + "Moviemeter id found in NFO = " + movie.getId("moviemeter"));
			return true;
		}
		LOGGER.debug(LOG_MESSAGE + "No Moviemeter id found in NFO : " + movie.getTitle());
		return false;
	}

	/**
	 * @param id can be either moviemeter-id or imdb-id
	 * @return HashMap containing movie details
	 */
	private HashMap<String, Object> getMovieDetailsById(String id) {
		return getMovieInfoByUrl(MOVIEMETER_REST_BASE_URL + id + "?api_key=" + MOVIEMETER_API_KEY, ResponseType.DETAIL);
	}

	@SuppressWarnings("unchecked")
	private HashMap<String, Object> getMovieInfoByUrl(String urlString, ResponseType type) {

		HashMap<String,Object> result = null;

		try {
			LOGGER.debug(LOG_MESSAGE + "Using url: " + urlString);
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			//conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				if (conn.getResponseCode() == 404) {
					// no info found, return null
					LOGGER.debug(LOG_MESSAGE + "MovieMeter returned HTTP 404, so no info found");
					return result;
				}
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			ObjectMapper mapper = new ObjectMapper();
			if (type == ResponseType.SEARCH) {
				// Search returns "no films found" or basicFilmInfoArray
				JsonFactory f = new JsonFactory();
				JsonParser jp = f.createParser(conn.getInputStream());
				// advance stream to START_ARRAY first:
				JsonToken startToken = jp.nextToken();
				if (startToken == JsonToken.START_ARRAY) {
					result = new HashMap<String,Object>();
					// and then each time, advance to opening START_OBJECT
					while (jp.nextToken() == JsonToken.START_OBJECT) {
						HashMap<String, Object> basicFilmInfo = mapper.readValue(jp, HashMap.class);
						String id = basicFilmInfo.get("id").toString();
						result.put(id, basicFilmInfo);
						// after binding, stream points to closing END_OBJECT
					}
				}
			} else {
				result = mapper.readValue(conn.getInputStream(), HashMap.class);
				// check on message means "no films found", probably returned HTTP 404, so already returned null
				if (result.containsKey("message")) {
					result = null;
				}
			}

			conn.disconnect();

		} catch (MalformedURLException error) {
			LOGGER.error(LOG_MESSAGE + "Failed retrieving media info : " + urlString);
			LOGGER.error(SystemTools.getStackTrace(error));
		} catch (IOException error) {
			LOGGER.error(LOG_MESSAGE + "Failed retrieving media info : " + urlString);
			LOGGER.error(SystemTools.getStackTrace(error));
		}		
		return result;
	}


}
