package net.runelite.client.plugins.runecraft;

import java.time.Instant;
import net.runelite.api.ItemID;

public class DenseRunecraftingSession
{
	private Instant lastDenseEssenceChipped;
	private int totalChipped = 0;
	private int soulRunesCreated = 0;
	private int bloodRunesCreated = 0;

	public void incrementDenseEsseenceChipped()
	{
		Instant now = Instant.now();

		lastDenseEssenceChipped = now;
		++totalChipped;
	}

	public void resetRecent()
	{
		bloodRunesCreated = 0;
		soulRunesCreated = 0;
		lastDenseEssenceChipped = null;
		totalChipped = 0;
	}

	public Instant getLastDenseEssenceChipped()
	{
		return lastDenseEssenceChipped;
	}

	public int getTotalChipped()
	{
		return totalChipped;
	}

	public void runesCreated(int itemid, int quantity)
	{
		if (itemid == ItemID.SOUL_RUNE)
		{
			if (soulRunesCreated == 0)
			{
				soulRunesCreated = quantity;
			}
			else
			{
				soulRunesCreated += quantity;
			}
		}

		if (itemid == ItemID.BLOOD_RUNE)
		{
			if (bloodRunesCreated == 0)
			{
				bloodRunesCreated = quantity;
			}
			else
			{
				bloodRunesCreated += quantity;
			}
		}
	}

	public int getBloodRunesCreated()
	{
		return bloodRunesCreated;
	}
	public int getSoulRunesCreated()
	{
		return soulRunesCreated;
	}

}
