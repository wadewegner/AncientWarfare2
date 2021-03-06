package net.shadowmage.ancientwarfare.npc.item;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.shadowmage.ancientwarfare.core.item.ItemBase;
import net.shadowmage.ancientwarfare.core.proxy.IClientRegistrar;
import net.shadowmage.ancientwarfare.core.util.ModelLoaderHelper;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;

public abstract class ItemBaseNPC extends ItemBase implements IClientRegistrar {
    public ItemBaseNPC(String regName) {
        super(AncientWarfareNPC.modID, regName);
        setCreativeTab(AWNPCItemLoader.npcTab);

        AncientWarfareNPC.proxy.addClientRegistrar(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerClient() {
        ModelLoaderHelper.registerItem(this, "npc");
    }
}
