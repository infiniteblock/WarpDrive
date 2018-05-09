package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ICelestialObject;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.RandomCollection;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.XmlFileManager;
import cr0s.warpdrive.config.structures.StructureGroup;
import org.w3c.dom.Element;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import net.minecraftforge.common.util.Constants.NBT;

/**
 * An astronomical object or celestial object is a naturally occurring physical entity, association, or structure in the observable universe.
 * They can be a planet, a more abstract construct like solar system (space dimension) or the all mighty hyperspace.
 *
 * @author LemADEC
 */
public class CelestialObject implements Cloneable, IStringSerializable, ICelestialObject {
	
	public static final double GRAVITY_NONE = 0.0D;
	public static final double GRAVITY_LEGACY_SPACE = -1.0D;
	public static final double GRAVITY_LEGACY_HYPERSPACE = -2.0D;
	public static final double GRAVITY_NORMAL = 1.0D;
	public static final String PROVIDER_AUTO       = "auto";
	public static final String PROVIDER_HYPERSPACE = "WarpDriveHyperspace";
	public static final String PROVIDER_SPACE      = "WarpDriveSpace";
	public static final String PROVIDER_OTHER      = "other";
	public static final String PROVIDER_NONE       = "";
	
	// unique name so children can refer to parent
	public String id;
	
	public String parentId;
	public CelestialObject parent;
	private boolean isParentResolved;
	protected int parentCenterX, parentCenterZ;
	
	public int borderRadiusX, borderRadiusZ;
	
	private String displayName;
	private String description;
	private NBTTagCompound tagCompound;
	
	private boolean isVirtual;
	public int dimensionId;
	public int dimensionCenterX, dimensionCenterZ;
	private double gravity;
	private boolean isBreathable;
	protected String provider;
	private boolean isHyperspace;
	
	private final RandomCollection<StructureGroup> randomStructures = new RandomCollection<>();
	
	public ColorData backgroundColor;
	public float baseStarBrightness;
	public float vanillaStarBrightness;
	public float opacityCelestialObjects;
	public ColorData colorFog;
	public ColorData factorFog;
	
	public LinkedHashSet<RenderData> setRenderData;
	
	public CelestialObject(final String location, final String parentId, Element elementCelestialObject) throws InvalidXmlException {
		loadFromXmlElement(location, parentId, elementCelestialObject);
	}
	
	public CelestialObject(final int parDimensionId, final int parDimensionCenterX, final int parDimensionCenterZ,
	                       final int parBorderRadiusX, final int parBorderRadiusZ,
	                       final String parParentId, final int parParentCenterX, final int parParentCenterZ) {
		isVirtual = false;
		isParentResolved = false;
		dimensionId = parDimensionId;
		dimensionCenterX = parDimensionCenterX;
		dimensionCenterZ = parDimensionCenterZ;
		borderRadiusX = parBorderRadiusX;
		borderRadiusZ = parBorderRadiusZ;
		parentId = parParentId;
		parentCenterX = parParentCenterX;
		parentCenterZ = parParentCenterZ;
	}
	
	public CelestialObject(final NBTTagCompound tagCompound) {
		readFromNBT(tagCompound);
	}
	
	@Override
	public String getName() {
		return id;
	}
	
