package nl.siwoc.moviejukebox.plugin;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.SearchEngineTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;

import nl.siwoc.moviejukebox.plugin.model.MovieDetails;
import nl.siwoc.moviejukebox.plugin.model.MovieDetails.Actor;
import nl.siwoc.moviejukebox.plugin.model.SearchMovieResult;

public class MovieMeterRestPlugin extends ImdbPlugin {

	private static String MOVIEMETER_API_KEY = PropertiesUtil.getProperty("API_KEY_MovieMeter");
	private static final String MOVIEMETER_REST_BASE_URL = "http://www.moviemeter.nl/api/film/";
	public static final String MOVIEMETER_REST_PLUGIN_ID = "movieMeterRest";
	private static final Logger LOGGER = Logger.getLogger(MovieMeterRestPlugin.class);
	private static final String LOG_MESSAGE = "MovieMeterRestPlugin 0.9.4: ";
	private static final String USER_AGENT = "";

	private SearchEngineTools searchEngineTools;

	private static final String ID_TYPE = "moviemeter";

	final ObjectMapper mapper = new ObjectMapper();
	
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
		Movie movie = new Movie();
		movie.setTitle("Brave", "main");
		movie.setYear("2012", "main");
		System.out.println(" id " + plugin.getMovieId(movie));

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
		String moviemeterId = movie.getId(ID_TYPE);
		if (!StringUtils.isNumeric(moviemeterId))
		{
			try {
				moviemeterId = getMovieId(searchMovie(movie), movie);
				if (moviemeterId != null) {
					movie.setId(ID_TYPE, moviemeterId);
				}
			} catch (Exception e) {
				LOGGER.debug(LOG_MESSAGE + "Unable to search movie " + movie.getTitle() ,e);
			}
		}
		return moviemeterId;
	}

	public ArrayList<SearchMovieResult> searchMovie(Movie movie) throws Exception {
		LOGGER.debug(LOG_MESSAGE + "Searching id for title : " + movie.getTitle() + " year: " + movie.getYear());
		String query = createQuery(movie);
		ArrayList<SearchMovieResult> result = new ArrayList<SearchMovieResult>();
		HttpURLConnection conn = null;
		
		// call moviemeter api
		try {
			URL url = new URL(MOVIEMETER_REST_BASE_URL + "?q=" + query + "&api_key=" + MOVIEMETER_API_KEY);
			LOGGER.debug("HTTP search call: " + url);
			conn = (HttpURLConnection) url.openConnection();
			conn.addRequestProperty("User-Agent", USER_AGENT);
			conn.setRequestMethod("GET");
			if (conn.getResponseCode() != 200) {
				if (conn.getResponseCode() == 404) {
					return result;
				}
				throw new Exception("Failed : HTTP error code : " + conn.getResponseCode());
			}
			
			result = new ArrayList<SearchMovieResult>(Arrays.asList(mapper.readValue(conn.getInputStream(), SearchMovieResult[].class)));
			
		} catch (Exception e) {
			throw e;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return result;
		
	}
	
	private String createQuery(Movie movie) {
		try {
			return URLEncoder.encode(movie.getTitle(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// never occurs, UTF-8 is hardcoded
			throw new RuntimeException(e);
		}
	}

	public String getMovieId(ArrayList<SearchMovieResult> searchMovieResultSet, Movie movie)
	{
		String moviemeterId = null;
		if (searchMovieResultSet != null)
		{
			// only 1 film found, assume that name is correct and year doesn't matter
			if (searchMovieResultSet.size() == 1) {
				moviemeterId = String.valueOf(searchMovieResultSet.get(0).getId());
				LOGGER.debug(LOG_MESSAGE + "Found ONLY id by name: " + moviemeterId);
				return moviemeterId;
			}
			
			// multiple films found, filter on year
			int yearToFind = Integer.valueOf(movie.getYear());
			ArrayList<SearchMovieResult> haveYearResults = new ArrayList<SearchMovieResult>();
			for (SearchMovieResult result : searchMovieResultSet) {
				if (result.getYear() == yearToFind) {
					haveYearResults.add(result);
				}
			}
			if (haveYearResults.size() == 1) {
				// 1 result with requested year
				LOGGER.debug(LOG_MESSAGE + "Found ONLY id by part of name with correct year: " + haveYearResults.get(0).getId());
				return String.valueOf(haveYearResults.get(0).getId());
			} else {
				// multipe films with searchterm in the name and requested year (like Wolf (2013) or Brave (2012))
				// find name equality
				for (SearchMovieResult hasYear : haveYearResults) {
					if (matchTitle(hasYear,movie)) {
						LOGGER.debug(LOG_MESSAGE + "Found id by part of name with correct year: " + hasYear.getId());
						return String.valueOf(hasYear.getId());
					}
				}
			}
		}
		return moviemeterId;
	}

	private boolean matchTitle(SearchMovieResult searchMovieResult, Movie movie) {
		String searchTitle = searchMovieResult.getTitle().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
		String movieTitle = movie.getTitle().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
		if (searchTitle.equals(movieTitle)) {
			return true;
		}
		if (searchMovieResult.getAlternative_title() != null) {
			searchTitle = searchMovieResult.getAlternative_title().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
			if (searchTitle.equals(movieTitle)) {
				return true;
			}
		}
		return false;
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
			MovieDetails filmInfo = getDetailsFromApi(movie);
			if (filmInfo == null) {
				return false;
			}
			movie.setId(ID_TYPE, String.valueOf(filmInfo.getId()));
			if (filmInfo.getImdb() != null)
			{
				movie.setId("imdb", filmInfo.getImdb());
				LOGGER.debug(LOG_MESSAGE + "Fetched imdb id: " + movie.getId("imdb"));
			}
			
			// title
			// start with Dutch title
			String title = filmInfo.getAlternative_title();
			if (title == null) {
				title = filmInfo.getTitle();
			}
			if (title != null)
			{
				if (OverrideTools.checkOverwriteTitle(movie, ID_TYPE))
				{
					movie.setTitle(title, ID_TYPE);
					LOGGER.debug(LOG_MESSAGE + "Fetched title: " + movie.getTitle());
				}
				if (OverrideTools.checkOverwriteOriginalTitle(movie, ID_TYPE)) {
					movie.setOriginalTitle(title, ID_TYPE);
				}
			}
			
			// average=rating
			if (movie.getRating() == -1)
			{
				
				LOGGER.debug(LOG_MESSAGE + "Average parsed: " + filmInfo.getAverage());
				int rating = (int) Math.round(filmInfo.getAverage() * 20.0F);
				movie.addRating(ID_TYPE, rating);
				LOGGER.debug(LOG_MESSAGE + "Added rating: " + rating);
			}
			
			// duration=runtime
			int duration = filmInfo.getDuration();
			if (OverrideTools.checkOverwriteRuntime(movie, ID_TYPE))
			{
				movie.setRuntime(String.valueOf(duration), ID_TYPE);
				LOGGER.debug(LOG_MESSAGE + "Fetched runtime: " + movie.getRuntime());
			}
			
			/*
			// country
			ArrayList<String> countries = filmInfo.getCountries();
			if (OverrideTools.checkOverwriteCountry(movie, ID_TYPE) 
					&& countries != null
					&& countries.size() > 0)
			{
				movie.setCountry(StringUtils.join(countries, ", "), ID_TYPE);
				LOGGER.debug(LOG_MESSAGE + "Fetched country: " + movie.getCountry());
			}
			*/
			
			// genres
			ArrayList<String> genres = filmInfo.getGenres();
			if (OverrideTools.checkOverwriteGenres(movie, ID_TYPE) 
					&& genres != null
					&& genres.size() > 0)
			{
				movie.setGenres(genres, ID_TYPE);
				LOGGER.debug(LOG_MESSAGE + "Fetched genres: " + StringUtils.join(movie.getGenres(), ", "));
			}
			
			// plot
			String plot = filmInfo.getPlot();
			if (OverrideTools.checkOverwritePlot(movie, ID_TYPE) && plot != null) {
				movie.setPlot(plot, ID_TYPE);
			}
			
			// year
			Integer year = filmInfo.getYear();
			if (OverrideTools.checkOverwriteYear(movie, ID_TYPE) && year != null)
			{
				movie.setYear(year.toString(), ID_TYPE);
				LOGGER.debug(LOG_MESSAGE + "Fetched year: " + movie.getYear());
			}
			
			// actors
			// do we use addActor or setCast?
			ArrayList<Actor> actors = filmInfo.getActors();
			if (OverrideTools.checkOverwriteActors(movie, ID_TYPE) && actors != null
					&& actors.size() > 0)
			{
				ArrayList<String> newActors = new ArrayList<String>();
				for (Actor actor : actors) {
					newActors.add(actor.getName());
				}
				movie.setCast(newActors, ID_TYPE);
				LOGGER.debug(LOG_MESSAGE + "Fetched actors: " + StringUtils.join(movie.getCast(), ", "));
			}
			
			// directors
			ArrayList<String> directors = filmInfo.getDirectors();
			if (OverrideTools.checkOverwriteDirectors(movie, ID_TYPE) && directors != null 
					&& directors.size() > 0) 
			{
				movie.setDirectors(directors, ID_TYPE);
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
		if (StringTools.isValidString(movie.getId(ID_TYPE))) {
			return true;
		}
		LOGGER.debug(LOG_MESSAGE + "Scanning NFO for Moviemeter id");
		int beginIndex = nfo.indexOf("www.moviemeter.nl/film/");
		if (beginIndex != -1)
		{
			StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 23), "/ \n,:!&é\"'(--è_çà)=$");
			movie.setId(ID_TYPE, st.nextToken());
			LOGGER.debug(LOG_MESSAGE + "Moviemeter id found in NFO = " + movie.getId(ID_TYPE));
			return true;
		}
		LOGGER.debug(LOG_MESSAGE + "No Moviemeter id found in NFO : " + movie.getTitle());
		return false;
	}

	private MovieDetails getDetailsFromApi(Movie movie) throws Exception {
		String moviemeterId = movie.getId(ID_TYPE);
		MovieDetails movieDetails = null;
		HttpURLConnection conn = null;
		
		if (moviemeterId == null) {
			return null;
		}
		
		// call moviemeter api
		try {
			URL url = new URL(MOVIEMETER_REST_BASE_URL + moviemeterId + "?api_key=" + MOVIEMETER_API_KEY);
			LOGGER.debug("HTTP details call: " + url);
			conn = (HttpURLConnection) url.openConnection();
			conn.addRequestProperty("User-Agent", USER_AGENT);
			conn.setRequestMethod("GET");
			if (conn.getResponseCode() != 200) {
				if (conn.getResponseCode() == 404) {
					return movieDetails;
				}
				throw new Exception("Failed : HTTP error code : " + conn.getResponseCode());
			}
			
			movieDetails = mapper.readValue(conn.getInputStream(), MovieDetails.class);
			
		} catch (Exception e) {
			throw e;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return movieDetails;
	}

}
