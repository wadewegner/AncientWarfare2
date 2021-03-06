/*
 Copyright 2012 John Cummens (aka Shadowmage, Shadowmage4513)
 This software is distributed under the terms of the GNU General Public License.
 Please see COPYING for precise license information.

 This file is part of Ancient Warfare.

 Ancient Warfare is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Ancient Warfare is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Ancient Warfare.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.RayTraceResult;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;

import java.util.List;
import java.util.UUID;

public class TEGateProxy extends TileEntity implements ITickable{

    private EntityGate owner = null;
    private UUID entityID = null;
    private int noParentTicks = 0;

    public void setOwner(EntityGate gate) {
        this.owner = gate;
        this.entityID = owner.getPersistentID();
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("msb") && tag.hasKey("lsb")) {
            long msb = tag.getLong("msb");
            long lsb = tag.getLong("lsb");
            entityID = new UUID(msb, lsb);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (this.entityID != null) {
            tag.setLong("msb", entityID.getMostSignificantBits());
            tag.setLong("lsb", entityID.getLeastSignificantBits());
        }
        return tag;
    }

    public boolean onBlockClicked(EntityPlayer player, EnumHand hand) {
        return this.owner == null || this.owner.processInitialInteract(player, hand);
    }

    public void onBlockAttacked(EntityPlayer player) {
        if(this.owner != null){
            DamageSource source = player!=null ? DamageSource.causePlayerDamage(player) : DamageSource.GENERIC;
            this.owner.attackEntityFrom(source, 1);
        }
    }

    public ItemStack onBlockPicked(RayTraceResult target) {
        if(this.owner != null){
            return owner.getPickedResult(target);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void update() {
        if (!hasWorld() || this.world.isRemote) {
            return;
        }
        if (this.entityID == null) {
            this.noParentTicks++;
        }
        else if (this.owner == null) {
            this.noParentTicks++;

            List<Entity> entities = this.world.loadedEntityList;
            for (Entity ent : entities) {
                if (ent instanceof EntityGate && ent.getPersistentID() != null && ent.getPersistentID().equals(entityID)) {
                    this.owner = (EntityGate) ent;
                    this.noParentTicks = 0;
                    break;
                }
            }
        }
        if (this.noParentTicks >= 100 || (owner != null && owner.isDead)) {
            owner = null;
            this.world.setBlockToAir(pos);
        }
    }

    public boolean isGateClosed() {
        return owner != null && owner.isClosed();
    }
}
