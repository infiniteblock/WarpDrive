package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.ForceFieldRegistry;
import cr0s.warpdrive.data.Vector3;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;

public class TileEntityAbstractForceField extends TileEntityAbstractEnergy implements IBeamFrequency {
	
	// persistent properties
	protected int beamFrequency = -1;
	public boolean isEnabled = true;
	protected boolean isConnected = false;
	
	// computed properties
	protected Vector3 vRGB;
	
	public TileEntityAbstractForceField() {
		super();
		
		addMethods(new String[] {
			"enable",
			"beamFrequency"
		});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		if (beamFrequency >= 0 && beamFrequency <= IBeamFrequency.BEAM_FREQUENCY_MAX) {
			ForceFieldRegistry.updateInRegistry(this);
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// Frequency is not set
		final boolean new_isConnected = beamFrequency > 0 && beamFrequency <= IBeamFrequency.BEAM_FREQUENCY_MAX;
		if (isConnected != new_isConnected) {
			isConnected = new_isConnected;
			markDirty();
		}
	}
	
	@Override
	public void invalidate() {
		ForceFieldRegistry.removeFromRegistry(this);
		super.invalidate();
	}
	
	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		// reload chunks as needed
		// ForceFieldRegistry.removeFromRegistry(this);
	}
	
	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}
	
	@Override
	public void setBeamFrequency(final int parBeamFrequency) {
		if (beamFrequency != parBeamFrequency && (parBeamFrequency <= BEAM_FREQUENCY_MAX) && (parBeamFrequency > BEAM_FREQUENCY_MIN)) {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("%s Beam frequency set from %d to %d",
				                                    this, beamFrequency, parBeamFrequency));
			}
			if (hasWorld()) {
				ForceFieldRegistry.removeFromRegistry(this);
			}
			beamFrequency = parBeamFrequency;
			vRGB = IBeamFrequency.getBeamColor(beamFrequency);
		}
		markDirty();
		if (hasWorld()) {
			ForceFieldRegistry.updateInRegistry(this);
		}
	}
	
	@Override
		public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		setBeamFrequency(tagCompound.getInteger(BEAM_FREQUENCY_TAG));
		isEnabled = !tagCompound.hasKey("isEnabled") || tagCompound.getBoolean("isEnabled");
		isConnected = tagCompound.getBoolean("isConnected");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger(BEAM_FREQUENCY_TAG, beamFrequency);
		tagCompound.setBoolean("isEnabled", isEnabled);
		tagCompound.setBoolean("isConnected", isConnected);
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		tagCompound.setBoolean("isConnected", isConnected);
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(final NetworkManager networkManager, final SPacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		readFromNBT(tagCompound);
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] enable(final Context context, final Arguments arguments) {
		return enable(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] beamFrequency(final Context context, final Arguments arguments) {
		if (arguments.count() == 1) {
			setBeamFrequency(arguments.checkInteger(0));
		}
		return new Integer[] { beamFrequency };
	}
	
	// Common OC/CC methods
	public Object[] enable(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			final boolean enable;
			try {
				enable = Commons.toBool(arguments[0]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on enable(): Boolean expected for 1st argument %s",
					                                     this, arguments[0]));
				}
				return new Object[] { isEnabled };
			}
			isEnabled = enable;
		}
		return new Object[] { isEnabled };
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		try {
			switch (methodName) {
			case "enable":
				return enable(arguments);
				
			case "beamFrequency":
				if (arguments.length == 1 && arguments[0] != null) {
					setBeamFrequency(Commons.toInt(arguments[0]));
				}
				return new Integer[] { beamFrequency };
			}
		} catch (final Exception exception) {
			exception.printStackTrace();
			return new String[] { exception.getMessage() };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s Beam \'%d\' %s",
		                     getClass().getSimpleName(),
		                     beamFrequency,
		                     Commons.format(world, pos));
	}
}
