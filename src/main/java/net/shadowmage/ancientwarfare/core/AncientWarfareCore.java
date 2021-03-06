package net.shadowmage.ancientwarfare.core;

import codechicken.lib.CodeChickenLib;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.shadowmage.ancientwarfare.core.command.CommandResearch;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.container.ContainerBackpack;
import net.shadowmage.ancientwarfare.core.container.ContainerEngineeringStation;
import net.shadowmage.ancientwarfare.core.container.ContainerResearchBook;
import net.shadowmage.ancientwarfare.core.container.ContainerResearchStation;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.entity.AWFakePlayer;
import net.shadowmage.ancientwarfare.core.gamedata.Timekeeper;
import net.shadowmage.ancientwarfare.core.interop.ModAccessors;
import net.shadowmage.ancientwarfare.core.item.AWCoreItemLoader;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.proxy.CommonProxyBase;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;

import java.util.UUID;

@Mod
        (
                name = "Ancient Warfare Core",
                modid = AncientWarfareCore.modID,
                version = "@VERSION@",
                guiFactory = "net.shadowmage.ancientwarfare.core.gui.options.OptionsGuiFactory",
                dependencies = CodeChickenLib.MOD_VERSION_DEP
        )
public class AncientWarfareCore {

    public static final String modID = "ancientwarfare";

    @Instance(value = AncientWarfareCore.modID)
    public static AncientWarfareCore instance;

    @SidedProxy
            (
                    clientSide = "net.shadowmage.ancientwarfare.core.proxy.ClientProxy",
                    serverSide = "net.shadowmage.ancientwarfare.core.proxy.CommonProxyBase"
            )
    public static CommonProxyBase proxy;

    public static org.apache.logging.log4j.Logger log;

    public static AWCoreStatics statics;
    
    // Used by FakePlayerFactory
    public static GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes("AncientWarfareMod".getBytes()), "[AncientWarfareMod]");

    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        /*
         * setup config file and logger
         */
        log = evt.getModLog();
        statics = new AWCoreStatics("AncientWarfare");

        /*
         * register server-side network handler and anything that needs loaded on the event busses
         */
        NetworkHandler.INSTANCE.registerNetwork();//register network handler, server side

        MinecraftForge.EVENT_BUS.register(ResearchTracker.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new Timekeeper());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(this);


        /*
         * register GUIs, containers, client-side network handler, renderers
         */
        NetworkHandler.registerContainer(NetworkHandler.GUI_CRAFTING, ContainerEngineeringStation.class);
        NetworkHandler.registerContainer(NetworkHandler.GUI_RESEARCH_STATION, ContainerResearchStation.class);
        NetworkHandler.registerContainer(NetworkHandler.GUI_BACKPACK, ContainerBackpack.class);
        NetworkHandler.registerContainer(NetworkHandler.GUI_RESEARCH_BOOK, ContainerResearchBook.class);

        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent evt) {

        proxy.init();

        AWCoreItemLoader.INSTANCE.load();

        ResearchGoal.initializeResearch();

        AWCraftingManager.loadRecipes();

        /*
         * Setup compats
         */
        ModAccessors.init();
    }

    @EventHandler
    public void postinit(FMLPostInitializationEvent evt) {
        statics.save();
    }

    @EventHandler
    public void serverStartingEvent(FMLServerStartingEvent evt) {
        evt.registerServerCommand(new CommandResearch());
    }
    
    @SubscribeEvent
    public void configChangedEvent(OnConfigChangedEvent evt) {
        if (modID.equals(evt.getModID())) {
            statics.save();
            proxy.onConfigChanged();
        }
    }

    @SubscribeEvent
    public void createResearchRecipeRegistry(RegistryEvent.NewRegistry evt) {
        AWCraftingManager.init();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDimensionUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof WorldServer)
            AWFakePlayer.onWorldUnload();
    }
}
