package nl.siwoc.moviejukebox.plugin.tests;

import com.moviejukebox.model.Movie;

import junit.framework.TestCase;
import nl.siwoc.moviejukebox.plugin.MovieMeterRestPlugin;

public class TestMovieMeterRestPlugin extends TestCase {

	MovieMeterRestPlugin plugin = null;

	public void setUp() throws Exception {
		super.setUp();
		MovieMeterRestPlugin.setMOVIEMETER_API_KEY("tyk0awf19uqm65mjfsqw9z9rx6t706pe");
		plugin = new MovieMeterRestPlugin();
	}
	
	public void test1() {
		// Lets be cops 2014 without apostrof, no info
		//assertEquals("Found wrong id for Lets be cops (2014)", "UNKNOWN", plugin.getMovieId("Lets be cops","2014"));
		// Let's be cops 2014 with apostrof
		assertEquals("Found wrong id for Let's be cops (2014)", "99052", plugin.getMovieId(prepareMovie("Let's be cops","2014")));
	}
	
	public void test2() {
		// Moby dick 2010
		assertEquals("Found wrong id for Moby dick (2010)", "68998", plugin.getMovieId(prepareMovie("Moby dick","2010")));
	}
	
	public void test3() {
		// Ruthless People 1986 had/has integer Average threw Double Exception
		assertEquals("Found wrong id for Ruthless People (1986)", "607", plugin.getMovieId(prepareMovie("Ruthless People","1986")));
	}
	
	public void test4() {
		// Soof 2013 returns only 1 result
		assertEquals("Found wrong id for Soof (2013)", "90303", plugin.getMovieId(prepareMovie("Soof","2013")));
	}
	
	public void test5() {
		// Wolf 2013 returns only multiple results even in 2013
		assertEquals("Found wrong id for Wolf (2013)", "95242", plugin.getMovieId(prepareMovie("Wolf","2013")));
	}
	
	public void test6() {
		// Wonderland (2003)
		assertEquals("Found wrong id for Wonderland (2003)", "8845", plugin.getMovieId(prepareMovie("Wonderland","2003")));
	}
	
	public void test7() {
		// Won't back down (2012)
		assertEquals("Found wrong id for Won't back down (2012)", "83318", plugin.getMovieId(prepareMovie("Won't back down","2012")));
	}
	
	private Movie prepareMovie(String title, String year) {
		Movie movie = new Movie();
		movie.setTitle(title, "test");
		movie.setYear(year, "test");
		return movie;
	}

}
