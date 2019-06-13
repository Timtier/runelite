/*
 * Copyright (c) 2018, Eadgars Ruse <https://github.com/Eadgars-Ruse>
 * Copyright (c) 2019, Jordan Atwood <nightfirecat@protonmail.com>
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
package net.runelite.client.plugins.cluescrolls.clues;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import static net.runelite.client.plugins.cluescrolls.ClueScrollOverlay.TITLED_CONTENT_COLOR;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import static net.runelite.client.plugins.cluescrolls.ClueScrollWorldOverlay.IMAGE_Z_OFFSET;
import net.runelite.client.plugins.cluescrolls.clues.hotcold.HotColdArea;
import net.runelite.client.plugins.cluescrolls.clues.hotcold.HotColdLocation;
import net.runelite.client.plugins.cluescrolls.clues.hotcold.HotColdSolver;
import net.runelite.client.plugins.cluescrolls.clues.hotcold.HotColdTemperature;
import net.runelite.client.plugins.cluescrolls.clues.hotcold.HotColdTemperatureChange;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Getter
public class HotColdClue extends ClueScroll implements LocationClueScroll, LocationsClueScroll, TextClueScroll, NpcClueScroll
{
	private static final int HOT_COLD_PANEL_WIDTH = 200;
	private static final HotColdClue CLUE =
		new HotColdClue("Buried beneath the ground, who knows where it's found. Lucky for you, A man called Jorral may have a clue.",
			"Jorral",
			"Speak to Jorral to receive a strange device.");

	private final String text;
	private final String npc;
	private final String solution;
	private HotColdSolver hotColdSolver;
	private WorldPoint location;

	public static HotColdClue forText(String text)
	{
		if (CLUE.text.equalsIgnoreCase(text))
		{
			return CLUE;
		}

		return null;
	}

	private HotColdClue(String text, String npc, String solution)
	{
		this.text = text;
		this.npc = npc;
		this.solution = solution;
		setRequiresSpade(true);
		initializeSolver();
	}

	@Override
	public WorldPoint[] getLocations()
	{
		return hotColdSolver.getPossibleLocations().stream().map(HotColdLocation::getWorldPoint).toArray(WorldPoint[]::new);
	}

	@Override
	public void makeOverlayHint(PanelComponent panelComponent, ClueScrollPlugin plugin)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Hot/Cold Clue")
			.build());
		panelComponent.setPreferredSize(new Dimension(HOT_COLD_PANEL_WIDTH, 0));

		// strange device has not been tested yet, show how to get it
		if (hotColdSolver.getLastWorldPoint() == null && location == null)
		{
			if (getNpc() != null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("NPC:")
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left(getNpc())
					.leftColor(TITLED_CONTENT_COLOR)
					.build());
			}

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Solution:")
				.build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left(getSolution())
				.leftColor(TITLED_CONTENT_COLOR)
				.build());
		}
		// strange device has been tested, show possible locations for final dig spot
		else
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Possible areas:")
				.build());

			final Map<HotColdArea, Integer> locationCounts = new EnumMap<>(HotColdArea.class);
			final Collection<HotColdLocation> digLocations = hotColdSolver.getPossibleLocations();

			for (HotColdLocation hotColdLocation : digLocations)
			{
				HotColdArea hotColdArea = hotColdLocation.getHotColdArea();

				if (locationCounts.containsKey(hotColdArea))
				{
					locationCounts.put(hotColdArea, locationCounts.get(hotColdArea) + 1);
				}
				else
				{
					locationCounts.put(hotColdArea, 1);
				}
			}

			if (digLocations.size() > 10)
			{
				for (HotColdArea area : locationCounts.keySet())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(area.getName())
						.right(Integer.toString(locationCounts.get(area)))
						.build());
				}
			}
			else
			{
				for (HotColdArea area : locationCounts.keySet())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(area.getName() + ':')
						.build());

					for (HotColdLocation hotColdLocation : digLocations)
					{
						if (hotColdLocation.getHotColdArea() == area)
						{
							panelComponent.getChildren().add(LineComponent.builder()
								.left("- " + hotColdLocation.getArea())
								.leftColor(Color.LIGHT_GRAY)
								.build());
						}
					}
				}
			}
		}
	}

	@Override
	public void makeWorldOverlayHint(Graphics2D graphics, ClueScrollPlugin plugin)
	{
		// when final location has been found
		if (location != null)
		{
			LocalPoint localLocation = LocalPoint.fromWorld(plugin.getClient(), getLocation());

			if (localLocation != null)
			{
				OverlayUtil.renderTileOverlay(plugin.getClient(), graphics, localLocation, plugin.getSpadeImage(), Color.ORANGE);
			}

			return;
		}

		// when strange device hasn't been activated yet, show Jorral
		if (hotColdSolver.getLastWorldPoint() == null && plugin.getNpcsToMark() != null)
		{
			for (NPC npcToMark : plugin.getNpcsToMark())
			{
				OverlayUtil.renderActorOverlayImage(graphics, npcToMark, plugin.getClueScrollImage(), Color.ORANGE, IMAGE_Z_OFFSET);
			}
		}

		// once the number of possible dig locations is below 10, show the dig spots
		final Collection<HotColdLocation> digLocations = hotColdSolver.getPossibleLocations();
		if (digLocations.size() < 10)
		{
			// Mark potential dig locations
			for (HotColdLocation hotColdLocation : digLocations)
			{
				WorldPoint wp = hotColdLocation.getWorldPoint();
				LocalPoint localLocation = LocalPoint.fromWorld(plugin.getClient(), wp.getX(), wp.getY());

				if (localLocation == null)
				{
					return;
				}

				OverlayUtil.renderTileOverlay(plugin.getClient(), graphics, localLocation, plugin.getSpadeImage(), Color.ORANGE);
			}
		}
	}

	public boolean update(final String message, final ClueScrollPlugin plugin)
	{
		final HotColdTemperature temperature = HotColdTemperature.of(message);

		if (temperature == null)
		{
			return false;
		}

		final WorldPoint localWorld = plugin.getClient().getLocalPlayer().getWorldLocation();

		if (localWorld == null)
		{
			return false;
		}

		if (temperature == HotColdTemperature.VISIBLY_SHAKING)
		{
			markFinalSpot(localWorld);
		}
		else
		{
			location = null;

			final HotColdTemperatureChange temperatureChange = HotColdTemperatureChange.of(message);
			hotColdSolver.signal(localWorld, temperature, temperatureChange);
		}

		return true;
	}

	@Override
	public void reset()
	{
		initializeSolver();
	}

	private void initializeSolver()
	{
		final Set<HotColdLocation> locations = Arrays.stream(HotColdLocation.values())
			.collect(Collectors.toSet());
		hotColdSolver = new HotColdSolver(locations);
	}

	private void markFinalSpot(WorldPoint wp)
	{
		this.location = wp;
		reset();
	}

	public String[] getNpcs()
	{
		return new String[] {npc};
	}
}
