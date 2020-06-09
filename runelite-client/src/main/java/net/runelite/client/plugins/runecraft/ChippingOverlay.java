package net.runelite.client.plugins.runecraft;

import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import static net.runelite.api.AnimationID.MINING_3A_PICKAXE;
import static net.runelite.api.AnimationID.MINING_ADAMANT_PICKAXE;
import static net.runelite.api.AnimationID.MINING_BLACK_PICKAXE;
import static net.runelite.api.AnimationID.MINING_BRONZE_PICKAXE;
import static net.runelite.api.AnimationID.MINING_CRYSTAL_PICKAXE;
import static net.runelite.api.AnimationID.MINING_DRAGON_PICKAXE;
import static net.runelite.api.AnimationID.MINING_DRAGON_PICKAXE_OR;
import static net.runelite.api.AnimationID.MINING_DRAGON_PICKAXE_UPGRADED;
import static net.runelite.api.AnimationID.MINING_GILDED_PICKAXE;
import static net.runelite.api.AnimationID.MINING_INFERNAL_PICKAXE;
import static net.runelite.api.AnimationID.MINING_IRON_PICKAXE;
import static net.runelite.api.AnimationID.MINING_MITHRIL_PICKAXE;
import static net.runelite.api.AnimationID.MINING_RUNE_PICKAXE;
import static net.runelite.api.AnimationID.MINING_STEEL_PICKAXE;
import static net.runelite.api.AnimationID.DENSE_ESSENCE_CHIPPING;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
public class ChippingOverlay extends OverlayPanel
{
	private static final Set<Integer> MINING_ANIMATION_IDS = ImmutableSet.of(
		MINING_BRONZE_PICKAXE, MINING_IRON_PICKAXE, MINING_STEEL_PICKAXE,
		MINING_BLACK_PICKAXE, MINING_MITHRIL_PICKAXE, MINING_ADAMANT_PICKAXE,
		MINING_RUNE_PICKAXE, MINING_GILDED_PICKAXE, MINING_DRAGON_PICKAXE,
		MINING_DRAGON_PICKAXE_UPGRADED, MINING_DRAGON_PICKAXE_OR, MINING_INFERNAL_PICKAXE,
		MINING_CRYSTAL_PICKAXE, MINING_3A_PICKAXE, DENSE_ESSENCE_CHIPPING
	);

	private final RunecraftConfig config;
	private final Client client;
	private final RunecraftPlugin plugin;

	@Inject
	ChippingOverlay(Client client, RunecraftPlugin plugin, RunecraftConfig config)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_LEFT);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		DenseRunecraftingSession session = plugin.getSession();
		panelComponent.getChildren().clear();

		if (session.getLastDenseEssenceChipped() != null)
		{
			if (config.showChippingState())
			{
				if (MINING_ANIMATION_IDS.contains(client.getLocalPlayer().getAnimation()))
				{
					panelComponent.getChildren().add(TitleComponent.builder()
						.text("Chipping")
						.color(Color.GREEN)
						.build());
				}
				else
				{
					panelComponent.getChildren().add(TitleComponent.builder()
						.text("NOT chipping")
						.color(Color.RED)
						.build());
				}
			}
		}
		return super.render(graphics);
	}
}
