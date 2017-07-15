package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.world.World;

public enum EnumShipMovementType implements IStringSerializable {
	
	//               hasConfig   name                   description                           maximumDistance_blocks                    energyRequired                                 warmup_seconds                        sickness_seconds                      cooldown_seconds       
	HYPERSPACE_ENTERING ( true , "hyperspace_entering", "entering hyperspace"  , new double[] {    128D, 0D, 0D, 0D, 0D }, new double[] { 10000000D,  0D,   0D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D }, new double[] { 40D, 0D, 0D, 0D, 0D } ),
	HYPERSPACE_EXITING  ( true , "hyperspace_exiting" , "existing hyperspace"  , new double[] {    128D, 0D, 0D, 0D, 0D }, new double[] { 10000000D,  0D,   0D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D }, new double[] { 40D, 0D, 0D, 0D, 0D } ),
	HYPERSPACE_MOVING   ( true , "hyperspace_moving"  , "moving in hyperspace" , new double[] {  12800D, 0D, 0D, 0D, 0D }, new double[] {    10000D,  1D,  10D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D }, new double[] { 20D, 0D, 0D, 0D, 0D } ),
	PLANET_LANDING      ( true , "planet_landing"     , "landing on a planet"  , new double[] {    128D, 0D, 0D, 0D, 0D }, new double[] {    10000D, 10D, 100D, 0D, 0D }, new double[] { 20D, 0D, 0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D }, new double[] { 60D, 0D, 0D, 0D, 0D } ),
	PLANET_MOVING       ( true , "planet_moving"      , "moving on a planet"   , new double[] {    128D, 0D, 0D, 0D, 0D }, new double[] {      100D, 10D, 100D, 0D, 0D }, new double[] {  5D, 0D, 0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D } ),
	PLANET_TAKEOFF      ( true , "planet_takeoff"     , "taking off a planet"  , new double[] {    128D, 0D, 0D, 0D, 0D }, new double[] {    10000D, 10D, 100D, 0D, 0D }, new double[] { 20D, 0D, 0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D }, new double[] { 60D, 0D, 0D, 0D, 0D } ),
	SPACE_MOVING        ( true , "space_moving"       , "moving in space"      , new double[] {    256D, 0D, 0D, 0D, 0D }, new double[] {     1000D, 10D, 100D, 0D, 0D }, new double[] { 10D, 0D, 0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D } ),
	GATE_ACTIVATING     ( true , "gate_activating"    , "activating a jumpgate", new double[] { 100000D, 0D, 0D, 0D, 0D }, new double[] {    20000D, 10D, 100D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D }, new double[] { 30D, 0D, 0D, 0D, 0D }, new double[] { 10D, 0D, 0D, 0D, 0D } ),
	CREATIVE            ( false, ""                   , "admin command"        , new double[] { 999999D, 0D, 0D, 0D, 0D }, new double[] {        0D,  0D,   0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D } ),
	NONE                ( false, ""                   , "idle, disabled, etc." , new double[] {      0D, 0D, 0D, 0D, 0D }, new double[] {        0D,  0D,   0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D }, new double[] {  0D, 0D, 0D, 0D, 0D } ),
	;
	// nota: empty names won't show up in the configuration file
	
	public final boolean hasConfiguration;
	private final String unlocalizedName;
	private final String description;
	public final double[] maximumDistanceDefault;
	public final double[] energyRequiredDefault;
	public final double[] warmupDefault;
	public final double[] sicknessDefault;
	public final double[] cooldownDefault;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumShipMovementType> ID_MAP = new HashMap<>();
	
	static {
		length = EnumShipMovementType.values().length;
		for (EnumShipMovementType componentType : values()) {
			ID_MAP.put(componentType.ordinal(), componentType);
		}
	}
	
	EnumShipMovementType(final boolean hasConfiguration, final String unlocalizedName, final String description,
	                     final double[] maximumDistanceDefault,
	                     final double[] energyRequiredDefault,
	                     final double[] warmupDefault,
	                     final double[] sicknessDefault,
	                     final double[] cooldownDefault) {
		this.hasConfiguration = hasConfiguration;
		this.unlocalizedName = unlocalizedName;
		this.description = description;
		this.warmupDefault = warmupDefault;
		this.energyRequiredDefault = energyRequiredDefault;
		this.cooldownDefault = cooldownDefault;
		this.sicknessDefault = sicknessDefault;
		this.maximumDistanceDefault = maximumDistanceDefault;
	}
	
	public static EnumShipMovementType get(final int damage) {
		return ID_MAP.get(damage);
	}
	
	@Nonnull
	@Override
	public String getName() { return unlocalizedName; }
	
	@Nonnull
	public String getDescription() { return description; }
	
	public static EnumShipMovementType compute(final World worldSource, final int xCurrent, final int yMin, final int yMax, final int zCurrent, 
	                                           final EnumShipControllerCommand command, final int yMove, final StringBuilder reason) {
		
		if (command == EnumShipControllerCommand.GATE) {
			return GATE_ACTIVATING;
		}
		
		final CelestialObject celestialObjectSource = CelestialObjectManager.get(worldSource, xCurrent, zCurrent);
		final boolean isInSpace = celestialObjectSource.isSpace();
		final boolean isInHyperSpace = celestialObjectSource.isHyperspace();
		
		switch (command) {
		case HYPERDRIVE:
			if (isInHyperSpace) {
				return HYPERSPACE_EXITING;
			} else if (isInSpace) {
				return HYPERSPACE_ENTERING;
			}
			reason.append("Unable to reach hyperspace from a planet");
			return null;
			
		case MANUAL:
			final boolean toSpace = (yMove > 0) && (yMax + yMove > 255) && (!isInSpace) && (!isInHyperSpace);
			if (toSpace) {
				return PLANET_TAKEOFF;
			}
			
			final boolean fromSpace = (yMove < 0) && (yMin + yMove < 0) && (!isInHyperSpace);
			if (fromSpace) {
				return PLANET_LANDING;
			}
			
			if (isInHyperSpace) {
				return HYPERSPACE_MOVING;
			}
			if (isInSpace) {
				return SPACE_MOVING;
			}
			return PLANET_MOVING;
			
		// case GATE_JUMP:
		//	return GATE_ACTIVATING;
		
		case IDLE:
		case MAINTENANCE:
		case OFFLINE:
		case SUMMON:
			return NONE;
		}
		
		// invalid command?
		reason.append(String.format("Invalid command '%s'", command));
		return null;
	}
}
