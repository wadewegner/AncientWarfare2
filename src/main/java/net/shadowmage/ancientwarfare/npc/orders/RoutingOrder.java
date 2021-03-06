package net.shadowmage.ancientwarfare.npc.orders;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.OrderingList;
import net.shadowmage.ancientwarfare.npc.item.ItemRoutingOrder;

import javax.annotation.Nonnull;
import java.util.List;

public class RoutingOrder extends OrderingList<RoutingOrder.RoutePoint> implements INBTSerializable<NBTTagCompound> {

    int routeDimension;

    public RoutingOrder() {
    }

    public void addRoutePoint(EnumFacing side, BlockPos pos) {
        add(new RoutePoint(side, pos));
    }

    private boolean check(int index){
        return index >= 0 && index < size();
    }

    public void changeRouteType(int index, boolean isRmb) {
        if (check(index)) {
            get(index).changeRouteType(isRmb);
        }
    }

    public void changeBlockSide(int index) {
        if (check(index)) {
            get(index).changeBlockSide();
        }
    }

    public void toggleIgnoreDamage(int index) {
        if (check(index)) {
            get(index).toggleIgnoreDamage();
        }
    }

    public void toggleIgnoreTag(int index) {
        if (check(index)) {
            get(index).toggleIgnoreTag();
        }
    }

