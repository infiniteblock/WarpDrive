package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.RadarEcho;
import cr0s.warpdrive.data.StarMapRegistry;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.EnumRadarMode;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.fml.common.Optional;

public class TileEntityRadar extends TileEntityAbstractEnergyConsumer {
	
	private ArrayList<RadarEcho> results;
	
	// persistent properties
	private int radius = 0;
	
	private boolean isScanning = false;
	private int scanning_radius = 0;
	private int scanning_countdown_ticks = 0;
	
	public TileEntityRadar() {
		super();
		
		peripheralName = "warpdriveRadar";
		addMethods(new String[] {
				"getGlobalPosition",
				"radius",
				"start",
				"getScanDuration",
				"getResults",
				"getResultsCount",
				"getResult"
			});
		CC_scripts = Arrays.asList("scan", "ping");
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		final IBlockState blockState = world.getBlockState(pos);
		if (!isScanning) {
			if (computer_isConnected()) {
				updateBlockState(blockState, BlockRadar.MODE, EnumRadarMode.ACTIVE);
			} else {
				updateBlockState(blockState, BlockRadar.MODE, EnumRadarMode.INACTIVE);
			}
		} else {
			updateBlockState(blockState, BlockRadar.MODE, EnumRadarMode.SCANNING);
			try {
				scanning_countdown_ticks--;
				if (scanning_countdown_ticks <= 0) {
					results = WarpDrive.starMap.getRadarEchos(this, scanning_radius);
					isScanning = false;
					if (WarpDriveConfig.LOGGING_RADAR) {
						WarpDrive.logger.info(String.format("%s Scan found %d results in %d radius...",
						                                    this, results.size(), scanning_radius));
					}
				}
			} catch (final Exception exception) {
				exception.printStackTrace();
			}
		}
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		radius = tagCompound.getInteger("radius");
		isScanning = tagCompound.getBoolean("isScanning");
		scanning_radius = tagCompound.getInteger("scanning_radius");
		scanning_countdown_ticks = tagCompound.getInteger("scanning_countdown");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger("radius", radius);
		tagCompound.setBoolean("isScanning", isScanning);
		tagCompound.setInteger("scanning_radius", scanning_radius);
		tagCompound.setInteger("scanning_countdown", scanning_countdown_ticks);
		
		return tagCompound;
	}
	
	private int calculateEnergyRequired(final int parRadius) {
		return (int) Math.round(Math.max(WarpDriveConfig.RADAR_SCAN_MIN_ENERGY_COST,
				  WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[0]
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[1] * parRadius
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[2] * parRadius * parRadius
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[3] * parRadius * parRadius * parRadius));
	}
	
	private int calculateScanDuration(final int parRadius) {
		return (int) Math.round(20 * Math.max(WarpDriveConfig.RADAR_SCAN_MIN_DELAY_SECONDS,
				  WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[0]
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[1] * parRadius
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[2] * parRadius * parRadius
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[3] * parRadius * parRadius * parRadius));
	}
	
	// Common OC/CC methods
	public Object[] getGlobalPosition() {
		// check for optical sensors
		if (false) {
			return new Object[] { false, StarMapRegistry.GALAXY_UNDEFINED, pos.getX(), pos.getY(), pos.getZ(), Commons.format(world) };
		}
		
		// check for registered celestial object
		final CelestialObject celestialObject = CelestialObjectManager.get(world, pos.getX(), pos.getZ());
		if (celestialObject == null) {
			return new Object[] { false, StarMapRegistry.GALAXY_UNDEFINED, pos.getX(), pos.getY(), pos.getZ(), Commons.format(world) };
		}
		
		// get actual coordinates
		final String galaxyName = StarMapRegistry.getGalaxyName(celestialObject, pos.getX(), pos.getY(), pos.getZ());
		final Vector3 vec3Position = StarMapRegistry.getUniversalCoordinates(celestialObject, pos.getX(), pos.getY(), pos.getZ());
		return new Object[] { true, galaxyName, vec3Position.x, vec3Position.y, vec3Position.z, celestialObject.getDisplayName() };
	}
	
