package net.shadowmage.ancientwarfare.automation.tile.warehouse2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.util.INBTSerializable;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseStockViewer;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.interfaces.IOwnable;
import net.shadowmage.ancientwarfare.core.inventory.ItemQuantityMap.ItemHashEntry;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.core.util.NBTSerializableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TileWarehouseStockViewer extends TileControlled implements IOwnable, IInteractableTile {

    private final List<WarehouseStockFilter> filters = new ArrayList<>();
    private UUID ownerId;
    private String ownerName = "";
    private boolean shouldUpdate = false;

    private final Set<ContainerWarehouseStockViewer> viewers = new HashSet<>();

    public TileWarehouseStockViewer() {
    }

    public void updateViewers() {
        for (ContainerWarehouseStockViewer viewer : viewers) {
            viewer.onFiltersChanged();
        }
    }

    public void addViewer(ContainerWarehouseStockViewer viewer) {
        viewers.add(viewer);
    }

    public void removeViewer(ContainerWarehouseStockViewer viewer) {
        viewers.remove(viewer);
    }

    public List<WarehouseStockFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<WarehouseStockFilter> filters) {
        this.filters.clear();
        this.filters.addAll(filters);
        shouldUpdate = false;//set to false, as we are manually updating right now
        recountFilters(false);//recount filters, do not send update
        BlockTools.notifyBlockUpdate(this); //to re-send description packet to client with new filters
    }

    /*
     * should be called whenever controller tile is set or warehouse inventory updated
     */
    private void recountFilters(boolean sendToClients) {
        TileWarehouseBase twb = (TileWarehouseBase) getController();
        int count;
        int index = 0;
        if (twb == null) {
            count = 0;
            for (WarehouseStockFilter filter : this.filters) {
                if (count != filter.getQuantity()) {
                    filter.setQuantity(0);
                    if (sendToClients) {
                        world.addBlockEvent(pos, getBlockType(), index, count);
                    }
                }
                index++;
            }
        } else {
            for (WarehouseStockFilter filter : this.filters) {
                count = filter.getFilterItem().isEmpty() ? 0 : twb.getCountOf(filter.getFilterItem());
                if (count != filter.getQuantity()) {
                    filter.setQuantity(count);
                    if (sendToClients) {
                        world.addBlockEvent(pos, getBlockType(), index, count);
                    }
                }
                index++;
            }
        }
    }

    @Override
    public boolean isOwner(EntityPlayer player){
        return EntityTools.isOwnerOrSameTeam(player, ownerId, ownerName);
    }

    @Override
    public void setOwner(EntityPlayer player) {
        this.ownerId = player.getUniqueID();
        this.ownerName = player.getName();
    }
    
    @Override
    public void setOwner(String ownerName, UUID ownerUuid) {
        this.ownerName = ownerName;
        this.ownerId = ownerUuid;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }
    
    @Override
    public UUID getOwnerUuid() {
        return ownerId;
    }

    @Override
    public boolean onBlockClicked(EntityPlayer player, @Nullable EnumHand hand) {
        if (!player.world.isRemote && isOwner(player)) {
            NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WAREHOUSE_STOCK, pos);
        }
        return true;
    }

    @Override
    protected void updateTile() {
        if (shouldUpdate) {
            shouldUpdate = false;
            recountFilters(true);
        }
    }

    /*
     * should be called on SERVER whenever warehouse inventory changes
     */
    public void onWarehouseInventoryUpdated() {
        shouldUpdate = true;
    }

    @Override
    protected void writeUpdateNBT(NBTTagCompound tag) {
        super.writeUpdateNBT(tag);
        NBTSerializableUtils.write(tag, "filterList", filters);
    }

    @Override
    protected void handleUpdateNBT(NBTTagCompound tag) {
        super.handleUpdateNBT(tag);
        this.filters.clear();
        this.filters.addAll(NBTSerializableUtils.read(tag, "filterList", WarehouseStockFilter.class));
        updateViewers();
    }

    @Override
    public boolean receiveClientEvent(int a, int b) {
        if (world.isRemote) {
            if (a >= 0 && a < filters.size()) {
                filters.get(a).setQuantity(b);
                updateViewers();
            }
        }
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        filters.addAll(NBTSerializableUtils.read(tag, "filterList", WarehouseStockFilter.class));
        ownerName = tag.getString("ownerName");
        if(tag.hasKey("ownerId"))
            ownerId = UUID.fromString(tag.getString("ownerId"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTSerializableUtils.write(tag, "filterList", filters);
        tag.setString("ownerName", ownerName);
        if(ownerId !=null)
            tag.setString("ownerId", ownerId.toString());

        return tag;
    }

    public static class WarehouseStockFilter implements INBTSerializable<NBTTagCompound> {
        @Nonnull
        private ItemStack item = ItemStack.EMPTY;
        ItemHashEntry hashKey;
        private int quantity;

        public WarehouseStockFilter() {
        }

        public WarehouseStockFilter(ItemStack item, int qty) {
            setQuantity(qty);
            setItem(item);
        }

        public void setItem(ItemStack item) {
            this.item = item;
            this.hashKey = item.isEmpty() ? null : new ItemHashEntry(item);
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public ItemStack getFilterItem() {
            return item;
        }

        public int getQuantity() {
            return quantity;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            if (!item.isEmpty()) {
                tag.setTag("item", item.writeToNBT(new NBTTagCompound()));
            }
            tag.setInteger("quantity", quantity);
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound tag) {
            setItem(tag.hasKey("item") ? new ItemStack(tag.getCompoundTag("item")) : ItemStack.EMPTY);
            setQuantity(tag.getInteger("quantity"));
        }
    }
}
