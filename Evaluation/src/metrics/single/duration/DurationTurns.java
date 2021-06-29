package metrics.single.duration;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Metric;
import other.trial.Trial;

/**
 * Number of turns in a game.
 * 
 * @author matthew.stephenson
 */
public class DurationTurns extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public DurationTurns()
	{
		super
		(
			"Duration Turns", 
			"Number or turns in a game.", 
			0.0, 
			-1,
			null
		);
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public double apply
	(
			final Game game,
			final Trial[] trials,
			final RandomProviderState[] randomProviderStates
	)
	{
		// Count the number of turns.
		double turnTally = 0;
		for (final Trial trial : trials)
			turnTally += trial.numTurns();
		
		return turnTally / trials.length;
	}

	//-------------------------------------------------------------------------

}
