package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ICrafting;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;

public class ContainerFilterSequential extends ContainerFilter
{
    protected final TileEntityFilterSequential tefiseq;
    public int filterPosition;

    public ContainerFilterSequential(EntityPlayer player, TileEntityFilterSequential te)
    {
        super(player, te);
        this.tefiseq = te;
    }

    @Override
    public void onCraftGuiOpened(ICrafting listener)
    {
        super.onCraftGuiOpened(listener);
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        for (int i = 0; i < this.listeners.size(); ++i)
        {
            ICrafting listener = this.listeners.get(i);

            if (this.tefiseq.getFilterPosition() != this.filterPosition)
            {
                listener.sendProgressBarUpdate(this, 0, this.tefiseq.getFilterPosition());
            }
        }

        this.filterPosition = this.tefiseq.getFilterPosition();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        if (id == 0)
        {
            this.filterPosition = data;
        }
    }
}
