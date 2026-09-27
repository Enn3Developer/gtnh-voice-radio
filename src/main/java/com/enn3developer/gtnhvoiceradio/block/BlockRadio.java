package com.enn3developer.gtnhvoiceradio.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gtnhvoiceradio.GtnhVoiceRadio;
import com.enn3developer.gtnhvoiceradio.server.RadioStation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockRadio extends BlockContainer {

    // Horizontal facing metadata (2..5 like furnaces); 0 renders the front on the south side for the item.
    private static final int[] FACING_BY_YAW = { 2, 5, 3, 4 };

    @SideOnly(Side.CLIENT)
    private IIcon front;
    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockRadio() {
        super(Material.wood);
        setHardness(1.5f);
        setStepSound(soundTypeWood);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(GtnhVoiceRadio.MODID + ".radio");
        setBlockTextureName(GtnhVoiceRadio.MODID + ":radio_side");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileRadio();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int yaw = MathHelper.floor_double(placer.rotationYaw * 4.0f / 360.0f + 0.5) & 3;
        world.setBlockMetadataWithNotify(x, y, z, FACING_BY_YAW[yaw], 2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        if (!(world.getTileEntity(x, y, z) instanceof TileRadio radio)) return false;

        boolean on = radio.toggle();
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.click", 0.3f, on ? 0.6f : 0.5f);
        if (!on) {
            player.addChatMessage(new ChatComponentTranslation("gtnhvoiceradio.chat.off"));
        } else {
            String title = RadioStation.INSTANCE.currentTitle();
            player.addChatMessage(
                title == null ? new ChatComponentTranslation("gtnhvoiceradio.chat.no_station")
                    : new ChatComponentTranslation("gtnhvoiceradio.chat.on", title));
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName());
        front = register.registerIcon(GtnhVoiceRadio.MODID + ":radio_front");
        top = register.registerIcon(GtnhVoiceRadio.MODID + ":radio_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 0 || side == 1) return top;
        int facing = meta == 0 ? 3 : meta;
        return side == facing ? front : blockIcon;
    }
}