	private Object[] radius(final Object[] arguments) {
		if (arguments.length == 1 && !isScanning) {
			final int newRadius;
			try {
				newRadius = Commons.toInt(arguments[0]);
			} catch (final Exception exception) {
				return new Integer[] { radius };
			}
			radius = Commons.clamp(0, 10000, newRadius);
		}
		return new Integer[] { radius };
	}
	
	@Override
	public Object[] getEnergyRequired() {
		return new Object[] { calculateEnergyRequired(radius) };
	}
	
	private Object[] getScanDuration(final Object[] arguments) {
		try {
			if (arguments.length == 1 && arguments[0] != null) {
				return new Object[] { 0.050D * calculateScanDuration(Commons.toInt(arguments[0])) };
			}
		} catch (final Exception exception) {
			return new Integer[] { -1 };
		}
		return new Integer[] { -1 };
	}
	
	private Object[] start() {
		if (isScanning) {
			return new Object[] { false, String.format("Already scanning, %.3f seconds to go", scanning_countdown_ticks / 20.0F) };
		}
		
		// always clear results
		results = null;
		
		// validate parameters
		if (radius <= 0 || radius > 10000) {
			radius = 0;
			return new Object[] { false, "Invalid radius" };
		}
		final int energyRequired = calculateEnergyRequired(radius);
		if (!energy_consume(energyRequired, false)) {
			return new Object[] { false, "Insufficient energy" };
		}
		
		// Begin searching
		scanning_radius = radius;
		scanning_countdown_ticks = calculateScanDuration(radius);
		isScanning = true;
		updateBlockState(null, BlockRadar.MODE, EnumRadarMode.SCANNING);
		if (WarpDriveConfig.LOGGING_RADAR) {
			WarpDrive.logger.info(String.format("%s Starting scan over radius %d for %s %s, results expected in %s ticks",
			                                    this, scanning_radius,
			                                    EnergyWrapper.format(energyRequired, null), WarpDriveConfig.ENERGY_DISPLAY_UNITS,
			                                    scanning_countdown_ticks));
		}
		return new Object[] { true, String.format("Scanning started, %.3f seconds to go", scanning_countdown_ticks / 20.0F) };
	}
	
	private Object[] getResults() {
		if (results == null) {
			return null;
		}
		final Object[] objectResults = new Object[results.size()];
		int index = 0;
		for (final RadarEcho radarEcho : results) {
			objectResults[index++] = new Object[] {
					radarEcho.type,
					radarEcho.name == null ? "" : radarEcho.name,
					radarEcho.x, radarEcho.y, radarEcho.z,
					radarEcho.mass };
		}
		return objectResults;
	}
	
	private Object[] getResultsCount() {
		if (results != null) {
			return new Integer[] { results.size() };
		}
		return new Integer[] { -1 };
	}
	
	private Object[] getResult(final Object[] arguments) {
		if (arguments.length == 1 && (results != null)) {
			final int index;
			try {
				index = Commons.toInt(arguments[0]);
			} catch(final Exception exception) {
				return new Object[] { false, COMPUTER_ERROR_TAG, null, 0, 0, 0 };
			}
			if (index >= 0 && index < results.size()) {
				final RadarEcho radarEcho = results.get(index);
				if (radarEcho != null) {
					return new Object[] {
							true,
							radarEcho.type,
							radarEcho.name == null ? "" : radarEcho.name,
							radarEcho.x, radarEcho.y, radarEcho.z,
							radarEcho.mass };
				}
			}
		}
		return new Object[] { false, COMPUTER_ERROR_TAG, null, 0, 0, 0 };
	}
	
	// OpenComputers callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] getGlobalPosition(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getGlobalPosition();
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] radius(final Context context, final Arguments arguments) {
		return radius(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] getScanDuration(final Context context, final Arguments arguments) {
		return getScanDuration(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] start(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return start();
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] getResults(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getResults();
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] getResultsCount(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getResultsCount();
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] getResult(final Context context, final Arguments arguments) {
		return getResult(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "getGlobalPosition":
			return getGlobalPosition();
			
		case "radius":
			return radius(arguments);
			
		case "getScanDuration":
			return getScanDuration(arguments);
			
		case "start":
			return start();
			
		case "getResults":
			return getResults();
			
		case "getResultsCount":
			return getResultsCount();
			
		case "getResult":
			return getResult(arguments);
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.RADAR_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
}
