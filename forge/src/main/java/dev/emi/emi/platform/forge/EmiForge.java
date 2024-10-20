package dev.emi.emi.platform.forge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.emi.emi.data.EmiData;
import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.network.PingS2CPacket;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.platform.EmiMain;
import dev.emi.emi.registry.EmiCommands;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import dev.emi.emi.backport.EmiResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

@Mod(modid = "emi", useMetadata = true, guiFactory = "dev.emi.emi.platform.forge.EmiGuiFactory")
public class EmiForge {

	public EmiForge() {
		EmiMain.init();
		EmiPacketHandler.init();
		EmiNetwork.initServer((player, packet) -> {
			EmiPacketHandler.CHANNEL.sendTo(packet, player);
		});
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance().bus().register(this);
		if (FMLLaunchHandler.side().isClient()) {
			MinecraftForge.EVENT_BUS.register(new EmiClientForge());
		}
	}

	@Mod.EventHandler
	public void serverStart(FMLServerStartingEvent event) {
		EmiCommands.registerCommands(event.getServer());
	}

	@Mod.EventHandler
	@SideOnly(Side.CLIENT)
	public void init(FMLInitializationEvent event) {
		if (FMLLaunchHandler.side().isClient()) {
			EmiClient.init();
			EmiNetwork.initClient(packet -> EmiPacketHandler.CHANNEL.sendToServer(packet));
		}
	}

	@Mod.EventHandler
	@SideOnly(Side.CLIENT)
	public void postInit(FMLPostInitializationEvent event) {
		EmiTags.registerTagModels(EmiResourceManager.INSTANCE, id -> {});
		EmiData.init(ResourceReloader::reload);

		EmiReloadManager.reloadTags();
		EmiReloadManager.reloadRecipes();
	}

	@SubscribeEvent
	public void playerConnect(EntityJoinWorldEvent event) {
		if (!event.world.isClient && event.entity instanceof ServerPlayerEntity spe) {
			EmiNetwork.sendToClient(spe, new PingS2CPacket());
		}
	}
}