    public static RoutingOrder getRoutingOrder(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof ItemRoutingOrder) {
            RoutingOrder order = new RoutingOrder();
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("orders")) {
                order.deserializeNBT(stack.getTagCompound().getCompoundTag("orders"));
            }
            return order;
        }
        return null;
    }

    public void write(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof ItemRoutingOrder) {
            stack.setTagInfo("orders", serializeNBT());
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagList list = new NBTTagList();
        for (RoutePoint p : points) {
            list.appendTag(p.writeToNBT(new NBTTagCompound()));
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("entryList", list);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        clear();
        NBTTagList entryList = tag.getTagList("entryList", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entryList.tagCount(); i++) {
            add(new RoutePoint(entryList.getCompoundTagAt(i)));
        }
    }

    public static class RoutePoint {
        boolean ignoreDamage, ignoreTag;
        RouteType routeType = RouteType.FILL_TARGET_TO;
        BlockPos target = BlockPos.ORIGIN;
        EnumFacing blockSide = EnumFacing.DOWN;
        NonNullList<ItemStack> filters = NonNullList.withSize(12, ItemStack.EMPTY);

        private RoutePoint(NBTTagCompound tag) {
            readFromNBT(tag);
        }

        public RoutePoint(EnumFacing side, BlockPos target) {
            this.target = target;
            this.blockSide = side;
        }

        private void changeBlockSide() {
            blockSide = EnumFacing.VALUES[(blockSide.ordinal() + 1) % EnumFacing.VALUES.length];
        }

        private void changeRouteType(boolean isRmb) {
            routeType = isRmb ? routeType.previous() : routeType.next();
        }

        public void setFilter(int index, ItemStack stack) {
            filters.set(index, stack);
        }

        public EnumFacing getBlockSide() {
            return blockSide;
        }

        public RouteType getRouteType() {
            return routeType;
        }

        public BlockPos getTarget() {
            return target;
        }

        public ItemStack getFilterInSlot(int slot) {
            return filters.get(slot);
        }

        public int getFilterSize(){
            return filters.size();
        }

        public boolean getIgnoreDamage() {
            return ignoreDamage;
        }

        public boolean getIgnoreTag() {
            return ignoreTag;
        }

        public void toggleIgnoreDamage() {
            ignoreDamage = !ignoreDamage;
        }

        public void toggleIgnoreTag() {
            ignoreTag = !ignoreTag;
        }

        private int depositAllItems(IItemHandler from, IItemHandler to, boolean reversed) {
            int movedStacks = 0;
            int movedSize = 0;
            for (ItemStack filter : filters) {
                if (filter.isEmpty()) {
                    continue;
                }
                movedSize += InventoryTools.transferItems(from, to, filter, Integer.MAX_VALUE, ignoreDamage, ignoreTag);
                movedStacks += movedSize / filter.getMaxStackSize();
            }
            return movedStacks;
        }

        private int depositAllItemsExcept(IItemHandler from, IItemHandler to, boolean reversed) {
            int movedStacks = 0;
            int movedSize = 0;
            for (ItemStack filter : filters) {
                if (filter.isEmpty()) {
                    continue;
                }
                movedSize += InventoryTools.transferItems(from, to, stack -> !InventoryTools.doItemStacksMatch(stack, filter, ignoreDamage, ignoreTag), Integer.MAX_VALUE);
                movedStacks += movedSize / filter.getMaxStackSize();
            }
            return movedStacks;
        }

        private int fillTo(IItemHandler from, IItemHandler to, boolean reversed) {
            int moved = 0;
            int toMove = 0;
            int foundCount = 0;
            int m1;
            for (ItemStack filter : filters) {
                if (filter.isEmpty()) {
                    continue;
                }
                foundCount = InventoryTools.getCountOf(to, filter);
                toMove = filter.getCount();
                if (foundCount > toMove) {
                    continue;
                }
                toMove -= foundCount;
                m1 = InventoryTools.transferItems(from, to, filter, toMove, ignoreDamage, ignoreTag);
                moved += m1 / filter.getMaxStackSize();
            }
            return moved;
        }

        private int depositRatio(IItemHandler from, IItemHandler to, boolean reversed) {
            int movedTotal = 0;
            int toMove = 0;
            for (ItemStack filter : filters) {
                if (filter.isEmpty()) {
                    continue;
                }
                int foundCount = InventoryTools.getCountOf(from, filter);
                toMove = (int) (foundCount * (1f/(float)filter.getCount()));
                InventoryTools.transferItems(from, to, filter, toMove, ignoreDamage, ignoreTag);
                movedTotal++;
            }
            
            return movedTotal;
        }

        private int depositExact(IItemHandler from, IItemHandler to, boolean reversed) {
            int movedTotal = 0;
            int toMove = 0;
            int foundCount = 0;
            int moved;
            for (ItemStack filter : filters) {
                if (filter.isEmpty()) {
                    continue;
                }
                foundCount = InventoryTools.getCountOf(from, filter);
                toMove = filter.getCount();
                if (foundCount < toMove) {
                    continue;
                }
                if(!InventoryTools.canInventoryHold(to, filter))
                    continue;
                moved = InventoryTools.transferItems(from, to, filter, toMove, ignoreDamage, ignoreTag);
                movedTotal += moved / filter.getMaxStackSize();
            }
            return movedTotal;
        }

        private int fillAtLeast(IItemHandler from, IItemHandler to, boolean reversed) {
            int movedTotal = 0;
            int toMove = 0;
            int foundCount = 0;
            int existingCount = 0;
            int moved;
            for (ItemStack filter : filters) {
                if (filter.isEmpty()) {
                    continue;
                }
                foundCount = InventoryTools.getCountOf(from, filter);
                existingCount = InventoryTools.getCountOf(to, filter);
                toMove = filter.getCount() - existingCount; // we only want to move items up to the specified filter size
                if (toMove < 1) {
                    // the target already has more than the filter specifies
                    continue;
                }
                
                if (foundCount < toMove) {
                    // the source doesn't have enough to fulfill the minimum requirement
                    continue;
                }
                @Nonnull ItemStack filterAdjusted = filter.copy();
                filterAdjusted.setCount(toMove);
                if(!InventoryTools.canInventoryHold(to, filterAdjusted))
                    continue;
                moved = InventoryTools.transferItems(from, to, filterAdjusted, foundCount, ignoreDamage, ignoreTag);
                movedTotal += moved / filter.getMaxStackSize();
            }
            return movedTotal;
        }

        private final void readFromNBT(NBTTagCompound tag) {
            routeType = RouteType.values()[tag.getInteger("type")];
            target = BlockPos.fromLong(tag.getLong("position"));
            blockSide = EnumFacing.VALUES[tag.getByte("blockSide")];
            ignoreDamage = tag.getBoolean("ignoreDamage");
            ignoreTag = tag.getBoolean("ignoreTag");
            NBTTagList filterList = tag.getTagList("filterList", Constants.NBT.TAG_COMPOUND);
            NBTTagCompound itemTag;
            int slot;
            for (int i = 0; i < filterList.tagCount(); i++) {
                itemTag = filterList.getCompoundTagAt(i);
                slot = itemTag.getInteger("slot");

                while (slot >= filters.size()) {
                    filters.add(ItemStack.EMPTY);
                }

                filters.set(slot, new ItemStack(itemTag));
            }
        }

        private final NBTTagCompound writeToNBT(NBTTagCompound tag) {
            tag.setInteger("type", routeType.ordinal());
            tag.setLong("position", target.toLong());
            tag.setByte("blockSide", (byte) blockSide.ordinal());
            tag.setBoolean("ignoreDamage", ignoreDamage);
            tag.setBoolean("ignoreTag", ignoreTag);
            NBTTagList filterList = new NBTTagList();
            NBTTagCompound itemTag;
            for (int i = 0; i < filters.size(); i++) {
                if (filters.get(i).isEmpty()) {
                    continue;
                }
                itemTag = filters.get(i).writeToNBT(new NBTTagCompound());
                itemTag.setInteger("slot", i);
                filterList.appendTag(itemTag);
            }
            tag.setTag("filterList", filterList);
            return tag;
        }

    }

    public enum RouteType {
        /*
         * fill target up to the specified quantity from couriers inventory
         */
        FILL_TARGET_TO("route.fill.upto"),

        /*
         * fill courier up to the specified quantity from targets inventory
         */
        FILL_COURIER_TO("route.take.upto"),

        /*
         * deposit any of the specified items from courier into target inventory
         * (no quantity limit)
         */
        DEPOSIT_ALL_OF("route.deposit.match"),

        /*
         * withdraw any of the specified items from target inventory into courier inventory
         * (no quantity limit)
         */
        WITHDRAW_ALL_OF("route.withdraw.match"),

        /*
         * deposit all items in courier inventory, except those matching filter items
         */
        DEPOSIT_ALL_EXCEPT("route.deposit.no_match"),

        /*
         * withdraw all items in target inventory except those matching filters
         */
        WITHDRAW_ALL_EXCEPT("route.withdraw.no_match"),
        
        /*
         * deposit specified ratio of items (ratio is 1/filterStacksize)
         */
        DEPOSIT_RATIO("route.deposit.ratio"),
        
        /*
         * withdraw specified ratio of items (ratio is 1/filterStacksize)
         */
        WITHDRAW_RATIO("route.withdraw.ratio"),
        
        /*
         * deposit exact number of items (or none at all if not possible)
         */
        DEPOSIT_EXACT("route.deposit.exact"),
        
        /*
         * withdraw exact number of items (or none at all if not possible)
         */
        WITHDRAW_EXACT("route.withdraw.exact"),
        
        /*
         * deposit a minimum of items
         */
        FILL_MINIMUM("route.fill.minimum"),
        
        /*
         * withdraw a minimum of items
         */
        TAKE_MINIMUM("route.take.minimum");

        final String key;

        RouteType(String key) {
            this.key = key;
        }

        public String getTranslationKey() {
            return key;
        }

        public static RouteType next(RouteType type) {
            return type == null ? RouteType.FILL_TARGET_TO : type.next();
        }
        
        public static RouteType previous(RouteType type) {
            return type == null ? RouteType.FILL_TARGET_TO : type.previous();
        }

        public RouteType next() {
            int ordinal = ordinal() + 1;
            if (ordinal >= RouteType.values().length) {
                ordinal = 0;
            }
            return RouteType.values()[ordinal];
        }
        
        public RouteType previous() {
            int ordinal = ordinal() - 1;
            if (ordinal < 0) {
                ordinal = RouteType.values().length - 1;
            }
            return RouteType.values()[ordinal];
        }

    }

    /*
     * do the routing action for the courier at the given route-point.  position/distance is not checked, should check in AI before calling<br>
     * returns the number of stacks processed for determining the length the courier should 'work' at the point
     */
    public int handleRouteAction(RoutePoint p, IItemHandler npc, IItemHandler target) {
        switch (p.routeType) {
            case FILL_COURIER_TO:
                return p.fillTo(target, npc, true);

            case FILL_TARGET_TO:
                return p.fillTo(npc, target, false);

            case DEPOSIT_ALL_EXCEPT:
                return p.depositAllItemsExcept(npc, target, false);

            case DEPOSIT_ALL_OF:
                return p.depositAllItems(npc, target, false);

            case WITHDRAW_ALL_EXCEPT:
                return p.depositAllItemsExcept(target, npc, true);

            case WITHDRAW_ALL_OF:
                return p.depositAllItems(target, npc, true);
                
            case DEPOSIT_RATIO:
                return p.depositRatio(npc, target, false);

            case WITHDRAW_RATIO:
                return p.depositRatio(target, npc, true);
                
            case DEPOSIT_EXACT:
                return p.depositExact(npc, target, false);
            
            case WITHDRAW_EXACT:
                return p.depositExact(target, npc, true);
            
            case FILL_MINIMUM:
                return p.fillAtLeast(npc, target, false);
                
            case TAKE_MINIMUM:
                return p.fillAtLeast(target, npc, true);

            default:
                return 0;
        }
    }


    public List<RoutePoint> getEntries() {
        return points;
    }

}
