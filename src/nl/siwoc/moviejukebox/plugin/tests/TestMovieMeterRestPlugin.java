package nl.siwoc.moviejukebox.plugin.tests;

import junit.framework.TestCase;
import nl.siwoc.moviejukebox.plugin.MovieMeterRestPlugin;

public class TestMovieMeterRestPlugin extends TestCase {

	public void test() {
		MovieMeterRestPlugin plugin = new MovieMeterRestPlugin();
		MovieMeterRestPlugin.setMOVIEMETER_API_KEY("tyk0awf19uqm65mjfsqw9z9rx6t706pe");
		
		// Lets be cops 2014 without apostrof, no info
		assertEquals("Found wrong id for Lets be cops (2014)", "UNKNOWN", plugin.getMovieId("Lets be cops","2014"));
		// Let's be cops 2014 with apostrof
		assertEquals("Found wrong id for Let's be cops (2014)", "99052", plugin.getMovieId("Let's be cops","2014"));
		// Moby dick 2010
		assertEquals("Found wrong id for Moby dick (2010)", "68998", plugin.getMovieId("Moby dick","2010"));
		// Ruthless People 1986 had/has integer Average threw Double Exception
		assertEquals("Found wrong id for Ruthless People (1986)", "607", plugin.getMovieId("Ruthless People","1986"));
		// Soof 2013 returns only 1 result
		assertEquals("Found wrong id for Soof (2013)", "90303", plugin.getMovieId("Soof","2013"));
		// Wolf 2013 returns only multiple results even in 2013
		assertEquals("Found wrong id for Wolf (2013)", "95242", plugin.getMovieId("Wolf","2013"));
		// Wonderland (2003)
		assertEquals("Found wrong id for Wonderland (2003)", "8845", plugin.getMovieId("Wonderland","2003"));
		// Won't back down (2012)
		assertEquals("Found wrong id for Won't back down (2012)", "83318", plugin.getMovieId("Won't back down","2012"));
	}

}
