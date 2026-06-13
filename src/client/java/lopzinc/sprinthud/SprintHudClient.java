
package lopzinc.sprinthud;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SprintHudClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Config config = configLoader.loadConfig();
		Minecraft client = Minecraft.getInstance();
        HudElement SprintHud = (drawContext, tickCounter) -> {
            LocalPlayer player = client.player;
            if (config.hudEnabled && player != null) {
                boolean sprinting = false;
                String mode = null;
                boolean holdMode = client.options.toggleSprint().get();
                boolean isSprinting = player.isSprinting();
                boolean keyDown = client.options.keySprint.isDown();
                if (holdMode && (keyDown | isSprinting)) {// if toggled
                    sprinting = true;
                    if (config.modeDisplay) {
                        mode = "Toggled";
                    }
                } else if (isSprinting | keyDown) {//holding mode
                    if (config.modeDisplay) {
                        mode = "Holding";
                    }
                    sprinting = true;
                }
                String hudText = config.format.replaceAll("&&", "§").replaceAll("%status%", ((sprinting ? "§aON" : "§cOFF") + (mode != null ? " (" + mode + ")" : "")));
                drawContext.text(client.font, hudText, config.x, config.y, 0xFFFFFFFF, true);
            }
        };
        Identifier SprintHudID = Identifier.fromNamespaceAndPath("modid", "sprint_hud");
        HudElementRegistry.addLast(SprintHudID,SprintHud);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					ClientCommands.literal("sprint")
							.then(ClientCommands.literal("move")
									.then(ClientCommands.argument("coords (x,y)", StringArgumentType.greedyString())
											.executes(context -> {
                                                assert client.player != null;
												String coordinates = StringArgumentType.getString(context, "coords (x,y)");
												Pattern pattern = Pattern.compile("^\\d+,\\d+$");
												Matcher m = pattern.matcher(coordinates);
												if (m.matches()) {
													String[] coords = coordinates.split(",");
													config.x = Integer.parseInt(coords[0]);
													config.y = Integer.parseInt(coords[1]);
													configLoader.saveConfig(config);
													client.player.sendSystemMessage(Component.literal("§8[§3Sprint§8] §7Set coordinates to §6" + coordinates + "§7."));
												} else {
													client.player.sendSystemMessage(Component.literal("§8[§3Sprint§8] §cInvalid argument, use the format x,y."));
												}
												return Command.SINGLE_SUCCESS;
											})
									)
							)
							.then(ClientCommands.literal("toggleHud")
									.executes(context -> {
                                        assert client.player != null;
										config.hudEnabled = !config.hudEnabled;
										configLoader.saveConfig(config);
										client.player.sendSystemMessage(Component.literal("§8[§3Sprint§8] §7Toggled sprint hud " + (config.hudEnabled ? "§aon" : "§coff") + "§7."));
										return Command.SINGLE_SUCCESS;
									}))
							.then(ClientCommands.literal("format")
									.then(ClientCommands.argument("%status% to write on/off, && for colour code.", StringArgumentType.greedyString())
											.executes(context -> {
                                                assert client.player != null;
												String input = StringArgumentType.getString(context, "%status% to write on/off, && for colour code.");
												if (input.contains("%status%")) {
													config.format = input;
													configLoader.saveConfig(config);
													client.player.sendSystemMessage(Component.literal("§8[§3Sprint§8] §7Status updated!"));
												} else {
													client.player.sendSystemMessage(Component.literal("§8[§3Sprint§8] §7 No %status% detected, if you wish to disable the hud, do /sprint toggle."));
												}
												return Command.SINGLE_SUCCESS;
											})
									)
							)
							.then(ClientCommands.literal("toggleModeDisplay")
									.executes(context -> {
                                        assert client.player != null;
										config.modeDisplay = !config.modeDisplay;
										configLoader.saveConfig(config);
										client.player.sendSystemMessage(Component.literal("§8[§3Sprint§8] §7Mode display set to " + (config.modeDisplay ? ("§aON") : ("§cOFF")) + "§7."));
										return Command.SINGLE_SUCCESS;
									})
							)
			);
		});
	}
}