	protected boolean loadFromXmlElement(final String location, final String parentElementId, final Element elementCelestialObject) throws InvalidXmlException {
		// get identity
		id = elementCelestialObject.getAttribute("id");
		if (id.isEmpty()) {
			throw new InvalidXmlException(String.format("Celestial object %s is missing an id attribute!", location));
		}
		
		WarpDrive.logger.info("- found Celestial object " + id);
		
		// get optional parent element, defaulting to parent defined by element hierarchy
		parentId = parentElementId;
		final List<Element> listParents = XmlFileManager.getChildrenElementByTagName(elementCelestialObject,"parent");
		if (listParents.size() > 1) {
			throw new InvalidXmlException(String.format("Celestial object %s can only have up to one parent element", id));
		}
		if (listParents.size() == 1) {
			final Element elementParent = listParents.get(0);
			
			// save linked parent
			final String parentIdRead = elementParent.getAttribute("id");
			if (!parentIdRead.isEmpty()) {
				parentId = parentIdRead;
			}
			
			// get required center element
			final List<Element> listElements = XmlFileManager.getChildrenElementByTagName(elementParent, "center");
			if (listElements.size() != 1) {
				throw new InvalidXmlException(String.format("Celestial object %s parent requires exactly one center element", id));
			}
			final Element elementCenter = listElements.get(0);
			parentCenterX = Integer.parseInt(elementCenter.getAttribute("x"));
			parentCenterZ = Integer.parseInt(elementCenter.getAttribute("z"));
		}
		
		// get required size element
		{
			final List<Element> listElements = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "size");
			if (listElements.size() != 1) {
				throw new InvalidXmlException(String.format("Celestial object %s requires exactly one size element", id));
			}
			final Element elementSize = listElements.get(0);
			borderRadiusX = Integer.parseInt(elementSize.getAttribute("x")) / 2;
			borderRadiusZ = Integer.parseInt(elementSize.getAttribute("z")) / 2;
		}
		
