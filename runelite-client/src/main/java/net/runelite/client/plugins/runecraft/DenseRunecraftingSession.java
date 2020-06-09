package net.runelite.client.plugins.runecraft;

import java.time.Instant;

public class DenseRunecraftingSession
{
	private Instant lastDenseEssenceChipped;

	public void incrementDenseEsseenceChipped()
	{
		Instant now = Instant.now();

		lastDenseEssenceChipped = now;
	}

	public void resetRecent()
	{
		lastDenseEssenceChipped = null;
	}

	public Instant getLastDenseEssenceChipped()
	{
		return lastDenseEssenceChipped;
	}
}
