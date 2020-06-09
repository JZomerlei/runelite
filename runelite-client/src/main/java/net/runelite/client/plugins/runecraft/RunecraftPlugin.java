/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.runecraft;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.NullObjectID;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import static net.runelite.client.plugins.runecraft.AbyssRifts.AIR_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.BLOOD_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.BODY_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.CHAOS_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.COSMIC_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.DEATH_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.EARTH_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.FIRE_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.LAW_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.MIND_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.NATURE_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.SOUL_RIFT;
import static net.runelite.client.plugins.runecraft.AbyssRifts.WATER_RIFT;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Runecraft",
	description = "Show minimap icons and clickboxes for abyssal rifts",
	tags = {"abyssal", "minimap", "overlay", "rifts", "rc", "runecrafting"}
)
public class RunecraftPlugin extends Plugin
{
	private static final String POUCH_DECAYED_NOTIFICATION_MESSAGE = "Your rune pouch has decayed.";
	private static final String POUCH_DECAYED_MESSAGE = "Your pouch has decayed through use.";
	private static final List<Integer> DEGRADED_POUCHES = ImmutableList.of(
		ItemID.MEDIUM_POUCH_5511,
		ItemID.LARGE_POUCH_5513,
		ItemID.GIANT_POUCH_5515
	);

	private static final int DENSE_RUNESTONE_SOUTH_ID = NullObjectID.NULL_10796;
	private static final int DENSE_RUNESTONE_NORTH_ID = NullObjectID.NULL_8981;
	private static final Item DENSE_ESSENCE_BLOCK = new Item(ItemID.DENSE_ESSENCE_BLOCK, 1);

	@Getter(AccessLevel.PACKAGE)
	private final Set<DecorativeObject> abyssObjects = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<AbyssRifts> rifts = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private boolean degradedPouchInInventory;

	@Getter(AccessLevel.PACKAGE)
	private NPC darkMage;

	@Getter(AccessLevel.PACKAGE)
	private GameObject denseRunestoneSouth;

	@Getter(AccessLevel.PACKAGE)
	private GameObject denseRunestoneNorth;

	@Getter(AccessLevel.PACKAGE)
	private boolean denseRunestoneSouthMineable;

	@Getter(AccessLevel.PACKAGE)
	private boolean denseRunestoneNorthMineable;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AbyssOverlay abyssOverlay;

	@Inject
	private AbyssMinimapOverlay abyssMinimapOverlay;

	@Getter(AccessLevel.PACKAGE)
	private DenseRunecraftingSession session;

	@Inject
	private DenseRunestoneOverlay denseRunestoneOverlay;

	@Inject
	private ChippingOverlay chippingOverlay;

	@Inject
	private RunecraftConfig config;

	@Inject
	private Notifier notifier;

	private int timeOut;
	private ArrayList<Item> prevInventory = null;
	private ArrayList<Item> currInventory = null;