		// get optional name elements
		{
			final List<Element> listElements = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "name");
			if (listElements.size() > 1) {
				throw new InvalidXmlException(String.format("Celestial object %s can only have up to one name element", id));
			}
			if (listElements.size() == 1) {
				final Element elementName = listElements.get(0);
				displayName = elementName.getTextContent();
			} else {
				displayName = id;
			}
		}
		
		// get optional description element
		{
			final List<Element> listElements = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "description");
			if (listElements.size() > 1) {
				throw new InvalidXmlException(String.format("Celestial object %s can only have up to one description element", id));
			}
			if (listElements.size() == 1) {
				final Element elementName = listElements.get(0);
				description = elementName.getTextContent();
			} else {
				description = "";
			}
		}
		
		// get optional NBT element
		{
			final List<Element> listElements = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "nbt");
			if (listElements.size() > 1) {
				throw new InvalidXmlException(String.format("Celestial object %s can only have up to one nbt element", id));
			}
			tagCompound = null;
			if (listElements.size() == 1) {
				final Element elementName = listElements.get(0);
				final String stringNBT = elementName.getTextContent();
				if (!stringNBT.isEmpty()) {
					try {
						tagCompound = (NBTTagCompound) JsonToNBT.getTagFromJson(stringNBT);
					} catch (NBTException exception) {
						throw new InvalidXmlException(String.format("Invalid nbt for Celestial object %s", id));
					}
				}
			}
		}
		
		// get optional dimension element
		final List<Element> listDimensions = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "dimension");
		if (listDimensions.size() > 1) {
			throw new InvalidXmlException(String.format("Celestial object %s can only have up to one dimension element", id));
		}
		if (listDimensions.size() == 0) {
			isVirtual = true;
			dimensionId = 0;
			gravity = GRAVITY_NORMAL;
			isBreathable = true;
			provider = PROVIDER_NONE;
			isHyperspace = false;
			dimensionCenterX = 0;
			dimensionCenterZ = 0;
		} else {
			isVirtual = false;
			
			final Element elementDimension = listDimensions.get(0);
			dimensionId = Integer.parseInt(elementDimension.getAttribute("id"));
			gravity = parseGravity(elementDimension.getAttribute("gravity"));
			isBreathable = Boolean.parseBoolean(elementDimension.getAttribute("isBreathable"));
			isHyperspace = Boolean.parseBoolean(elementDimension.getAttribute("isHyperspace"));
			
			// get optional provider element
			{
				final List<Element> listElements = XmlFileManager.getChildrenElementByTagName(elementDimension, "provider");
				if (listElements.size() > 1) {
					throw new InvalidXmlException(String.format("Celestial object %s dimension can only have up to one provider element", id));
				}
				if (listElements.size() == 1) {
					final Element element = listElements.get(0);
					provider = element.getAttribute("type");
				} else {
					provider = PROVIDER_AUTO;
				}
			}
			
			// get required center element
			final List<Element> listCenters = XmlFileManager.getChildrenElementByTagName(elementDimension, "center");
			if (listCenters.size() != 1) {
				throw new InvalidXmlException(String.format("Celestial object %s dimension requires exactly one center element", id));
			}
			final Element elementCenter = listCenters.get(0);
			dimensionCenterX = Integer.parseInt(elementCenter.getAttribute("x"));
			dimensionCenterZ = Integer.parseInt(elementCenter.getAttribute("z"));
			
			// get optional generate element(s)
			final StructureGroup structureGroup = new StructureGroup(null, null);
			randomStructures.add(structureGroup, "", "100");
			
			final List<Element> listGenerates = XmlFileManager.getChildrenElementByTagName(elementDimension, "generate");
			for (int indexElement = 0; indexElement < listGenerates.size(); indexElement++) {
				final Element elementGenerate = listGenerates.get(indexElement);
				final String locationGenerate = String.format("Celestial object %s generate %d/%d", id, indexElement + 1, listGenerates.size());
				parseGenerateElement(locationGenerate, elementGenerate);
			}
			
			// get optional effect element(s)
			// @TODO not implemented
		}
		
		// get optional skybox element
		final List<Element> listSkyboxes = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "skybox");
		if (listSkyboxes.size() > 1) {
			throw new InvalidXmlException(String.format("Celestial object %s can only have up to one skybox element", id));
		}
		if (listSkyboxes.isEmpty()) {
			backgroundColor = new ColorData(0.0F      , 0.0F       , 0.0F );
			baseStarBrightness = 0.0F;
			vanillaStarBrightness = 1.0F;
			opacityCelestialObjects = 1.0F;
			colorFog  = new ColorData(0.7529412F, 0.84705883F, 1.0F );
			factorFog = new ColorData(0.94F     , 0.94F      , 0.91F);
		} else {
			final Element elementSkybox = listSkyboxes.get(0);
			final String locationSkybox = String.format("Celestial object %s skybox 1/1", id);
			backgroundColor = getColorData(locationSkybox, elementSkybox, "backgroundColor" , 0.0F, 0.0F, 0.0F );
			baseStarBrightness =  getFloat(locationSkybox, elementSkybox, "starBrightnessBase", 0.0F);
			vanillaStarBrightness =  getFloat(locationSkybox, elementSkybox, "starBrightnessVanilla", 1.0F);
			opacityCelestialObjects = getFloat(locationSkybox, elementSkybox, "celestialObjectOpacity", 1.0F);
			colorFog  = getColorData(locationSkybox, elementSkybox, "fogColor" , 0.7529412F, 0.84705883F, 1.0F );
			factorFog = getColorData(locationSkybox, elementSkybox, "fogFactor", 0.94F     , 0.94F      , 0.91F);
		}
		
		// get optional render element(s)
		final List<Element> listRenders = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "render");
		setRenderData = new LinkedHashSet<>(listRenders.size());
		if (!listRenders.isEmpty()) {
			for (int indexElement = 0; indexElement < listRenders.size(); indexElement++) {
				final Element elementRender = listRenders.get(indexElement);
				final String locationRender = String.format("Celestial object %s render %d/%d", id, indexElement + 1, listRenders.size());
				final RenderData renderData = new RenderData(locationRender, elementRender);
				setRenderData.add(renderData);
			}
		}
		
		if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
			WarpDrive.logger.info("  loaded " + this);
		}
		
		return true;
	}
	
	private float getFloat(final String locationParent, final Element elementParent, final String tagName, final float value) throws InvalidXmlException {
		final List<Element> listElements = XmlFileManager.getChildrenElementByTagName(elementParent, tagName);
		if (listElements.size() > 1) {
			throw new InvalidXmlException(String.format("%s can only have up to one %s element", locationParent, tagName));
		}
		if (listElements.isEmpty()) {
			return value;
		} else {
			final Element elementSub = listElements.get(0);
			final String valueSub = elementSub.getTextContent();
			// final String locationChild = String.format("%s %s 1/1", locationParent, tagName);
			return Commons.clamp(0.0F, 1.0F, Float.parseFloat(valueSub));
		}
	}
	
	private ColorData getColorData(final String locationParent, final Element elementParent, final String tagName, final float red, final float green, final float blue) throws InvalidXmlException {
		final List<Element> listElements = XmlFileManager.getChildrenElementByTagName(elementParent, tagName);
		if (listElements.size() > 1) {
			throw new InvalidXmlException(String.format("%s can only have up to one %s element", locationParent, tagName));
		}
		if (listElements.isEmpty()) {
			return new ColorData(red, green, blue);
		} else {
			final Element elementChild = listElements.get(0);
			final String locationChild = String.format("%s %s 1/1", locationParent, tagName);
			return new ColorData(locationChild, elementChild);
		}
	}
	
	private static double parseGravity(final String stringGravity) {
		try {
			switch(stringGravity) {
			case "none"            : return GRAVITY_NONE;
			case "legacySpace"     : return GRAVITY_LEGACY_SPACE;
			case "legacyHyperspace": return GRAVITY_LEGACY_HYPERSPACE;
			case "normal"          : return GRAVITY_NORMAL;
			default:
				final double gravity = Double.parseDouble(stringGravity);
				if (gravity < 0.0D) {
					throw new RuntimeException();
				}
				return Math.min(gravity, 1.0D);
			}
		} catch (Exception exception) {
			WarpDrive.logger.error("Invalid gravity value, expecting none, legacySpace, legacyHyperspace, normal or a positive double. Found: " + stringGravity);
			exception.printStackTrace();
			return 1.0D;
		}
	}
	
	private void parseGenerateElement(final String location, final Element elementGenerate) throws InvalidXmlException {
		final String group = elementGenerate.getAttribute("group");
		if (group.isEmpty()) {
			throw new InvalidXmlException(location + " is missing a group attribute!");
		}
		
		final String name = elementGenerate.getAttribute("name");
		
		if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
			WarpDrive.logger.info("  + found Generate " + group + ":" + name);
		}
		
		final String stringRatio = elementGenerate.getAttribute("ratio");
		final String stringWeight = elementGenerate.getAttribute("weight");
		
		final StructureGroup structureGroup = new StructureGroup(group, name);
		randomStructures.add(structureGroup, stringRatio, stringWeight);
	}
	
	protected void resolveParent(final CelestialObject celestialObjectParent) {
		parent = celestialObjectParent;
		isParentResolved = true;
		if ( parent == null
		  && provider.equals(PROVIDER_HYPERSPACE) ) {
			isHyperspace = true;
		}
	}
	
	protected void lateUpdate() {
		if (provider.equals(PROVIDER_AUTO)) {
			/* if (isHyperspace()) {
				provider = PROVIDER_HYPERSPACE;
			} else /* @TODO need to define a key for Hyperspace */
			if (isSpace()) {
				provider = PROVIDER_SPACE;
			} else {
				provider = PROVIDER_OTHER;
			}
		}
	}
	
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public CelestialObject clone() {
		return new CelestialObject(dimensionId, dimensionCenterX, dimensionCenterZ, borderRadiusX, borderRadiusZ, parentId, parentCenterX, parentCenterZ);
	}
	
	public StructureGroup getRandomStructure(final Random random, final int x, final int z) {
		return randomStructures.getRandomEntry(random);
	}
	
	@Override
	public String getDisplayName() {
		return displayName == null ? "" : displayName;
	}
	
	@Override
	public String getDescription() {
		return description == null ? "" : description;
	}
	
	@Override
	public NBTTagCompound getTag() {
		return tagCompound;
	}
	
	@Override
	public boolean isVirtual() {
		return isVirtual;
	}
	
	@Override
	public boolean isHyperspace() {
		return isHyperspace;
	}
	
	@Override
	public boolean isSpace() {
		return !isHyperspace() && parent != null && parent.isHyperspace();
	}
	
	@Override
	public double getGravity() {
		return gravity;
	}
	
	@Override
	public boolean hasAtmosphere() {
		return isBreathable && !isHyperspace() && !isSpace();
	}
	
	// offset vector when moving from parent to this dimension
	public VectorI getEntryOffset() {
		return new VectorI(dimensionCenterX - parentCenterX, 0, dimensionCenterZ - parentCenterZ);
	}
	
	@Override
	public AxisAlignedBB getWorldBorderArea() {
		return new AxisAlignedBB(
			(dimensionCenterX - borderRadiusX),   0, (dimensionCenterZ - borderRadiusZ),
			(dimensionCenterX + borderRadiusX), 255, (dimensionCenterZ + borderRadiusZ) );
	}
	
	@Override
	public AxisAlignedBB getAreaToReachParent() {
		return new AxisAlignedBB(
			(dimensionCenterX - borderRadiusX), 250, (dimensionCenterZ - borderRadiusZ),
			(dimensionCenterX + borderRadiusX), 255, (dimensionCenterZ + borderRadiusZ) );
	}
	
	@Override
	public AxisAlignedBB getAreaInParent() {
		return new AxisAlignedBB(
			(parentCenterX - borderRadiusX), 0, (parentCenterZ - borderRadiusZ),
			(parentCenterX + borderRadiusX), 8, (parentCenterZ + borderRadiusZ) );
	}
	
	/**
	 * Verify that the given area is fully contained within the border.
	 * It's up to caller to verify if this celestial object is matched.
	 *
	 * @param aabb bounding box that should fit within border
	 * @return true if we're fully inside the border
	 */
	@Override
	public boolean isInsideBorder(final AxisAlignedBB aabb) {
		final double rangeX = Math.max(Math.abs(aabb.minX - dimensionCenterX), Math.abs(aabb.maxX - dimensionCenterX));
		final double rangeZ = Math.max(Math.abs(aabb.minZ - dimensionCenterZ), Math.abs(aabb.maxZ - dimensionCenterZ));
		return (rangeX <= borderRadiusX) && (rangeZ <= borderRadiusZ);
	}
	
	/**
	 * Verify that the given position is within the border.
	 * It's up to caller to verify if this celestial object is matched.
	 *
	 * @param x coordinates inside the celestial object
	 * @param z coordinates inside the celestial object
	 * @return true if we're fully inside the border
	 */
	@Override
	public boolean isInsideBorder(final double x, final double z) {
		final double rangeX = Math.abs(x - dimensionCenterX);
		final double rangeZ = Math.abs(z - dimensionCenterZ);
		return (rangeX <= borderRadiusX) && (rangeZ <= borderRadiusZ);
	}
	
	/**
	 * Compute distance to reach closest border, while inside the same dimension.
	 *
	 * @param x coordinates inside the celestial object
	 * @param z coordinates inside the celestial object
	 * @return 'square' distance to the closest border,
	 *          <=0 if we're inside, > 0 if we're outside
	 */
	public double getSquareDistanceOutsideBorder(final double x, final double z) {
		final double rangeX = Math.abs(x - dimensionCenterX);
		final double rangeZ = Math.abs(z - dimensionCenterZ);
		final double dX = rangeX - borderRadiusX;
		final double dZ = rangeZ - borderRadiusZ;
		if ( (rangeX <= borderRadiusX)
		  && (rangeZ <= borderRadiusZ) ) {
			// inside: both dX and dZ are negative, so the max is actually the closest to zero
			final double dMax = Math.max(dX, dZ);
			return - (dMax * dMax);
		} else if ( (rangeX > borderRadiusX)
		         && (rangeZ > borderRadiusZ) ) {
			// outside in a diagonal
			return (dX * dX + dZ * dZ);
		}
		// outside aligned: one is negative (inside), the other is positive (outside), so the max is the outside one
		final double dMax = Math.max(dX, dZ);
		return dMax * dMax;
	}
	
	/**
	 * Check if current space coordinates allow to enter this dimension atmosphere from space.
	 *
	 * @param dimensionId current position in parent dimension
	 * @param x current position in parent dimension
	 * @param z current position in parent dimension
	 * @return square distance to transition borders, 0 if we're in orbit of the object
	 */
	public double getSquareDistanceInParent(final int dimensionId, final double x, final double z) {
		// are we in another dimension?
		if (!isParentResolved || parent == null || dimensionId != parent.dimensionId) {
			return Double.POSITIVE_INFINITY;
		}
		// are we in orbit?
		if ( (Math.abs(x - parentCenterX) <= borderRadiusX)
		  && (Math.abs(z - parentCenterZ) <= borderRadiusZ) ) {
			return 0.0D;
		}
		// do the maths
		final double dx = Math.max(0.0D, Math.abs(x - parentCenterX) - borderRadiusX);
		final double dz = Math.max(0.0D, Math.abs(z - parentCenterZ) - borderRadiusZ);
		return dx * dx + dz * dz;
	}
	
	/**
	 * Check if given position is in this object orbit.
	 *
	 * @param dimensionId current position in parent dimension
	 * @param x current position in parent dimension
	 * @param z current position in parent dimension
	 * @return true if we're in orbit of the object
	 */
	@Override
	public boolean isInOrbit(final int dimensionId, final double x, final double z) {
		// are we in another dimension?
		if (!isParentResolved || parent == null || dimensionId != parent.dimensionId) {
			return false;
		}
		// are we in orbit?
		return (Math.abs(x - parentCenterX) <= borderRadiusX)
		    && (Math.abs(z - parentCenterZ) <= borderRadiusZ);
	}
	
	public void readFromNBT(final NBTTagCompound tagCompound) {
		isParentResolved = false;
		
		id = tagCompound.getString("id");
		
		parentId = tagCompound.getString("parentId");
		parentCenterX = tagCompound.getInteger("parentCenterX");
		parentCenterZ = tagCompound.getInteger("parentCenterZ");
		
		borderRadiusX = tagCompound.getInteger("borderRadiusX");
		borderRadiusZ = tagCompound.getInteger("borderRadiusZ");
		
		displayName = tagCompound.getString("displayName");
		description = tagCompound.getString("description");
		
		isVirtual = tagCompound.getBoolean("isVirtual");
		if (isVirtual) {
			dimensionId = 0;
			dimensionCenterX = 0;
			dimensionCenterZ = 0;
			gravity = GRAVITY_NORMAL;
			isBreathable = true;
			isHyperspace = false;
			provider = PROVIDER_NONE;
		} else {
			dimensionId = tagCompound.getInteger("dimensionId");
			dimensionCenterX = tagCompound.getInteger("dimensionCenterX");
			dimensionCenterZ = tagCompound.getInteger("dimensionCenterZ");
			gravity = tagCompound.getDouble("gravity");
			isBreathable = tagCompound.getBoolean("isBreathable");
			isHyperspace = tagCompound.getBoolean("isHyperspace");
			provider = tagCompound.getString("provider");
		}
		
		// randomStructures are server side only
		
		backgroundColor = new ColorData(tagCompound.getCompoundTag("backgroundColor"));
		baseStarBrightness = tagCompound.getFloat("baseStarBrightness");
		vanillaStarBrightness = tagCompound.getFloat("vanillaStarBrightness");
		opacityCelestialObjects = tagCompound.getFloat("opacityCelestialObjects");
		colorFog = new ColorData(tagCompound.getCompoundTag("colorFog"));
		factorFog = new ColorData(tagCompound.getCompoundTag("factorFog"));
		
		final NBTTagList tagListRenderData = tagCompound.getTagList("renderData", NBT.TAG_COMPOUND);
		final int countRender = tagListRenderData.tagCount();
		setRenderData = new LinkedHashSet<>(countRender);
		for (int indexRenderData = 0; indexRenderData < countRender; indexRenderData++) {
			final NBTTagCompound tagCompoundRenderData = tagListRenderData.getCompoundTagAt(indexRenderData);
			setRenderData.add(new RenderData(tagCompoundRenderData));
		}
	}
	
	public NBTTagCompound writeToNBT(final NBTTagCompound tagCompound) {
		tagCompound.setString("id", id);
		
		tagCompound.setString("parentId", parentId);
		tagCompound.setInteger("parentCenterX", parentCenterX);
		tagCompound.setInteger("parentCenterZ", parentCenterZ);
		
		tagCompound.setInteger("borderRadiusX", borderRadiusX);
		tagCompound.setInteger("borderRadiusZ", borderRadiusZ);
		
		if (displayName != null && !displayName.isEmpty()) {
			tagCompound.setString("displayName", displayName);
		}
		if (description != null && !description.isEmpty()) {
			tagCompound.setString("description", description);
		}
		
		tagCompound.setBoolean("isVirtual", isVirtual);
		if (!isVirtual) {
			tagCompound.setInteger("dimensionId", dimensionId);
			tagCompound.setInteger("dimensionCenterX", dimensionCenterX);
			tagCompound.setInteger("dimensionCenterZ", dimensionCenterZ);
			tagCompound.setDouble("gravity", gravity);
			tagCompound.setBoolean("isBreathable", isBreathable);
			tagCompound.setBoolean("isHyperspace", isHyperspace);
			tagCompound.setString("provider", provider);
		}
		
		// randomStructures are server side only
		
		tagCompound.setTag("backgroundColor", backgroundColor.writeToNBT(new NBTTagCompound()));
		tagCompound.setFloat("baseStarBrightness", baseStarBrightness);
		tagCompound.setFloat("vanillaStarBrightness", vanillaStarBrightness);
		tagCompound.setFloat("opacityCelestialObjects", opacityCelestialObjects);
		tagCompound.setTag("colorFog", colorFog.writeToNBT(new NBTTagCompound()));
		tagCompound.setTag("factorFog", factorFog.writeToNBT(new NBTTagCompound()));
		
		final NBTTagList nbtTagListRenderData = new NBTTagList();
		for (final RenderData renderData : setRenderData) {
			nbtTagListRenderData.appendTag(renderData.writeToNBT(new NBTTagCompound()));
		}
		tagCompound.setTag("renderData", nbtTagListRenderData);
		return tagCompound;
	}
	
	@Override
	public int hashCode() {
		return dimensionId << 16 + (dimensionCenterX >> 10) << 8 + (dimensionCenterZ >> 10);
	}
	
	@Override
	public boolean equals(final Object object) {
		if (object instanceof CelestialObject) {
			CelestialObject celestialObject = (CelestialObject) object;
			return dimensionId == celestialObject.dimensionId
				&& dimensionCenterX == celestialObject.dimensionCenterX
				&& dimensionCenterZ == celestialObject.dimensionCenterZ
				&& borderRadiusX == celestialObject.borderRadiusX
				&& borderRadiusZ == celestialObject.borderRadiusZ
				&& parentId.equals(celestialObject.parentId)
				&& parentCenterX == celestialObject.parentCenterX
				&& parentCenterZ == celestialObject.parentCenterZ;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		final String stringParent;
		if (isParentResolved && parent != null) {
			stringParent = String.format("Parent(%d @ %d %d)",
			                             parent.dimensionId, parentCenterX, parentCenterZ);
		} else {
			stringParent = String.format("Parent(%s @ %d %d)",
			                             parentId, parentCenterX, parentCenterZ);
		}
		if (isVirtual) {
			return String.format("CelestialObject %s [-Virtual- Border(%d %d) %s]",
			                     id,
			                     2 * borderRadiusX, 2 * borderRadiusZ,
			                     stringParent);
		} else {
			return String.format("CelestialObject %s [Dimension %d @ %d %d Border(%d %d) %s provider %s gravity %.3f isBreathable %s]",
			                     id, dimensionId, dimensionCenterX, dimensionCenterZ,
			                     2 * borderRadiusX, 2 * borderRadiusZ,
			                     stringParent,
			                     provider, gravity, isBreathable);
		}
	}
	
	
	public class ColorData {
		
		public float red;
		public float green;
		public float blue;
		
		ColorData(final float red, final float green, final float blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}
		
		ColorData(final String location, final Element elementColor) throws InvalidXmlException {
			try {
				red = Commons.clamp(0.0F, 1.0F, Float.parseFloat(elementColor.getAttribute("red")));
				green = Commons.clamp(0.0F, 1.0F, Float.parseFloat(elementColor.getAttribute("green")));
				blue = Commons.clamp(0.0F, 1.0F, Float.parseFloat(elementColor.getAttribute("blue")));
			} catch (Exception exception) {
				exception.printStackTrace();
				WarpDrive.logger.error("Exception while parsing Color element at " + location);
				red = 0.5F;
				green = 0.5F;
				blue = 0.5F;
			}
		}
		
		ColorData(final NBTTagCompound tagCompound) {
			readFromNBT(tagCompound);
		}
		
		public void readFromNBT(final NBTTagCompound tagCompound) {
			red = tagCompound.getFloat("red");
			green = tagCompound.getFloat("green");
			blue = tagCompound.getFloat("blue");
		}
		
		public NBTTagCompound writeToNBT(final NBTTagCompound tagCompound) {
			tagCompound.setFloat("red", red);
			tagCompound.setFloat("green", green);
			tagCompound.setFloat("blue", blue);
			return tagCompound;
		}
	}
	
	public class RenderData {
		
		public float red;
		public float green;
		public float blue;
		public float alpha;
		public String texture;
		public ResourceLocation resourceLocation;
		public double periodU;
		public double periodV;
		public boolean isAdditive;
		
		RenderData(final String location, final Element elementRender) throws InvalidXmlException {
			try {
				red = Commons.clamp(0.0F, 1.0F, Float.parseFloat(elementRender.getAttribute("red")));
				green = Commons.clamp(0.0F, 1.0F, Float.parseFloat(elementRender.getAttribute("green")));
				blue = Commons.clamp(0.0F, 1.0F, Float.parseFloat(elementRender.getAttribute("blue")));
				alpha = Commons.clamp(0.0F, 1.0F, Float.parseFloat(elementRender.getAttribute("alpha")));
			} catch (Exception exception) {
				exception.printStackTrace();
				WarpDrive.logger.error(String.format("Exception while parsing Render element RGBA attributes at %s", location));
				red = 0.5F;
				green = 0.5F;
				blue = 0.5F;
				alpha = 0.5F;
			}
			texture = elementRender.getAttribute("texture");
			if (texture == null || texture.isEmpty()) {
				texture = null;
				resourceLocation = null;
				periodU = 1.0D;
				periodV = 1.0D;
				isAdditive = false;
			} else {
				resourceLocation = new ResourceLocation(texture);
				
				periodU = 0.001D;
				final String stringPeriodU = elementRender.getAttribute("periodU");
				if (!stringPeriodU.isEmpty()) {
					try {
						periodU = Commons.clampMantisse(0.001D, 1000000.0D, Double.parseDouble(stringPeriodU));
					} catch (NumberFormatException exception) {
						throw new InvalidXmlException(String.format("Invalid periodU attribute '%s' at %s", stringPeriodU, location));
					}
				}
				
				periodV = 0.001D;
				final String stringPeriodV = elementRender.getAttribute("periodV");
				if (!stringPeriodV.isEmpty()) {
					try {
						periodV = Commons.clampMantisse(0.001D, 1000000.0D, Double.parseDouble(stringPeriodV));
					} catch (NumberFormatException exception) {
						throw new InvalidXmlException(String.format("Invalid periodV attribute '%s' at %s", stringPeriodV, location));
					}
				}
				
				isAdditive = Boolean.parseBoolean(elementRender.getAttribute("additive"));
			}
		}
		
		RenderData(final NBTTagCompound tagCompound) {
			readFromNBT(tagCompound);
		}
		
		public void readFromNBT(final NBTTagCompound tagCompound) {
			red = tagCompound.getFloat("red");
			green = tagCompound.getFloat("green");
			blue = tagCompound.getFloat("blue");
			alpha = tagCompound.getFloat("alpha");
			texture = tagCompound.getString("texture");
			if (texture == null || texture.isEmpty()) {
				texture = null;
				resourceLocation = null;
				periodU = 1.0D;
				periodV = 1.0D;
				isAdditive = false;
			} else {
				resourceLocation = new ResourceLocation(texture);
				periodU = tagCompound.getDouble("periodU");
				periodV = tagCompound.getDouble("periodV");
				isAdditive = tagCompound.getBoolean("isAdditive");
			}
		}
		
		public NBTTagCompound writeToNBT(final NBTTagCompound tagCompound) {
			tagCompound.setFloat("red", red);
			tagCompound.setFloat("green", green);
			tagCompound.setFloat("blue", blue);
			tagCompound.setFloat("alpha", alpha);
			if (texture != null) {
				tagCompound.setString("texture", texture);
				tagCompound.setDouble("periodU", periodU);
				tagCompound.setDouble("periodV", periodV);
				tagCompound.setBoolean("isAdditive", isAdditive);
			}
			return tagCompound;
		}
	}
}