package net.shadowmage.ancientwarfare.automation.tile.warehouse;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.shadowmage.ancientwarfare.core.config.AWLog;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.inventory.InventoryBasic;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.BlockPosition;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

public class TileWarehouseInput extends TileEntity implements IInventory, IInteractableTile, IControlledTile, ITickable
{

private InventoryBasic inventory;

private BlockPosition controllerPosition = null;

private boolean init;

public TileWarehouseInput()
  {
  inventory = new InventoryBasic(27);
  }

@Override
public void setControllerPosition(BlockPosition position)
  {
  this.controllerPosition = position;
  AWLog.logDebug("set controller position to: "+position);
  this.init = this.controllerPosition!=null;
  }

@Override
public void validate()
  {
  super.validate();
  init = false;
  }

@Override
public void invalidate()
  {  
  super.invalidate();
  init = false;
  if(controllerPosition!=null && world.blockExists(controllerPosition.x, controllerPosition.y, controllerPosition.z))
    {
    TileEntity te = world.getTileEntity(controllerPosition.x, controllerPosition.y, controllerPosition.z);
    if(te instanceof WorkSiteWarehouse)
      {
      WorkSiteWarehouse warehouse = (WorkSiteWarehouse)te;
      BlockPosition min = warehouse.getWorkBoundsMin();
      BlockPosition max = warehouse.getWorkBoundsMax();
      if(x>=min.x && x<=max.x && y>=min.y && y<=max.y && z>=min.z && z<=max.z)
        {
        warehouse.removeInputBlock(this);
        }
      }
    }
  controllerPosition = null;
  }

@Override
public void update()
  {  
  if(!init)
    {
    init = true;
    AWLog.logDebug("scanning for controller...");
    for(TileEntity te : WorldTools.getTileEntitiesInArea(world, x-16, y-4, z-16, x+16, y+4, z+16))
      {
      if(te instanceof WorkSiteWarehouse)
        {
        WorkSiteWarehouse warehouse = (WorkSiteWarehouse)te;
        BlockPosition min = warehouse.getWorkBoundsMin();
        BlockPosition max = warehouse.getWorkBoundsMax();
        if(x>=min.x && x<=max.x && y>=min.y && y<=max.y && z>=min.z && z<=max.z)
          {
          warehouse.addInputBlock(this);
          controllerPosition = new BlockPosition(warehouse.x, warehouse.y, warehouse.z);
          warehouse.onInputInventoryUpdated(this);
          break;
          }
        }
      } 
    }  
  }

@Override
public void markDirty()
  {  
  super.markDirty();
  if(this.controllerPosition!=null)
    {
    TileEntity te = world.getTileEntity(controllerPosition.x, controllerPosition.y, controllerPosition.z);
    if(te instanceof WorkSiteWarehouse)
      {
      ((WorkSiteWarehouse) te).onInputInventoryUpdated(this);
      }
    }
  }

@Override
public void readFromNBT(NBTTagCompound tag)
  {
  super.readFromNBT(tag);
  inventory.readFromNBT(tag.getCompoundTag("inventory"));
  }

@Override
public void writeToNBT(NBTTagCompound tag)
  {
  super.writeToNBT(tag);
  NBTTagCompound tag1 = new NBTTagCompound();
  inventory.writeToNBT(tag1);
  tag.setTag("inventory", tag1);
  }

@Override
public int getSizeInventory()
  {
  return inventory.getSizeInventory();
  }

@Override
public ItemStack getStackInSlot(int var1)
  {
  return inventory.getStackInSlot(var1);
  }

@Override
public ItemStack decrStackSize(int var1, int var2)
  {
  return inventory.decrStackSize(var1, var2);
  }

@Override
public ItemStack getStackInSlotOnClosing(int var1)
  {
  return inventory.getStackInSlotOnClosing(var1);
  }

@Override
public void setInventorySlotContents(int var1, ItemStack var2)
  {
  inventory.setInventorySlotContents(var1, var2);
  }

@Override
public String getInventoryName()
  {
  return inventory.getInventoryName();
  }

@Override
public boolean hasCustomInventoryName()
  {
  return false;
  }

@Override
public int getInventoryStackLimit()
  {
  return 64;
  }

@Override
public boolean isUseableByPlayer(EntityPlayer var1)
  {
  return true;
  }

@Override
public void openInventory()
  {
  
  }

@Override
public void closeInventory()
  {
  
  }

@Override
public boolean isItemValidForSlot(int var1, ItemStack var2)
  {
  return true;
  }

@Override
public boolean onBlockClicked(EntityPlayer player)
  {
  if(!player.world.isRemote)
    {
//    NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WAREHOUSE_INPUT, x, y, z);
    }
  return true;
  }


}