	@Provides
	RunecraftConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RunecraftConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(abyssOverlay);
		overlayManager.add(abyssMinimapOverlay);
		overlayManager.add(denseRunestoneOverlay);
		overlayManager.add(chippingOverlay);
		session = new DenseRunecraftingSession();
		updateRifts();
		timeOut = config.statTimeout();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(abyssOverlay);
		overlayManager.remove(abyssMinimapOverlay);
		overlayManager.remove(denseRunestoneOverlay);
		overlayManager.remove(chippingOverlay);
		abyssObjects.clear();
		darkMage = null;
		denseRunestoneNorth = null;
		denseRunestoneSouth = null;
		degradedPouchInInventory = false;
		prevInventory = null;
		currInventory = null;
		session = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("runecraft"))
		{
			updateRifts();
			timeOut = config.statTimeout();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (config.degradingNotification())
		{
			if (event.getMessage().contains(POUCH_DECAYED_MESSAGE))
			{
				notifier.notify(POUCH_DECAYED_NOTIFICATION_MESSAGE);
			}
		}
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		DecorativeObject decorativeObject = event.getDecorativeObject();
		if (AbyssRifts.getRift(decorativeObject.getId()) != null)
		{
			abyssObjects.add(decorativeObject);
		}
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		DecorativeObject decorativeObject = event.getDecorativeObject();
		abyssObjects.remove(decorativeObject);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		switch (gameState)
		{
			case LOADING:
				abyssObjects.clear();
				denseRunestoneNorth = null;
				denseRunestoneSouth = null;
				break;
			case CONNECTION_LOST:
			case HOPPING:
			case LOGIN_SCREEN:
				darkMage = null;
				break;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId())
		{
			return;
		}

		final Item[] items = event.getItemContainer().getItems();
		degradedPouchInInventory = Stream.of(items).anyMatch(i -> DEGRADED_POUCHES.contains(i.getId()));

		currInventory = new ArrayList <>(Arrays.asList(items));

		// only set the previous inventory if it was originally null
		prevInventory = prevInventory == null ? new ArrayList <>(Arrays.asList(items)) : prevInventory;

		int prevDenseEssenceCount = Collections.frequency(prevInventory, DENSE_ESSENCE_BLOCK);
		int currDenseEssenceCount = Collections.frequency(currInventory, DENSE_ESSENCE_BLOCK);

		// if current essence count is more than previous count then we are actively chipping
		if (currDenseEssenceCount > prevDenseEssenceCount)
		{
			session.incrementDenseEsseenceChipped();
		}

		// set the previous inventory to the what it currently is so it can be compared onItemContainerChanged
		prevInventory = new ArrayList <>(Arrays.asList(items));
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		final NPC npc = event.getNpc();
		if (npc.getId() == NpcID.DARK_MAGE)
		{
			darkMage = npc;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		final NPC npc = event.getNpc();
		if (npc == darkMage)
		{
			darkMage = null;
		}
	}

	private void updateRifts()
	{
		rifts.clear();
		if (config.showAir())
		{
			rifts.add(AIR_RIFT);
		}
		if (config.showBlood())
		{
			rifts.add(BLOOD_RIFT);
		}
		if (config.showBody())
		{
			rifts.add(BODY_RIFT);
		}
		if (config.showChaos())
		{
			rifts.add(CHAOS_RIFT);
		}
		if (config.showCosmic())
		{
			rifts.add(COSMIC_RIFT);
		}
		if (config.showDeath())
		{
			rifts.add(DEATH_RIFT);
		}
		if (config.showEarth())
		{
			rifts.add(EARTH_RIFT);
		}
		if (config.showFire())
		{
			rifts.add(FIRE_RIFT);
		}
		if (config.showLaw())
		{
			rifts.add(LAW_RIFT);
		}
		if (config.showMind())
		{
			rifts.add(MIND_RIFT);
		}
		if (config.showNature())
		{
			rifts.add(NATURE_RIFT);
		}
		if (config.showSoul())
		{
			rifts.add(SOUL_RIFT);
		}
		if (config.showWater())
		{
			rifts.add(WATER_RIFT);
		}
	}

	@Schedule(
		period = 1,
		unit = ChronoUnit.SECONDS
	)
	public void checkChipping()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Instant lastDenseEssenceChipped = session.getLastDenseEssenceChipped();

		if (lastDenseEssenceChipped == null)
		{
			return;
		}
		// reset statst if you haven't chipped anything recently
		Duration statTimeout = Duration.ofMinutes(timeOut);
		Duration sinceChipped = Duration.between(lastDenseEssenceChipped, Instant.now());

		if (sinceChipped.compareTo(statTimeout) >= 0)
		{
			session.resetRecent();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();
		int id = obj.getId();

		switch (id)
		{
			case DENSE_RUNESTONE_SOUTH_ID:
				denseRunestoneSouth = obj;
				break;
			case DENSE_RUNESTONE_NORTH_ID:
				denseRunestoneNorth = obj;
				break;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		switch (event.getGameObject().getId())
		{
			case DENSE_RUNESTONE_SOUTH_ID:
				denseRunestoneSouth = null;
				break;
			case DENSE_RUNESTONE_NORTH_ID:
				denseRunestoneNorth = null;
				break;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		denseRunestoneSouthMineable = client.getVar(Varbits.DENSE_RUNESTONE_SOUTH_DEPLETED) == 0;
		denseRunestoneNorthMineable = client.getVar(Varbits.DENSE_RUNESTONE_NORTH_DEPLETED) == 0;

		updateDenseRunestoneState();
	}

	private void updateDenseRunestoneState()
	{
		denseRunestoneSouthMineable = client.getVar(Varbits.DENSE_RUNESTONE_SOUTH_DEPLETED) == 0;
		denseRunestoneNorthMineable = client.getVar(Varbits.DENSE_RUNESTONE_NORTH_DEPLETED) == 0;
	}
}
